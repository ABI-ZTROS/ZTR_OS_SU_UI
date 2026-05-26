package com.ztros.ztrosu.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.ztros.ztrosu.R
import com.ztros.ztrosu.ui.LocalScrollState
import com.ztros.ztrosu.ui.component.rememberLoadingDialog
import com.ztros.ztrosu.ui.rememberScrollConnection
import com.ztros.ztrosu.ui.util.LocalSnackbarHost
import com.ztros.ztrosu.ui.util.ShellUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ZTR_OS SU UI-Only Mode - Backup & Restore Screen
 * Simplified version without libsu dependencies
 */
@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun BackupRestoreScreen(navigator: DestinationsNavigator) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackBarHost = LocalSnackbarHost.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadingDialog = rememberLoadingDialog()

    val scrollState = LocalScrollState.current
    val isNavBarHidden = scrollState?.isScrollingDown?.value ?: false
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + if (isNavBarHidden) 0.dp else 112.dp

    // Backup launcher
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/x-tar")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = loadingDialog.withLoading {
                    backupModulesToUri(context, it)
                }
                if (success) {
                    snackBarHost.showSnackbar(context.getString(R.string.backup_success))
                } else {
                    snackBarHost.showSnackbar(context.getString(R.string.backup_failed))
                }
            }
        }
    }

    // Restore launcher
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = loadingDialog.withLoading {
                    restoreModulesFromUri(context, it)
                }
                if (success) {
                    snackBarHost.showSnackbar(context.getString(R.string.restore_success))
                } else {
                    snackBarHost.showSnackbar(context.getString(R.string.restore_failed))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopBar(
                onBack = dropUnlessResumed { navigator.popBackStack() },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackBarHost, modifier = Modifier.padding(bottom = navBarPadding)) },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .let { modifier ->
                    val bottomBarScrollState = LocalScrollState.current
                    val bottomBarScrollConnection = bottomBarScrollState?.let {
                        rememberScrollConnection(
                            isScrollingDown = it.isScrollingDown,
                            scrollOffset = it.scrollOffset,
                            previousScrollOffset = it.previousScrollOffset,
                            threshold = 30f
                        )
                    }
                    if (bottomBarScrollConnection != null) {
                        modifier
                            .nestedScroll(bottomBarScrollConnection)
                            .nestedScroll(scrollBehavior.nestedScrollConnection)
                    } else {
                        modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.backup_modules),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.backup_modules_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                            backupLauncher.launch("modules_backup_$timestamp.tar")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.backup))
                    }
                }
            }

            // Restore Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.restore_modules),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.restore_modules_summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { restoreLauncher.launch(arrayOf("application/x-tar", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Upload,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.restore))
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.note),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(R.string.backup_restore_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

/**
 * Backup modules to URI (UI-Only mode - simulated)
 */
private suspend fun backupModulesToUri(context: android.content.Context, destUri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        // In UI-Only mode, we create a dummy tar file for testing
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val tmpFile = File(context.cacheDir, "modules_backup_$timestamp.tar")
        
        // Create empty tar file for testing
        tmpFile.writeBytes(byteArrayOf())
        
        // Copy to destination
        context.contentResolver.openOutputStream(destUri)?.use { output ->
            tmpFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        
        tmpFile.delete()
        true
    }.getOrDefault(false)
}

/**
 * Restore modules from URI (UI-Only mode - simulated)
 */
private suspend fun restoreModulesFromUri(context: android.content.Context, sourceUri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        // In UI-Only mode, just verify the file exists
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            // Read first few bytes to verify it's a valid file
            val header = ByteArray(512)
            val read = input.read(header)
            read > 0
        } ?: false
    }.getOrDefault(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    onBack: () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.backup_restore),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        windowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        scrollBehavior = scrollBehavior
    )
}

@Preview
@Composable
private fun BackupRestorePreview() {
    BackupRestoreScreen(EmptyDestinationsNavigator)
}
