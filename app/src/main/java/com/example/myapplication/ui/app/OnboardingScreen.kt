@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.AppConfig
import com.example.myapplication.ui.components.AppCard
import com.example.myapplication.ui.components.PrimaryButton
import com.example.myapplication.ui.theme.UiDimens

@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onContinue: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var serverUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        serverUrl = AppConfig.getServerBaseUrl(context)
    }

    Scaffold(topBar = { TopAppBar(title = { Text(text = "Welcome") }) }) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(UiDimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(UiDimens.sectionSpacing),
        ) {
            AppCard {
                Text(text = "Message Reply", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                Text(
                    text = "Relay SMS to your server and keep replying with your real number.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AppCard {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(text = "Server base URL") },
                    placeholder = { Text(text = "http://10.0.2.2:3000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                PrimaryButton(
                    text = "Continue",
                    onClick = {
                        AppConfig.setServerBaseUrl(context, serverUrl)
                        onContinue()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serverUrl.isNotBlank(),
                )

                Text(
                    text = "Tip: on a real phone, use your PC LAN IP. Example: http://192.168.1.50:3000",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
