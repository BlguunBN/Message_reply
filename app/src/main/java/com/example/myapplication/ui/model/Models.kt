package com.example.myapplication.ui.model

import java.time.Instant

enum class DeliveryState { QUEUED, SENT, DELIVERED, FAILED }

data class Conversation(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Instant,
    val unread: Boolean,
    val state: DeliveryState,
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val incoming: Boolean,
    val body: String,
    val timestamp: Instant,
    val state: DeliveryState,
    val requestId: String? = null,
    val error: String? = null,
)

enum class NumberStatus { ACTIVE, NEEDS_VERIFICATION, ERROR }

data class ConnectedNumber(
    val id: String,
    val number: String,
    val status: NumberStatus,
    val subtitle: String,
)

data class AutomationRule(
    val id: String,
    val title: String,
    val description: String,
    val enabled: Boolean,
    val chips: List<String>,
)

enum class TokenStatus { ACTIVE, REVOKED }

data class ApiTokenItem(
    val id: String,
    val label: String,
    val status: TokenStatus,
    val createdAt: String,
    val lastUsedAt: String?,
    val expiresAt: String?,
)
