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


def _table_exists(conn: sqlite3.Connection, name: str) -> bool:
    row = conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name = ?",
        (name,),
    ).fetchone()
    return row is not None


def _get_schema_version(conn: sqlite3.Connection) -> int:
    if not _table_exists(conn, "schema_migrations"):
        return 0
    row = conn.execute("SELECT MAX(version) AS v FROM schema_migrations").fetchone()
    return int(row["v"] or 0)


def _set_schema_version(conn: sqlite3.Connection, version: int) -> None:
    conn.execute(
        "INSERT OR IGNORE INTO schema_migrations (version, applied_at) VALUES (?, ?)",
        (version, _utc_now_iso()),
    )


def _ensure_migrations_table(conn: sqlite3.Connection) -> None:
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS schema_migrations (
          version INTEGER PRIMARY KEY,
          applied_at TEXT NOT NULL
        )
        """
    )


def init_db(db_path: str) -> None:
    """Initialize DB and apply migrations.

    Migration strategy:
    - schema version 1 == baseline schema (sms_messages/users/api_tokens)
    - Older DBs without schema_migrations will be detected and stamped as v1
      if baseline tables already exist.
    """

    conn = connect(db_path)
    try:
        _ensure_migrations_table(conn)
        current = _get_schema_version(conn)

        # Define migrations (incremental)
        migrations: list[tuple[int, list[str]]] = [
            (
                1,
                [
                    # v1 baseline schema
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
                    """,
                    "CREATE INDEX IF NOT EXISTS idx_sms_created_at ON sms_messages(created_at)",
                    """
                    CREATE TABLE IF NOT EXISTS users (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      username TEXT NOT NULL UNIQUE,
                      email TEXT NOT NULL UNIQUE,
                      password_hash TEXT NOT NULL,
                      created_at TEXT NOT NULL
                    )
                    """,
                    "CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at)",
                    """
                    CREATE TABLE IF NOT EXISTS api_tokens (
                      id INTEGER PRIMARY KEY AUTOINCREMENT,
                      user_id INTEGER NOT NULL,
                      token_hash TEXT NOT NULL UNIQUE,
                      created_at TEXT NOT NULL,
                      last_used_at TEXT,
                      revoked_at TEXT,
                      FOREIGN KEY (user_id) REFERENCES users(id)
                    )
                    """,
                    "CREATE INDEX IF NOT EXISTS idx_api_tokens_user_id ON api_tokens(user_id)",
                ],
            ),
        ]

        if current == 0:
            # If this is an existing DB from older versions, stamp it as v1 if tables exist.
            has_baseline = all(
                _table_exists(conn, t)
                for t in ("sms_messages", "users", "api_tokens")
            )
            if has_baseline:
                _set_schema_version(conn, 1)
                conn.commit()
                current = 1

        # Apply missing migrations
        for version, statements in migrations:
            if version <= current:
                continue
            for stmt in statements:
                conn.execute(stmt)
            _set_schema_version(conn, version)
            conn.commit()
            current = version

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


def create_user(*, db_path: str, username: str, email: str, password_hash: str) -> int:
    conn = connect(db_path)
    try:
        cur = conn.cursor()
        cur.execute(
            """
            INSERT INTO users (username, email, password_hash, created_at)
            VALUES (?, ?, ?, ?)
            """,
            (username, email, password_hash, _utc_now_iso()),
        )
        conn.commit()
        return int(cur.lastrowid)
    finally:
        conn.close()


def get_user_by_identifier(*, db_path: str, identifier: str):
    """identifier can be username or email."""
    conn = connect(db_path)
    try:
        row = conn.execute(
            """
            SELECT id, username, email, password_hash, created_at
              FROM users
             WHERE lower(username) = lower(?) OR lower(email) = lower(?)
            """,
            (identifier, identifier),
        ).fetchone()
        return dict(row) if row else None
    finally:
        conn.close()


def update_user_password_hash(*, db_path: str, user_id: int, password_hash: str) -> None:
    conn = connect(db_path)
    try:
        conn.execute(
            "UPDATE users SET password_hash = ? WHERE id = ?",
            (password_hash, user_id),
        )
        conn.commit()
    finally:
        conn.close()


def create_api_token(*, db_path: str, user_id: int, token_hash: str) -> int:
    conn = connect(db_path)
    try:
        cur = conn.cursor()
        cur.execute(
            """
            INSERT INTO api_tokens (user_id, token_hash, created_at)
            VALUES (?, ?, ?)
            """,
            (user_id, token_hash, _utc_now_iso()),
        )
        conn.commit()
        return int(cur.lastrowid)
    finally:
        conn.close()


def get_user_by_token_hash(*, db_path: str, token_hash: str):
    conn = connect(db_path)
    try:
        row = conn.execute(
            """
            SELECT u.id, u.username, u.email
              FROM api_tokens t
              JOIN users u ON u.id = t.user_id
             WHERE t.token_hash = ?
               AND t.revoked_at IS NULL
            """,
            (token_hash,),
        ).fetchone()
        if not row:
            return None
        # update last_used_at (best effort)
        conn.execute("UPDATE api_tokens SET last_used_at = ? WHERE token_hash = ?", (_utc_now_iso(), token_hash))
        conn.commit()
        return dict(row)
    finally:
        conn.close()
