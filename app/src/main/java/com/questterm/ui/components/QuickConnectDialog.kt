package com.questterm.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import com.questterm.R
import com.questterm.data.AuthMethod
import com.questterm.data.ConnectionProfile
import com.questterm.session.SessionManager
import com.questterm.ui.screens.HostKeyPrompt
import com.questterm.ui.screens.QuickConnectViewModel

@Composable
fun QuickConnectDialog(
    onDismissRequest: () -> Unit,
    onConnected: () -> Unit,
    windowId: String = SessionManager.DEFAULT_WINDOW_ID,
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
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize(),
            ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
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

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.authMethod == AuthMethod.PASSWORD,
                        onClick = { viewModel.selectAuthMethod(AuthMethod.PASSWORD) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) {
                        Text("Password")
                    }
                    SegmentedButton(
                        selected = uiState.authMethod == AuthMethod.KEY,
                        onClick = { viewModel.selectAuthMethod(AuthMethod.KEY) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) {
                        Text("Key")
                    }
                }

                if (uiState.authMethod == AuthMethod.PASSWORD) {
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { viewModel.connect(windowId = windowId, onSuccess = onConnected) },
                        ),
                    )

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
                } else {
                    GeneratedKeyPanel(
                        publicKey = uiState.generatedPublicKey,
                        isSaved = uiState.generatedKeyIsSaved,
                        onRegenerate = viewModel::regenerateKey,
                        onExported = viewModel::onPublicKeyExported,
                    )
                }

                    }

                // Outside the scrolling form so it's never hidden below the fold
                // (the key panel alone can push it out of view).
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                        onClick = { viewModel.connect(windowId = windowId, onSuccess = onConnected) },
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

            // Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_company),
                    contentDescription = "Rabbit Hole Solutions",
                    modifier = Modifier.size(36.dp),
                    alpha = 0.5f,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\u00a9 2026 Rabbit Hole Solutions Inc.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                    Text(
                        text = "github.com/rabbitHoleSolutions/highmark-ssh",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
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
private fun GeneratedKeyPanel(
    publicKey: String?,
    isSaved: Boolean,
    onRegenerate: () -> Unit,
    onExported: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            if (isSaved) {
                "Saved key for this connection. If it isn't on the server yet, add it to ~/.ssh/authorized_keys:"
            } else {
                "New key. Add it to the server's ~/.ssh/authorized_keys:"
            },
            style = MaterialTheme.typography.bodySmall,
        )
        // Buttons above the key: the long key line pushes anything below it out of
        // the scrollable form's visible area on Quest.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = {
                    publicKey?.let {
                        clipboardManager.setText(AnnotatedString(it))
                        onExported()
                    }
                },
                enabled = publicKey != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("Copy")
            }
            OutlinedButton(
                onClick = {
                    publicKey?.let {
                        onExported()
                        // Hand off to the system share sheet; the OS lists whatever
                        // installed apps accept text.
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Highmark SSH public key")
                            putExtra(Intent.EXTRA_TEXT, it)
                        }
                        context.startActivity(Intent.createChooser(send, "Share public key"))
                    }
                },
                enabled = publicKey != null,
                modifier = Modifier.weight(1f),
            ) {
                Text("Share")
            }
            OutlinedButton(
                onClick = onRegenerate,
                modifier = Modifier.weight(1f),
            ) {
                Text("Regen")
            }
        }
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SelectionContainer {
                Text(
                    text = publicKey ?: "Generating…",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
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
                // Show what's actually stored. A profile keeps its key and password
                // independently, so it can have both regardless of the last method used.
                val hasKey = !profile.encryptedPrivateKey.isNullOrEmpty() && !profile.publicKeyRaw.isNullOrEmpty()
                val hasPassword = !profile.encryptedPassword.isNullOrEmpty()
                if (hasKey || hasPassword) {
                    // Stacked: side by side they're too cramped in the narrow list.
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (hasKey) {
                            Text(
                                "🔑 Key saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (hasPassword) {
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
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = if (profile.isFavorite) "Unfavorite" else "Favorite",
                        modifier = Modifier.size(16.dp),
                        tint = if (profile.isFavorite)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
