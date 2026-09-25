package de.j4velin.calendarWidget.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import de.j4velin.calendarWidget.R
import de.j4velin.calendarWidget.data.Backup

private fun Context.openUri(uri: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, uri, Toast.LENGTH_LONG).show()
    }
}

/** Requests the calendar permission when first shown */
@Composable
internal fun rememberCalendarPermissionRequest(viewModel: ConfigViewModel<*>): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionResult(it)
    }
    var requested by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!viewModel.hasPermission && !requested) {
            requested = true
            launcher.launch(Manifest.permission.READ_CALENDAR)
        }
    }
    return remember(launcher) { { launcher.launch(Manifest.permission.READ_CALENDAR) } }
}

/** Explains why the calendar permission is needed and lets the user grant it */
@Composable
internal fun PermissionRequired(onRequest: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRequest) { Text(stringResource(R.string.grant_permission)) }
        OutlinedButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
            )
        }) { Text(stringResource(R.string.app_settings)) }
    }
}

@Composable
private fun BackupDialog(viewModel: ConfigViewModel<*>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val backupSaved = stringResource(R.string.backupsaved)
    val storageError = stringResource(R.string.externalstorageerror)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backupandrestore)) },
        text = { Text(stringResource(R.string.backuplocation)) },
        confirmButton = {
            TextButton(onClick = {
                val file = viewModel.backup()
                Toast.makeText(
                    context,
                    if (file != null) "$backupSaved ${file.path}" else storageError,
                    Toast.LENGTH_LONG
                ).show()
                onDismiss()
            }) { Text(stringResource(R.string.backup)) }
        },
        dismissButton = {
            TextButton(onClick = {
                val message = when (viewModel.restore()) {
                    Backup.RestoreResult.RESTORED -> R.string.restored
                    Backup.RestoreResult.NO_BACKUP -> R.string.nobackup
                    Backup.RestoreResult.ERROR -> R.string.externalstorageerror
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                onDismiss()
            }) { Text(stringResource(R.string.restore)) }
        },
    )
}

/**
 * Common frame of the configuration screens: top bar with cancel, done and menu, backup dialog
 * and, if [requirePermission], a calendar permission request instead of the content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigScaffold(
    viewModel: ConfigViewModel<*>,
    requirePermission: Boolean,
    requestPermission: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showBackup by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(android.R.string.cancel),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onDone) { Text(stringResource(R.string.done)) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painterResource(R.drawable.ic_more_vert),
                                contentDescription = stringResource(R.string.more_options),
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.backupandrestore)) },
                                onClick = {
                                    menuExpanded = false
                                    showBackup = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.moreapps)) },
                                onClick = {
                                    menuExpanded = false
                                    context.openUri("market://search?q=pub:j4velin")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.website)) },
                                onClick = {
                                    menuExpanded = false
                                    context.openUri("https://j4velin.de/contact.php")
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (requirePermission && !viewModel.hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { PermissionRequired(requestPermission) }
        } else {
            content(padding)
        }
    }
    if (showBackup) BackupDialog(viewModel) { showBackup = false }
}
