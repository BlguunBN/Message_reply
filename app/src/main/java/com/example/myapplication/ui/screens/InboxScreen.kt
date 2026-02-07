@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.components.AppCard
import com.example.myapplication.ui.components.ConversationRow
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.components.FilterChipRow
import com.example.myapplication.ui.components.LoadingSkeleton
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.components.TopSearchField
import com.example.myapplication.ui.model.DeliveryState
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.theme.UiDimens
import kotlinx.coroutines.launch

private enum class InboxUiState {
    Loading,
    Error,
    Empty,
    Success,
}

@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    errorMessage: String? = null,
    onOpenThread: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var searchExpanded by remember { mutableStateOf(true) }

    val filtered = remember(query, selectedFilter) {
        FakeData.conversations
            .filter { conversation ->
                query.isBlank() ||
                    conversation.title.contains(query, ignoreCase = true) ||
                    conversation.lastMessage.contains(query, ignoreCase = true)
            }
            .filter { conversation ->
                when (selectedFilter) {
                    "Delivered" -> conversation.state == DeliveryState.DELIVERED
                    "Failed" -> conversation.state == DeliveryState.FAILED
                    "Pending" -> conversation.state == DeliveryState.QUEUED
                    else -> true
                }
            }
    }

    val state = when {
        loading -> InboxUiState.Loading
        errorMessage != null -> InboxUiState.Error
        filtered.isEmpty() -> InboxUiState.Empty
        else -> InboxUiState.Success
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Inbox") },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Toggle search")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                snackbars.showSnackbar(message = "Compose new message is not available yet")
                            }
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "New message")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbars) },
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = UiDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(UiDimens.d12),
        ) {
            AnimatedVisibility(visible = searchExpanded) {
                TopSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = "Search name, number, message",
                )
            }

            FilterChipRow(
                options = listOf("All", "Delivered", "Failed", "Pending"),
                selected = selectedFilter,
                onSelect = { selectedFilter = it },
                modifier = Modifier.fillMaxWidth(),
            )

            when (state) {
                InboxUiState.Loading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(UiDimens.d12)) {
                        repeat(6) { LoadingSkeleton() }
                    }
                }

                InboxUiState.Error -> {
                    AppCard {
                        Text(text = "Could not load conversations", style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                        Text(
                            text = errorMessage.orEmpty(),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        )
                        PrimaryButton(text = "Retry", onClick = {
                            scope.launch {
                                snackbars.showSnackbar("Retry requested")
                            }
                        })
                    }
                }

                InboxUiState.Empty -> {
                    EmptyState(
                        title = "No conversations",
                        body = "Conversations appear after your number receives or sends messages.",
                        cta = "Connect number",
                        onCta = {
                            scope.launch {
                                snackbars.showSnackbar("Open Numbers tab to connect a number")
                            }
                        },
                    )
                }

                InboxUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = UiDimens.d24),
                        verticalArrangement = Arrangement.spacedBy(UiDimens.listItemSpacing),
                    ) {
                        items(filtered, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = { onOpenThread(conversation.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
