import hashlib
import json
import os
from datetime import datetime, timezone
from typing import Optional

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from db import init_db, mark_telegram_result, try_insert_incoming

load_dotenv()

SECRET = os.getenv("SMS_BRIDGE_SECRET", "")
BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")
PORT = int(os.getenv("PORT", "3000"))

# Telegram formatting
# - "plain": send as plain text
# - "markdown": send as MarkdownV2 (escaped)
TELEGRAM_FORMAT = os.getenv("TELEGRAM_FORMAT", "plain").strip().lower()

# SQLite log (relative to server/ unless absolute)
DB_PATH = os.getenv("DB_PATH", "./sms-bridge.sqlite3")
DEDUP_WINDOW_SECONDS = int(os.getenv("DEDUP_WINDOW_SECONDS", "120"))

app = FastAPI(title="Message_reply: SMS → Telegram (v0.2)")


class IncomingSMS(BaseModel):
    secret: str
    from_number: str = Field(alias="from")
    body: str
    receivedAt: Optional[str] = None  # ISO timestamp string (optional)


def _parse_iso_to_epoch_seconds(iso: Optional[str]) -> Optional[int]:
    if not iso:
        return None
    try:
        # Python 3.11+ parses ISO offsets; tolerate trailing Z
        dt = datetime.fromisoformat(iso.replace("Z", "+00:00"))
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return int(dt.timestamp())
    except Exception:
        return None


def _escape_markdown_v2(text: str) -> str:
    # Telegram MarkdownV2 requires escaping these characters:
    # _ * [ ] ( ) ~ ` > # + - = | { } . !
    # https://core.telegram.org/bots/api#markdownv2-style
    escape = r"_[]()~`>#+-=|{}.!*"
    out = []
    for ch in text:
        if ch in escape:
            out.append("\\" + ch)
        else:
            out.append(ch)
    return "".join(out)


def _format_message(from_number: str, body: str, ts: str) -> tuple[str, Optional[str]]:
    """Returns (text, parse_mode). parse_mode None => plain text."""
    if TELEGRAM_FORMAT == "markdown":
        # Clean MarkdownV2 layout
        f = _escape_markdown_v2(from_number)
        t = _escape_markdown_v2(ts)
        b = _escape_markdown_v2(body)
        text = (
            "*📩 New SMS*\n"
            f"*From:* `{f}`\n"
            f"*Time:* `{t}`\n"
            "*Body:*\n"
            f"```\n{b}\n```"
        )
        return text, "MarkdownV2"

    # plain
    text = f"SMS\nFrom: {from_number}\nTime: {ts}\nBody:\n{body}"
    return text, None


def _compute_fingerprint(from_number: str, body: str, received_at: Optional[str]) -> str:
    # Goal: suppress duplicates caused by retries/multipart within a short window.
    # We bucket by time window to tolerate small timestamp differences.
    epoch = _parse_iso_to_epoch_seconds(received_at)
    if epoch is None:
        epoch = int(datetime.now(timezone.utc).timestamp())
    window = max(1, DEDUP_WINDOW_SECONDS)
    bucket = epoch // window

    payload = {
        "from": from_number,
        "body": body,
        "bucket": bucket,
    }
    raw = json.dumps(payload, ensure_ascii=False, sort_keys=True).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


@app.on_event("startup")
def _startup():
    init_db(DB_PATH)


@app.get("/health")
def health():
    return {"ok": True, "dedupWindowSeconds": DEDUP_WINDOW_SECONDS}


@app.post("/sms/incoming")
async def sms_incoming(payload: IncomingSMS):
    if not SECRET or not BOT_TOKEN or not CHAT_ID:
        raise HTTPException(status_code=500, detail="Server not configured. Create .env from .env.example")

    if payload.secret != SECRET:
        raise HTTPException(status_code=401, detail="Bad secret")

    fingerprint = _compute_fingerprint(payload.from_number, payload.body, payload.receivedAt)
    inserted, row_id = try_insert_incoming(
        db_path=DB_PATH,
        fingerprint=fingerprint,
        from_number=payload.from_number,
        body=payload.body,
        received_at=payload.receivedAt,
    )

    if not inserted:
        # Already processed (or currently being processed) in this time window.
        return {"ok": True, "duplicate": True, "id": row_id}

    ts = payload.receivedAt or datetime.now(timezone.utc).isoformat(timespec="seconds")
    text, parse_mode = _format_message(payload.from_number, payload.body, ts)

    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
    data = {
        "chat_id": CHAT_ID,
        "text": text,
        "disable_web_page_preview": True,
    }
    if parse_mode:
        data["parse_mode"] = parse_mode

    telegram_message_id: Optional[int] = None
    telegram_error: Optional[str] = None

    async with httpx.AsyncClient(timeout=10) as client:
        r = await client.post(url, data=data)
        if r.status_code != 200:
            telegram_error = r.text
        else:
            try:
                j = r.json()
                telegram_message_id = int(j.get("result", {}).get("message_id")) if j.get("ok") else None
            except Exception:
                telegram_message_id = None

    mark_telegram_result(
        db_path=DB_PATH,
        row_id=row_id,
        telegram_message_id=telegram_message_id,
        telegram_error=telegram_error,
    )

    if telegram_error:
        raise HTTPException(status_code=502, detail=f"Telegram error: {telegram_error}")

    return {"ok": True, "duplicate": False, "id": row_id, "telegram_message_id": telegram_message_id}
