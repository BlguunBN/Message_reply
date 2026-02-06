package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.myapplication.ui.components.MessageBubble
import com.example.myapplication.ui.model.FakeData

@Composable
fun ThreadScreen(
    conversationId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val messages = remember(conversationId) { FakeData.thread(conversationId) }
    var composer by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thread") },
                navigationIcon = {
                    Button(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(
                        msg = msg,
                        onRetry = { /* TODO */ },
                        onCopyRequestId = { /* TODO */ }
                    )
                }
            }

            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = composer,
                    onValueChange = { composer = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") },
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    enabled = composer.isNotBlank(),
                    onClick = {
                        // TODO send
                        composer = ""
                    }
                ) { Text("Send") }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Delivery states are simulated (fake data).",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
