package com.questterm.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import com.questterm.data.ConnectionProfile
import com.questterm.ui.screens.HostKeyPrompt
import com.questterm.ui.screens.QuickConnectViewModel

@Composable
fun QuickConnectDialog(
    onDismissRequest: () -> Unit,
    onConnected: () -> Unit,
    viewModel: QuickConnectViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Host key verification dialog
    uiState.hostKeyPrompt?.let { prompt ->
        HostKeyDialog(
            prompt = prompt,
            onAccept = viewModel::acceptHostKey,
            onReject = viewModel::rejectHostKey,
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Left side: Saved connections list
                if (uiState.savedProfiles.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Saved Connections",
                            style = MaterialTheme.typography.titleSmall,
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(uiState.savedProfiles) { profile ->
                                SavedConnectionItem(
                                    profile = profile,
                                    onSelect = { viewModel.selectProfile(profile) },
                                    onToggleFavorite = { viewModel.toggleFavorite(profile.id) },
                                    onDelete = { viewModel.deleteProfile(profile.id) }
                                )
                            }
                        }
                    }
                }

                // Right side: Connection form
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Quick Connect",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.host,
                        onValueChange = viewModel::updateHost,
                        label = { Text("Host") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    )

                    OutlinedTextField(
                        value = uiState.port,
                        onValueChange = viewModel::updatePort,
                        label = { Text("Port") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )
                }

                OutlinedTextField(
                    value = uiState.username,
                    onValueChange = viewModel::updateUsername,
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::updatePassword,
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { viewModel.connect(onConnected) },
                    ),
                )

                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Remember password checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.rememberPassword,
                        onCheckedChange = { viewModel.toggleRememberPassword() }
                    )
                    Text("Remember password", style = MaterialTheme.typography.bodyMedium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        enabled = !uiState.isConnecting,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { viewModel.connect(onConnected) },
                        enabled = !uiState.isConnecting,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (uiState.isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Connect")
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun HostKeyDialog(
    prompt: HostKeyPrompt,
    onAccept: () -> Unit,
    onReject: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onReject,
        title = {
            Text(
                text = if (prompt.keyChanged) "HOST KEY CHANGED" else "Unknown Host",
                color = if (prompt.keyChanged) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column {
                if (prompt.keyChanged) {
                    Text(
                        text = "WARNING: The host key for ${prompt.host}:${prompt.port} has changed! " +
                            "This could indicate a man-in-the-middle attack.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Text(
                        text = "The authenticity of ${prompt.host}:${prompt.port} can't be established.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "${prompt.algorithm} key fingerprint (SHA-256):",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(4.dp))
                SelectionContainer {
                    Text(
                        text = prompt.fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (prompt.keyChanged) {
                        "Accept the new key?"
                    } else {
                        "Do you want to continue connecting?"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accept")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onReject) {
                Text("Reject")
            }
        },
    )
}

@Composable
private fun SavedConnectionItem(
    profile: ConnectionProfile,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (profile.isFavorite)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.displayLabel,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (profile.encryptedPassword != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = "Password saved",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            "Password saved",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = if (profile.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (profile.isFavorite)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
