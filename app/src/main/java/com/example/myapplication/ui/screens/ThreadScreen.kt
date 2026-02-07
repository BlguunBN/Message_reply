@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.components.MessageBubble
import com.example.myapplication.ui.model.ChatMessage
import com.example.myapplication.ui.model.DeliveryState
import com.example.myapplication.ui.theme.Dimens
import com.example.myapplication.ui.model.FakeData
import java.time.Instant
import java.util.UUID
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun ThreadScreen(
    conversationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val messages = remember(conversationId) { mutableStateListOf<ChatMessage>().apply { addAll(FakeData.thread(conversationId)) } }
    var composer by remember { mutableStateOf("") }

    LaunchedEffect(conversationId) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thread") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Dimens.sm)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    top = Dimens.screenPadding,
                    // Extra bottom padding so the last bubble doesn't sit under the composer surface.
                    bottom = Dimens.screenPadding + 72.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        msg = msg,
                        onRetry = { /* TODO */ },
                        onCopyRequestId = { /* TODO */ }
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    // Subtle divider to separate message list from composer.
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.textNormalGap)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.inlineGap)
                        ) {
                            OutlinedTextField(
                                value = composer,
                                onValueChange = { composer = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Message") },
                                singleLine = true,
                            )
                            Button(
                                enabled = composer.isNotBlank(),
                                onClick = {
                                    val body = composer.trim()
                                    if (body.isNotBlank()) {
                                        messages.add(
                                            ChatMessage(
                                                id = UUID.randomUUID().toString(),
                                                conversationId = conversationId,
                                                incoming = false,
                                                body = body,
                                                timestamp = Instant.now(),
                                                state = DeliveryState.QUEUED,
                                            )
                                        )
                                    }
                                    composer = ""
                                }
                            ) { Text("Send") }
                        }

                        Text(
                            "Delivery states are simulated (fake data).",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
