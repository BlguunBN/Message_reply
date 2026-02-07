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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.IconButton
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
import com.example.myapplication.ui.theme.Dimens
import com.example.myapplication.ui.model.FakeData
import androidx.compose.foundation.layout.PaddingValues

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
                contentPadding = PaddingValues(
                    start = Dimens.screenPadding,
                    end = Dimens.screenPadding,
                    top = Dimens.screenPadding,
                    // Extra bottom padding so the last bubble doesn't sit under the composer surface.
                    bottom = Dimens.screenPadding + 72.dp
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing)
            ) {
                items(messages) { msg ->
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
                                // TODO send
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
