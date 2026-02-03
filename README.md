# Message_reply — Prototype v0 (Incoming SMS → Local Server → Telegram)

## Status
- ✅ Local server is running and can forward a test payload to Telegram.
- ⏭️ Next milestone: Android receiver (or emulator) to capture incoming SMS and POST it to the local server.

## What this is
- **v0 goal:** forward **incoming SMS** (Android device / emulator) to **Telegram**.
- **Server runs locally** on your PC on the same Wi‑Fi (no tunneling).
- **Message format:** `Time + From + Text + Phone number`

## Folder layout
- `server/` — Python FastAPI webhook server

---

## 1) Telegram setup (bot token + chat_id)

### A) Create a bot token
1. Open Telegram → search **@BotFather**
2. Send: `/newbot`
3. Follow prompts (name + username)
4. Copy the token it gives you → `TELEGRAM_BOT_TOKEN`

### B) Get your chat_id (DM)
1. Open your bot chat and send: `hi`
2. In a browser, open:
   `https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates`
3. Look for:
   `result[0].message.chat.id`
4. Copy that number → `TELEGRAM_CHAT_ID`

### (Optional) chat_id for a Group
1. Add the bot into the group.
2. Send a message in the group (e.g. `hi`).
3. Open `getUpdates` again and find the group chat id (often a negative number).

---

## 2) Server setup (Windows)

### A) Install Python
Recommended: **Python 3.11 / 3.12** (best wheel support on Windows).

Check your Python:
```powershell
python --version
```

### B) Create & activate a virtual environment (recommended)
From the `server/` directory:
```powershell
cd path\to\Message_reply\server
python -m venv .venv
.\.venv\Scripts\Activate.ps1
```

### C) Install dependencies
```powershell
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

### D) Configure env
Copy `.env.example` to `.env` and fill values:
```powershell
copy .env.example .env
notepad .env
```

Required keys:
- `SMS_BRIDGE_SECRET`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`

### E) Run the server
```powershell
python -m uvicorn main:app --host 0.0.0.0 --port 3000
```

Health check (on the PC):
- `http://127.0.0.1:3000/health`

---

## 3) Test sending a fake SMS payload (from PC)
PowerShell:
```powershell
$body = @{
  secret = "YOUR_SECRET_HERE"
  from   = "+8613800138000"
  body   = "Test"
  receivedAt = (Get-Date).ToString("yyyy-MM-ddTHH:mm:ssK")
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "http://127.0.0.1:3000/sms/incoming" -ContentType "application/json" -Body $body
```

Expected Telegram message format:
```
Time: <timestamp>
From: <phone number>
Text: <sms body>
Phone: <phone number>
```

---

## 4) LAN access (Android → PC, same Wi‑Fi)
1. Find your PC’s LAN IP:
```powershell
ipconfig
```
Look for IPv4 like `192.168.1.50`.

2. Open Windows Firewall for TCP port `3000`.

3. From Android browser, test:
- `http://<PC_IP>:3000/health`

If it fails:
- PC not listening on 0.0.0.0 (must use the uvicorn command above)
- Windows firewall blocking
- Router “AP isolation” enabled

---

## 5) Next steps (tomorrow)
- Build Android receiver app (or emulator) to capture SMS and POST JSON to:
  - `http://<PC_IP>:3000/sms/incoming`
- Add minimal settings on Android:
  - Server URL
  - Secret
