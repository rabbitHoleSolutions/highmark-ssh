package com.questterm.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object WindowCloseConfirmDialogDefaults {
    fun formatSessionWarning(sessionCount: Int): String {
        return if (sessionCount <= 1) {
            "This window has 1 active SSH session. Closing will disconnect the connection and terminate remote processes."
        } else {
            "This window has $sessionCount active SSH sessions. Closing will disconnect all connections and terminate remote processes."
        }
    }

    fun shouldShowCloseConfirmOnBack(
        showConnectDialog: Boolean,
        showCloseConfirmDialog: Boolean,
        hasActiveTabs: Boolean,
    ): Boolean {
        return !showConnectDialog && !showCloseConfirmDialog && hasActiveTabs
    }
}

@Composable
fun WindowCloseConfirmDialog(
    sessionCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("window_close_confirm_dialog"),
        title = {
            Text(
                text = "Really close?",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column {
                Text(
                    text = WindowCloseConfirmDialogDefaults.formatSessionWarning(sessionCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Do you really want to close this window?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("window_close_confirm_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Close Window")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("window_close_dismiss_button"),
            ) {
                Text("Cancel")
            }
        },
    )
}
