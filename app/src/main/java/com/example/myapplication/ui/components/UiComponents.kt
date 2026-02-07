package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.model.ChatMessage
import com.example.myapplication.ui.model.Conversation
import com.example.myapplication.ui.model.DeliveryState
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.UiDimens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class StatusChipTone {
    Neutral,
    Success,
    Warning,
    Error,
}

@Composable
fun StatusChip(state: DeliveryState, modifier: Modifier = Modifier) {
    val (label, tone) = when (state) {
        DeliveryState.QUEUED -> "Queued" to StatusChipTone.Warning
        DeliveryState.SENT -> "Sent" to StatusChipTone.Neutral
        DeliveryState.DELIVERED -> "Delivered" to StatusChipTone.Success
        DeliveryState.FAILED -> "Failed" to StatusChipTone.Error
    }
    StatusChip(label = label, tone = tone, modifier = modifier)
}

@Composable
fun StatusChip(
    label: String,
    tone: StatusChipTone = StatusChipTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val (container, onContainer) = when (tone) {
        StatusChipTone.Success -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        StatusChipTone.Warning -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        StatusChipTone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        StatusChipTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(container)
            .padding(horizontal = UiDimens.chipHorizontal, vertical = UiDimens.chipVertical)
            .semantics { contentDescription = "Status $label" }
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = onContainer)
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = UiDimens.minTouch),
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = UiDimens.minTouch),
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        modifier = modifier.then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
            } else {
                Modifier
            }
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(UiDimens.cardPadding),
            verticalArrangement = Arrangement.spacedBy(UiDimens.d8),
            content = content,
        )
    }
}

@Composable
fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = UiDimens.minTouch),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarInitials(text = conversation.title)
            Spacer(modifier = Modifier.width(UiDimens.inlineGap))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(UiDimens.d8))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatTime(conversation.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(UiDimens.d8))
                AnimatedVisibility(visible = conversation.unread) {
                    Box(
                        modifier = Modifier
                            .size(UiDimens.d8)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    msg: ChatMessage,
    onRetry: (() -> Unit)? = null,
    onCopyRequestId: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = when {
        msg.state == DeliveryState.FAILED -> MaterialTheme.colorScheme.errorContainer
        msg.incoming -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val onBubbleColor = when {
        msg.state == DeliveryState.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        msg.incoming -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    val bubbleShape = if (msg.incoming) {
        RoundedCornerShape(
            topStart = UiDimens.d8,
            topEnd = UiDimens.d16,
            bottomStart = UiDimens.d16,
            bottomEnd = UiDimens.d16,
        )
    } else {
        RoundedCornerShape(
            topStart = UiDimens.d16,
            topEnd = UiDimens.d8,
            bottomStart = UiDimens.d16,
            bottomEnd = UiDimens.d16,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (msg.incoming) Alignment.Start else Alignment.End,
    ) {
        Box(modifier = Modifier.fillMaxWidth(0.88f)) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = bubbleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = UiDimens.d12, vertical = UiDimens.d8),
                    verticalArrangement = Arrangement.spacedBy(UiDimens.d8),
                ) {
                    Text(text = msg.body, style = MaterialTheme.typography.bodyLarge, color = onBubbleColor)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(UiDimens.d8),
                    ) {
                        if (!msg.incoming) {
                            StatusChip(state = msg.state)
                        }
                        Text(
                            text = formatTime(msg.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = msg.state == DeliveryState.FAILED) {
            Column {
                Spacer(modifier = Modifier.height(UiDimens.d8))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(UiDimens.d8),
                    verticalArrangement = Arrangement.spacedBy(UiDimens.d8),
                ) {
                    if (onRetry != null) {
                        SecondaryButton(text = "Retry", onClick = onRetry)
                    }
                    if (msg.requestId != null && onCopyRequestId != null) {
                        SecondaryButton(text = "Copy request_id", onClick = onCopyRequestId)
                    }
                }
                if (!msg.error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(UiDimens.d4))
                    Text(
                        text = msg.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    cta: String,
    onCta: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(UiDimens.d24),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(UiDimens.d16))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(UiDimens.d8))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(UiDimens.d16))
        PrimaryButton(text = cta, onClick = onCta)
    }
}

@Composable
fun LoadingSkeleton(modifier: Modifier = Modifier) {
    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(UiDimens.avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(UiDimens.d12))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(UiDimens.d8),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(UiDimens.d12)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(UiDimens.d12)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    open: Boolean,
    title: String,
    body: String,
    confirmText: String,
    dismissText: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        },
    )
}

@Composable
private fun AvatarInitials(text: String, modifier: Modifier = Modifier) {
    val initials = text
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "#" }

    Box(
        modifier = modifier
            .size(UiDimens.avatarSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics { contentDescription = "Contact $initials" },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initials, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun TopSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = placeholder) },
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun FilterChipRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(UiDimens.d8),
        verticalArrangement = Arrangement.spacedBy(UiDimens.d8),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(text = option) },
            )
        }
    }
}

@Composable
fun AssistActionChip(text: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(text = text) })
}

private fun formatTime(timestamp: Instant): String {
    return DateTimeFormatter.ofPattern("MMM d, HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(timestamp)
}

@Preview(showBackground = true)
@Composable
private fun ConversationRowLightPreview() {
    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        ConversationRow(
            conversation = Conversation(
                id = "c1",
                title = "Alex Carter",
                lastMessage = "Shipment delivered successfully.",
                timestamp = Instant.now(),
                unread = true,
                state = DeliveryState.DELIVERED,
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConversationRowDarkPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        ConversationRow(
            conversation = Conversation(
                id = "c1",
                title = "Alex Carter",
                lastMessage = "Shipment delivered successfully.",
                timestamp = Instant.now(),
                unread = false,
                state = DeliveryState.SENT,
            ),
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageBubblePreview() {
    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        MessageBubble(
            msg = ChatMessage(
                id = "m1",
                conversationId = "c1",
                incoming = false,
                body = "Payment link sent. Let me know if it worked.",
                timestamp = Instant.now(),
                state = DeliveryState.FAILED,
                requestId = "req_123",
                error = "Upstream timeout",
            ),
            onRetry = {},
            onCopyRequestId = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    MyApplicationTheme(darkTheme = false, dynamicColor = false) {
        EmptyState(
            title = "No data",
            body = "Connect a number to start receiving messages.",
            cta = "Add Number",
            onCta = {},
        )
    }
}
