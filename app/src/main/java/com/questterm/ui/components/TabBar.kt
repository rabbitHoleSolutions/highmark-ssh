package com.questterm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questterm.session.TabSession

@Composable
fun TabBar(
    tabs: List<TabSession>,
    activeTabId: String?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onNewTabClick: () -> Unit,
    onKeyboardToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.id == activeTabId }.coerceAtLeast(0),
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.weight(1f),
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = tab.id == activeTabId,
                    onClick = { onTabClick(tab.id) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(tab.displayLabel)
                        IconButton(
                            onClick = { onTabClose(tab.id) },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close tab",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // "+" button to add new tab
            Tab(
                selected = false,
                onClick = onNewTabClick,
                icon = {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New tab",
                        modifier = Modifier.size(24.dp),
                    )
                },
            )
        }

        // Keyboard toggle button
        IconButton(
            onClick = onKeyboardToggle,
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Text(
                text = "\u2328",
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
