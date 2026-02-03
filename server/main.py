import os
from datetime import datetime
from typing import Optional

import httpx
from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

load_dotenv()

SECRET = os.getenv("SMS_BRIDGE_SECRET", "")
BOT_TOKEN = os.getenv("TELEGRAM_BOT_TOKEN", "")
CHAT_ID = os.getenv("TELEGRAM_CHAT_ID", "")
PORT = int(os.getenv("PORT", "3000"))

app = FastAPI(title="Message_reply: SMS → Telegram (v0)")


class IncomingSMS(BaseModel):
    secret: str
    from_number: str = Field(alias="from")
    body: str
    receivedAt: Optional[str] = None  # ISO timestamp string (optional)


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/sms/incoming")
async def sms_incoming(payload: IncomingSMS):
    if not SECRET or not BOT_TOKEN or not CHAT_ID:
        raise HTTPException(status_code=500, detail="Server not configured. Create .env from .env.example")

    if payload.secret != SECRET:
        raise HTTPException(status_code=401, detail="Bad secret")

    # Message format requested:
    # "Time + from + Text + phone number"
    ts = payload.receivedAt or datetime.now().isoformat(timespec="seconds")
    text = f"Time: {ts}\nFrom: {payload.from_number}\nText: {payload.body}\nPhone: {payload.from_number}"

    url = f"https://api.telegram.org/bot{BOT_TOKEN}/sendMessage"
    data = {
        "chat_id": CHAT_ID,
        "text": text,
        "disable_web_page_preview": True,
    }

    async with httpx.AsyncClient(timeout=10) as client:
        r = await client.post(url, data=data)
        if r.status_code != 200:
            raise HTTPException(status_code=502, detail=f"Telegram error: {r.text}")

    return {"ok": True}
