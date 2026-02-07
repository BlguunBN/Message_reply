package com.example.myapplication.ui.model

import java.time.Instant
import java.time.temporal.ChronoUnit

object FakeData {
    val conversations: List<Conversation> = listOf(
        Conversation(
            id = "c1",
            title = "Mom",
            lastMessage = "Did you get home?",
            timestamp = Instant.now().minus(4, ChronoUnit.MINUTES),
            unread = true,
            state = DeliveryState.DELIVERED,
        ),
        Conversation(
            id = "c2",
            title = "+86 138 0013 8000",
            lastMessage = "OTP: 348921",
            timestamp = Instant.now().minus(51, ChronoUnit.MINUTES),
            unread = false,
            state = DeliveryState.SENT,
        ),
        Conversation(
            id = "c3",
            title = "Bank",
            lastMessage = "We could not process your request.",
            timestamp = Instant.now().minus(6, ChronoUnit.HOURS),
            unread = false,
            state = DeliveryState.FAILED,
        ),
    )

    fun thread(conversationId: String): List<ChatMessage> = listOf(
        ChatMessage(
            id = "m1",
            conversationId = conversationId,
            incoming = true,
            body = "Hello! This is a test message.",
            timestamp = Instant.now().minus(2, ChronoUnit.HOURS),
            state = DeliveryState.DELIVERED,
        ),
        ChatMessage(
            id = "m2",
            conversationId = conversationId,
            incoming = false,
            body = "Replying from Message Reply.",
            timestamp = Instant.now().minus(110, ChronoUnit.MINUTES),
            state = DeliveryState.DELIVERED,
        ),
        ChatMessage(
            id = "m3",
            conversationId = conversationId,
            incoming = false,
            body = "This one failed to send.",
            timestamp = Instant.now().minus(15, ChronoUnit.MINUTES),
            state = DeliveryState.FAILED,
            requestId = "req_9f21c1c5",
            error = "HTTP 502: Telegram error",
        ),
    )

    val numbers: List<ConnectedNumber> = listOf(
        ConnectedNumber("n1", "+1 (415) 555-0198", NumberStatus.ACTIVE, "Last check: just now"),
        ConnectedNumber("n2", "+86 138 0013 8000", NumberStatus.NEEDS_VERIFICATION, "Verify to start relaying"),
        ConnectedNumber("n3", "+44 20 7946 0958", NumberStatus.ERROR, "Server cannot reach device"),
    )

    val rules: List<AutomationRule> = listOf(
        AutomationRule("r1", "Forward OTP", "Only forward OTP-like messages", true, listOf("OTP", "All numbers")),
        AutomationRule("r2", "Only from contacts", "Reduce spam by filtering unknown senders", false, listOf("Contacts")),
    )

    val tokens: List<ApiTokenItem> = listOf(
        ApiTokenItem("t1", "This device", TokenStatus.ACTIVE, "2026-02-06", "just now", "2026-08-06"),
        ApiTokenItem("t2", "Old laptop", TokenStatus.REVOKED, "2026-01-20", null, null),
    )
}
