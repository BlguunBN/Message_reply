@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.theme.Dimens

@Composable
fun AutomationsScreen(modifier: Modifier = Modifier) {
    val enabledMap = remember { mutableStateMapOf<String, Boolean>().apply { FakeData.rules.forEach { put(it.id, it.enabled) } } }

    Scaffold(topBar = { TopAppBar(title = { Text("Automations") }) }) { inner ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(Dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing)
        ) {
            FakeData.rules.forEach { rule ->
                val enabled = enabledMap[rule.id] == true
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(Dimens.cardPadding), verticalArrangement = Arrangement.spacedBy(Dimens.textNormalGap)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(rule.title, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(Dimens.textTightGap))
                                Text(rule.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(Dimens.inlineGap))
                            Switch(checked = enabled, onCheckedChange = { enabledMap[rule.id] = it })
                        }
                        Text(
                            "Applies: ${rule.chips.joinToString(", ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
