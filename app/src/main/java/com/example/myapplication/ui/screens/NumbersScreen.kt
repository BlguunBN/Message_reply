package com.example.myapplication.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.model.FakeData
import com.example.myapplication.ui.model.NumberStatus

@Composable
fun NumbersScreen(modifier: Modifier = Modifier) {
    Scaffold(topBar = { TopAppBar(title = { Text("Numbers") }) }) { inner ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FakeData.numbers) { n ->
                val chipText = when (n.status) {
                    NumberStatus.ACTIVE -> "Active"
                    NumberStatus.NEEDS_VERIFICATION -> "Needs verification"
                    NumberStatus.ERROR -> "Error"
                }
                val color = when (n.status) {
                    NumberStatus.ACTIVE -> MaterialTheme.colorScheme.secondaryContainer
                    NumberStatus.NEEDS_VERIFICATION -> MaterialTheme.colorScheme.tertiaryContainer
                    NumberStatus.ERROR -> MaterialTheme.colorScheme.errorContainer
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(n.number, style = MaterialTheme.typography.titleMedium)
                        Text(n.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            chipText,
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
