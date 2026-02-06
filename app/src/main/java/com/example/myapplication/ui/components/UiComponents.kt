package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.model.ChatMessage
import com.example.myapplication.ui.model.Conversation
import com.example.myapplication.ui.model.DeliveryState

@Composable
fun StatusChip(state: DeliveryState, modifier: Modifier = Modifier) {
    val (label, bg) = when (state) {
        DeliveryState.DELIVERED -> "Delivered" to MaterialTheme.colorScheme.secondaryContainer
        DeliveryState.SENT -> "Sent" to MaterialTheme.colorScheme.surfaceVariant
        DeliveryState.QUEUED -> "Pending" to MaterialTheme.colorScheme.tertiaryContainer
        DeliveryState.FAILED -> "Failed" to MaterialTheme.colorScheme.errorContainer
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun ConversationRow(
    convo: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarInitials(text = convo.title)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(convo.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(convo.lastMessage, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(convo.state)
                AnimatedVisibility(visible = convo.unread) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .size(8.dp)
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
        msg.incoming -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val align = if (msg.incoming) Alignment.Start else Alignment.End

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = align,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(msg.body, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(msg.state)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        msg.timestamp.toString().replace('T', ' ').take(16),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Crossfade(targetState = msg.state == DeliveryState.FAILED, label = "failedActions") { failed ->
            if (failed) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onRetry != null) {
                        Button(onClick = onRetry) { Text("Retry") }
                    }
                    if (msg.requestId != null && onCopyRequestId != null) {
                        OutlinedButton(onClick = onCopyRequestId) { Text("Copy request_id") }
                    }
                }
                if (!msg.error.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        msg.error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onCta) { Text(cta) }
    }
}

@Composable
fun LoadingSkeletonRow(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth(0.9f).height(12.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
    }
}

@Composable
fun ConfirmRevokeDialog(
    open: Boolean,
    title: String = "Revoke token?",
    body: String = "Apps using this token will be signed out.",
    confirmText: String = "Revoke",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!open) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AvatarInitials(text: String, modifier: Modifier = Modifier) {
    val initials = text.trim().take(2).uppercase()
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(initials, style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { onSelect(opt) },
                label = { Text(opt) },
            )
        }
    }
}

@Composable
fun AssistActionChip(text: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(text) })
}
