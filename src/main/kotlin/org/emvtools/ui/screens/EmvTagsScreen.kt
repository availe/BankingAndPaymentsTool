package org.emvtools.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.emvtools.data.EmvTagDatabase
import org.emvtools.model.EmvTag
import org.emvtools.model.TagSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmvTagsScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf<TagSource?>(null) }
    
    val allTags = remember { EmvTagDatabase.getAllTags() }
    
    val filteredTags = remember(searchQuery, selectedSource) {
        var tags = if (searchQuery.isBlank()) allTags else EmvTagDatabase.searchTags(searchQuery)
        if (selectedSource != null) {
            tags = tags.filter { it.source == selectedSource }
        }
        tags
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "EMV Tag Database",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search tags (by tag, name, or description)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Source filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSource == null,
                onClick = { selectedSource = null },
                label = { Text("All") }
            )
            TagSource.entries.forEach { source ->
                FilterChip(
                    selected = selectedSource == source,
                    onClick = { selectedSource = if (selectedSource == source) null else source },
                    label = { Text(source.name) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${filteredTags.size} tags found",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Tag list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredTags) { tag ->
                EmvTagCard(tag)
            }
        }
    }
}

@Composable
private fun EmvTagCard(tag: EmvTag) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tag.tag,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                AssistChip(
                    onClick = {},
                    label = { Text(tag.source.name, style = MaterialTheme.typography.labelSmall) }
                )
            }
            
            Text(
                text = tag.name,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = tag.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Format: ${tag.format.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tag.minLength != null || tag.maxLength != null) {
                    Text(
                        text = "Length: ${tag.minLength ?: "?"}-${tag.maxLength ?: "?"} bytes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

