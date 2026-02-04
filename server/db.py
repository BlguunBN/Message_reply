import os
import sqlite3
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Optional


def _utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="seconds")


@dataclass
class MessageRecord:
    id: int
    fingerprint: str
    from_number: str
    body: str
    received_at: Optional[str]
    created_at: str
    telegram_message_id: Optional[int]
    telegram_error: Optional[str]


def connect(db_path: str) -> sqlite3.Connection:
    os.makedirs(os.path.dirname(db_path) or ".", exist_ok=True)
    conn = sqlite3.connect(db_path, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA synchronous=NORMAL")
    return conn


def init_db(db_path: str) -> None:
    conn = connect(db_path)
    try:
        conn.execute(
            """
            CREATE TABLE IF NOT EXISTS sms_messages (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              fingerprint TEXT NOT NULL UNIQUE,
              from_number TEXT NOT NULL,
              body TEXT NOT NULL,
              received_at TEXT,
              created_at TEXT NOT NULL,
              telegram_message_id INTEGER,
              telegram_error TEXT
            )
            """
        )
        conn.execute("CREATE INDEX IF NOT EXISTS idx_sms_created_at ON sms_messages(created_at)")
        conn.commit()
    finally:
        conn.close()


def try_insert_incoming(
    *,
    db_path: str,
    fingerprint: str,
    from_number: str,
    body: str,
    received_at: Optional[str],
) -> tuple[bool, int]:
    """Returns (inserted, row_id). If duplicate fingerprint, inserted=False."""
    conn = connect(db_path)
    try:
        cur = conn.cursor()
        try:
            cur.execute(
                """
                INSERT INTO sms_messages (fingerprint, from_number, body, received_at, created_at)
                VALUES (?, ?, ?, ?, ?)
                """,
                (fingerprint, from_number, body, received_at, _utc_now_iso()),
            )
            conn.commit()
            return True, int(cur.lastrowid)
        except sqlite3.IntegrityError:
            row = conn.execute(
                "SELECT id FROM sms_messages WHERE fingerprint = ?",
                (fingerprint,),
            ).fetchone()
            return False, int(row["id"]) if row else -1
    finally:
        conn.close()


def mark_telegram_result(
    *,
    db_path: str,
    row_id: int,
    telegram_message_id: Optional[int],
    telegram_error: Optional[str],
) -> None:
    conn = connect(db_path)
    try:
        conn.execute(
            """
            UPDATE sms_messages
               SET telegram_message_id = ?, telegram_error = ?
             WHERE id = ?
            """,
            (telegram_message_id, telegram_error, row_id),
        )
        conn.commit()
    finally:
        conn.close()
