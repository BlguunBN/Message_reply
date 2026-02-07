@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.components.MessageBubble
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.model.ChatMessage
import com.example.myapplication.ui.model.DeliveryState
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.theme.UiDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

private enum class ThreadUiState {
    Loading,
    Error,
    Empty,
    Success,
}

@Composable
fun ThreadScreen(
    conversationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    errorMessage: String? = null,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    val conversationTitle = remember(conversationId) {
        FakeData.conversations.firstOrNull { it.id == conversationId }?.title ?: "Thread"
    }

    var composer by rememberSaveable(conversationId) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val messages = remember(conversationId) {
        mutableStateListOf<ChatMessage>().apply { addAll(FakeData.thread(conversationId)) }
    }

    val state = when {
        loading -> ThreadUiState.Loading
        errorMessage != null -> ThreadUiState.Error
        messages.isEmpty() -> ThreadUiState.Empty
        else -> ThreadUiState.Success
    }

    val isNearBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            messages.isEmpty() || lastVisible >= messages.lastIndex - 1
        }
    }

    LaunchedEffect(conversationId, state) {
        if (state == ThreadUiState.Success && messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(messages.size) {
        if (state == ThreadUiState.Success && messages.isNotEmpty() && isNearBottom) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = conversationTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Thread search is not available yet")
                            }
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search in thread")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            when (state) {
                ThreadUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(UiDimens.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(UiDimens.d12),
                    ) {
                        repeat(5) {
                            Surface(
                                tonalElevation = UiDimens.d4,
                                shape = androidx.compose.material3.MaterialTheme.shapes.medium,
                            ) {
                                Text(
                                    text = "Loading message...",
                                    modifier = Modifier.padding(UiDimens.d16),
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                ThreadUiState.Error -> {
                    EmptyState(
                        title = "Could not load thread",
                        body = errorMessage.orEmpty(),
                        cta = "Retry",
                        onCta = {
                            scope.launch { snackbarHostState.showSnackbar("Retry requested") }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                ThreadUiState.Empty -> {
                    EmptyState(
                        title = "No messages yet",
                        body = "Start the conversation with your first message.",
                        cta = "Write message",
                        onCta = {},
                        modifier = Modifier.weight(1f),
                    )
                }

                ThreadUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
                        contentPadding = PaddingValues(
                            horizontal = UiDimens.screenPadding,
                            vertical = UiDimens.d16,
                        ),
                        verticalArrangement = Arrangement.spacedBy(UiDimens.d12),
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                msg = message,
                                onRetry = {
                                    val index = messages.indexOfFirst { it.id == message.id }
                                    if (index != -1) {
                                        messages[index] = message.copy(
                                            state = DeliveryState.QUEUED,
                                            error = null,
                                        )
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Message queued for retry")
                                    }
                                },
                                onCopyRequestId = {
                                    message.requestId?.let { requestId ->
                                        clipboardManager.setText(AnnotatedString(requestId))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("request_id copied")
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Surface(
                tonalElevation = UiDimens.d4,
                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(UiDimens.d16),
                        horizontalArrangement = Arrangement.spacedBy(UiDimens.d12),
                    ) {
                        OutlinedTextField(
                            value = composer,
                            onValueChange = { composer = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(text = "Write a reply") },
                            singleLine = true,
                            enabled = !sending,
                        )
                        PrimaryButton(
                            text = if (sending) "Sending" else "Send",
                            onClick = {
                                if (composer.isBlank() || sending) return@PrimaryButton

                                val body = composer.trim()
                                composer = ""
                                sending = true

                                scope.launch {
                                    val message = ChatMessage(
                                        id = UUID.randomUUID().toString(),
                                        conversationId = conversationId,
                                        incoming = false,
                                        body = body,
                                        timestamp = Instant.now(),
                                        state = DeliveryState.QUEUED,
                                    )
                                    messages.add(message)
                                    delay(650)
                                    val index = messages.indexOfFirst { it.id == message.id }
                                    if (index != -1) {
                                        messages[index] = message.copy(state = DeliveryState.SENT)
                                    }
                                    sending = false
                                    snackbarHostState.showSnackbar("Message sent")
                                }
                            },
                            enabled = composer.isNotBlank() && !sending,
                        )
                    }
                    if (sending) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = UiDimens.d16)
                                .padding(bottom = UiDimens.d12),
                            horizontalArrangement = Arrangement.Start,
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = UiDimens.d4,
                                modifier = Modifier
                                    .padding(end = UiDimens.d8)
                                    .padding(top = UiDimens.d4),
                            )
                            Text(
                                text = "Sending...",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
