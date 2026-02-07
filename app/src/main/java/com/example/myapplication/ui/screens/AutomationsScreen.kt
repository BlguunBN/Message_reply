@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.components.AppCard
import com.example.myapplication.ui.components.AssistActionChip
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.theme.UiDimens
import kotlinx.coroutines.launch

@Composable
fun AutomationsScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val enabledMap = remember {
        mutableStateMapOf<String, Boolean>().apply {
            FakeData.rules.forEach { rule -> put(rule.id, rule.enabled) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Automations") },
                actions = {
                    IconButton(onClick = {
                        scope.launch { snackbarHostState.showSnackbar("Add automation flow is not available yet") }
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add automation")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = UiDimens.screenPadding),
            contentPadding = PaddingValues(vertical = UiDimens.d16),
            verticalArrangement = Arrangement.spacedBy(UiDimens.listItemSpacing),
        ) {
            items(FakeData.rules, key = { it.id }) { rule ->
                val enabled = enabledMap[rule.id] == true

                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(UiDimens.d4)) {
                            Text(text = rule.title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                            Text(
                                text = rule.description,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { enabledMap[rule.id] = it })
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(UiDimens.d8)) {
                        rule.chips.forEach { chip ->
                            AssistActionChip(text = chip, onClick = {})
                        }
                    }
                }
            }
        }
    }
}
