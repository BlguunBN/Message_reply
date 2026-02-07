@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.components.AppCard
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.components.StatusChip
import com.example.myapplication.ui.components.StatusChipTone
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.model.NumberStatus
import com.example.myapplication.ui.theme.UiDimens
import kotlinx.coroutines.launch

private enum class NumbersUiState {
    Loading,
    Error,
    Empty,
    Success,
}

@Composable
fun NumbersScreen(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    errorMessage: String? = null,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val numbers = FakeData.numbers

    val state = when {
        loading -> NumbersUiState.Loading
        errorMessage != null -> NumbersUiState.Error
        numbers.isEmpty() -> NumbersUiState.Empty
        else -> NumbersUiState.Success
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Numbers") },
                actions = {
                    IconButton(onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Add number flow is not available yet") }
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add number")
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
                .padding(horizontal = UiDimens.screenPadding),
        ) {
            when (state) {
                NumbersUiState.Loading -> {
                    Column(verticalArrangement = Arrangement.spacedBy(UiDimens.d12)) {
                        repeat(4) {
                            AppCard {
                                Text(text = "Loading number...")
                            }
                        }
                    }
                }

                NumbersUiState.Error -> {
                    EmptyState(
                        title = "Could not load numbers",
                        body = errorMessage.orEmpty(),
                        cta = "Retry",
                        onCta = { scope.launch { snackbarHostState.showSnackbar("Retry requested") } },
                    )
                }

                NumbersUiState.Empty -> {
                    EmptyState(
                        title = "No connected numbers",
                        body = "Add your first number to start forwarding and replying.",
                        cta = "Add Number",
                        onCta = { scope.launch { snackbarHostState.showSnackbar("Add number flow is not available yet") } },
                    )
                }

                NumbersUiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = UiDimens.d16),
                        verticalArrangement = Arrangement.spacedBy(UiDimens.listItemSpacing),
                    ) {
                        item {
                            PrimaryButton(
                                text = "Add Number",
                                onClick = { scope.launch { snackbarHostState.showSnackbar("Add number flow is not available yet") } },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        items(numbers, key = { it.id }) { number ->
                            val statusLabel = when (number.status) {
                                NumberStatus.ACTIVE -> "Active"
                                NumberStatus.NEEDS_VERIFICATION -> "Needs verification"
                                NumberStatus.ERROR -> "Error"
                            }

                            AppCard {
                                Text(text = number.number, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                                Text(
                                    text = number.subtitle,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                StatusChip(
                                    label = statusLabel,
                                    tone = when (number.status) {
                                        NumberStatus.ACTIVE -> StatusChipTone.Success
                                        NumberStatus.NEEDS_VERIFICATION -> StatusChipTone.Warning
                                        NumberStatus.ERROR -> StatusChipTone.Error
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
