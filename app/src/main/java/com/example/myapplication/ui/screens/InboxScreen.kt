@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.components.ConversationRow
import com.example.myapplication.ui.theme.Dimens
import com.example.myapplication.ui.components.EmptyState
import com.example.myapplication.ui.components.FilterChipRow
import com.example.myapplication.ui.components.LoadingSkeletonRow
import com.example.myapplication.ui.components.TopSearchBar
import com.example.myapplication.ui.model.FakeData

@Composable
fun InboxScreen(
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onOpenThread: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }

    val conversations = FakeData.conversations
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true) }
        .filter {
            when (filter) {
                "Delivered" -> it.state.name == "DELIVERED"
                "Failed" -> it.state.name == "FAILED"
                "Pending" -> it.state.name == "QUEUED"
                else -> true
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inbox") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* TODO new message */ }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "New message")
            }
        }
    ) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.md)
        ) {
            TopSearchBar(query = query, onQueryChange = { query = it }, placeholder = "Search")

            FilterChipRow(
                options = listOf("All", "Delivered", "Pending", "Failed"),
                selected = filter,
                onSelect = { filter = it },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(visible = loading) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.sm)) {
                    repeat(6) { LoadingSkeletonRow() }
                }
            }

            if (!loading && conversations.isEmpty()) {
                EmptyState(
                    title = "No conversations",
                    body = "Messages will appear here once your number is connected.",
                    cta = "Connect a number",
                    onCta = { /* TODO */ }
                )
            } else if (!loading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing)
                ) {
                    items(conversations) { convo ->
                        ConversationRow(convo = convo, onClick = { onOpenThread(convo.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
