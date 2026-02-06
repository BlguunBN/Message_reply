import hashlib
import hmac
import json
import os
from datetime import datetime, timezone
from typing import Optional

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, Field

from passlib.context import CryptContext

from db import (
    create_api_token,
    create_user,
    get_user_by_identifier,
    get_user_by_token_hash,
    init_db,
    mark_telegram_result,
    try_insert_incoming,
    update_user_password_hash,
)

load_dotenv()

SECRET = os.getenv("SMS_BRIDGE_SECRET", "")
BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")
PORT = int(os.getenv("PORT", "3000"))

# Auth
AUTH_REQUIRED = os.getenv("AUTH_REQUIRED", "true").strip().lower() in ("1", "true", "yes", "y")
ALLOW_SECRET_AUTH = os.getenv("ALLOW_SECRET_AUTH", "false").strip().lower() in ("1", "true", "yes", "y")

# Password hashing
# Prefer argon2 (no 72-byte password limitation like bcrypt). Keep bcrypt support for old hashes.
pwd_context = CryptContext(schemes=["argon2", "bcrypt"], deprecated="auto")

# Telegram formatting
# - "plain": send as plain text
# - "markdown": send as MarkdownV2 (escaped)
TELEGRAM_FORMAT = os.getenv("TELEGRAM_FORMAT", "plain").strip().lower()

# Auth/security
# We accept BOTH methods:
# - Legacy: payload.secret == SMS_BRIDGE_SECRET
# - Recommended: HMAC headers (X-Timestamp + X-Signature)
#   signature = hex(hmac_sha256(SMS_BRIDGE_SECRET, f"{timestamp}.{raw_body}"))
HMAC_WINDOW_SECONDS = int(os.getenv("HMAC_WINDOW_SECONDS", "120"))
ALLOW_LEGACY_SECRET = os.getenv("ALLOW_LEGACY_SECRET", "true").strip().lower() in ("1", "true", "yes", "y")

# SQLite log (relative to server/ unless absolute)
DB_PATH = os.getenv("DB_PATH", "./sms-bridge.sqlite3")
DEDUP_WINDOW_SECONDS = int(os.getenv("DEDUP_WINDOW_SECONDS", "120"))

app = FastAPI(title="Message_reply: SMS → Telegram (v0.3)")


class IncomingSMS(BaseModel):
    # Legacy secret (optional). Prefer Authorization: Bearer <token>.
    secret: Optional[str] = None
    from_number: str = Field(alias="from")
    body: str
    receivedAt: Optional[str] = None  # ISO timestamp string (optional)


class SignupRequest(BaseModel):
    username: str
    email: str
    password: str


class LoginRequest(BaseModel):
    identifier: str  # username OR email
    password: str


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


def _verify_hmac_headers(*, request: Request, raw_body: bytes) -> bool:
    """Return True if HMAC auth passes, False if headers missing.

    Raises HTTPException on present-but-invalid headers.
    """
    ts = request.headers.get("x-timestamp")
    sig = request.headers.get("x-signature")

    if not ts and not sig:
        return False  # no HMAC headers

    if not ts or not sig:
        raise HTTPException(status_code=401, detail="Missing X-Timestamp or X-Signature")

    try:
        ts_int = int(ts)
    except Exception:
        raise HTTPException(status_code=401, detail="Bad X-Timestamp")

    now = int(datetime.now(timezone.utc).timestamp())
    if abs(now - ts_int) > max(1, HMAC_WINDOW_SECONDS):
        raise HTTPException(status_code=401, detail="Stale request")

    if not SECRET:
        raise HTTPException(status_code=500, detail="Server missing SMS_BRIDGE_SECRET")

    msg = str(ts_int).encode("utf-8") + b"." + raw_body
    expected = hmac.new(SECRET.encode("utf-8"), msg, hashlib.sha256).hexdigest()

    if not hmac.compare_digest(expected, sig.strip().lower()):
        raise HTTPException(status_code=401, detail="Bad signature")

    return True


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode("utf-8")).hexdigest()


def _get_bearer_token(request: Request) -> Optional[str]:
    auth = request.headers.get("authorization") or request.headers.get("Authorization")
    if not auth:
        return None
    parts = auth.strip().split(" ")
    if len(parts) != 2 or parts[0].lower() != "bearer":
        return None
    return parts[1].strip() or None


def _require_user(request: Request):
    token = _get_bearer_token(request)
    if not token:
        raise HTTPException(status_code=401, detail="Missing Bearer token")
    user = get_user_by_token_hash(db_path=DB_PATH, token_hash=_hash_token(token))
    if not user:
        raise HTTPException(status_code=401, detail="Invalid token")
    return user


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
    return {
        "ok": True,
        "dedupWindowSeconds": DEDUP_WINDOW_SECONDS,
        "auth": {
            "bearerRequired": AUTH_REQUIRED,
            "allowSecretAuthFallback": ALLOW_SECRET_AUTH,
            "legacySecret": ALLOW_LEGACY_SECRET,
            "hmac": True,
            "hmacWindowSeconds": HMAC_WINDOW_SECONDS,
        },
    }


@app.post("/auth/signup")
def auth_signup(req: SignupRequest):
    if not req.username.strip() or not req.email.strip() or not req.password:
        raise HTTPException(status_code=400, detail="Missing fields")

    username = req.username.strip()
    email = req.email.strip()

    pw_hash = pwd_context.hash(req.password)
    try:
        user_id = create_user(db_path=DB_PATH, username=username, email=email, password_hash=pw_hash)
    except Exception as e:
        # likely UNIQUE constraint
        raise HTTPException(status_code=400, detail=f"User already exists or invalid: {e}")

    # Create a token immediately
    token = os.urandom(32).hex()
    create_api_token(db_path=DB_PATH, user_id=user_id, token_hash=_hash_token(token))

    return {"ok": True, "token": token, "user": {"id": user_id, "username": username, "email": email}}


@app.post("/auth/login")
def auth_login(req: LoginRequest):
    identifier = req.identifier.strip()
    if not identifier or not req.password:
        raise HTTPException(status_code=400, detail="Missing fields")

    user = get_user_by_identifier(db_path=DB_PATH, identifier=identifier)
    if not user:
        raise HTTPException(status_code=401, detail="Bad credentials")

    if not pwd_context.verify(req.password, user["password_hash"]):
        raise HTTPException(status_code=401, detail="Bad credentials")

    # If the stored hash is using an older scheme/params, upgrade it transparently.
    try:
        if pwd_context.needs_update(user["password_hash"]):
            new_hash = pwd_context.hash(req.password)
            update_user_password_hash(db_path=DB_PATH, user_id=int(user["id"]), password_hash=new_hash)
    except Exception:
        # best-effort; don't block login
        pass

    token = os.urandom(32).hex()
    create_api_token(db_path=DB_PATH, user_id=int(user["id"]), token_hash=_hash_token(token))

    return {
        "ok": True,
        "token": token,
        "user": {"id": int(user["id"]), "username": user["username"], "email": user["email"]},
    }


@app.post("/sms/incoming")
async def sms_incoming(request: Request, payload: IncomingSMS):
    if not BOT_TOKEN or not CHAT_ID:
        raise HTTPException(status_code=500, detail="Server not configured. Create .env from .env.example")

    # Preferred auth: Bearer token
    authed_user = None
    if AUTH_REQUIRED:
        token = _get_bearer_token(request)
        if token:
            authed_user = get_user_by_token_hash(db_path=DB_PATH, token_hash=_hash_token(token))
        if not authed_user:
            if not ALLOW_SECRET_AUTH:
                raise HTTPException(status_code=401, detail="Unauthorized")
            # else: try secret/HMAC below

    # Optional fallback auth: secret/HMAC
    if not authed_user and (not AUTH_REQUIRED or ALLOW_SECRET_AUTH):
        raw = await request.body()

        used_hmac = _verify_hmac_headers(request=request, raw_body=raw)
        if not used_hmac:
            if not ALLOW_LEGACY_SECRET:
                raise HTTPException(status_code=401, detail="HMAC required")
            if not SECRET:
                raise HTTPException(status_code=500, detail="Server missing SMS_BRIDGE_SECRET")
            if (payload.secret or "") != SECRET:
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
