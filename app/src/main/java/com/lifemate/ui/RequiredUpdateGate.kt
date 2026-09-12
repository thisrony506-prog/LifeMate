package com.lifemate.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lifemate.updates.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable fun RequiredUpdateDialog(update: UpdateState, onDownload: () -> Unit, onBackup: () -> Unit, onClose: () -> Unit, error: String? = null, download: ApkDownloadState = ApkDownloadState(), onInstall: () -> Unit = onDownload, installing: Boolean = false, onStop: () -> Unit = {}) {
    if (!update.available) return
    AlertDialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        modifier = Modifier.testTag("required-update"),
        title = { Text("A fresh LifeMate is ready") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Version ${update.release!!.version} · verified official update", style = MaterialTheme.typography.titleMedium)
            Text("Download here inside LifeMate, then tap Install update. No browser is needed. Installing the new version is required to continue.")
            Text("Your records stay on this device. Android will open its installation screen. If asked, allow LifeMate as an installation source. Do not uninstall LifeMate.")
            DownloadProgress(download)
            if (download.busy) TextButton(onStop) { Text("Stop download") }
            Text("A backup remains available before updating. Android will always ask you to confirm installation.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onBackup) { Text("Export a private backup") }
        } },
        confirmButton = { NativeDownloadButton(download,onDownload,NativeInstallUi(onInstall,installing,null)) },
        dismissButton = { TextButton(onClose) { Text("Close app") } })
}

@Composable fun RequiredUpdateGate(update: UpdateState, vm: LifeViewModel, onClose: () -> Unit) {
    val state by vm.apkDownload.collectAsStateWithLifecycle()
    val download = if (state.code == update.release?.code) state else ApkDownloadState()
    val installer = rememberNativeInstaller(vm)
    var backup by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri)
    }
    RequiredUpdateDialog(update, vm::downloadUpdate, { backup = true }, { vm.stopUpdateDownload(); onClose() },
        installer.message, download, installer.install, installer.preparing, vm::stopUpdateDownload)
    if (backup) ConfirmDialog("Export before updating?", "The ZIP contains your private records and media and is NOT encrypted. Save it in a private location. Nothing is uploaded automatically.", "Choose location", { backup = false }) {
        backup = false; export.launch("LifeMate-backup-${LocalDate.now()}.zip")
    }
}
