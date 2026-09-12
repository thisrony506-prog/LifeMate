package com.lifemate.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lifemate.updates.UpdateState
import java.time.LocalDate

@Composable fun RequiredUpdateDialog(update: UpdateState, onDownload: () -> Unit, onBackup: () -> Unit, onClose: () -> Unit, error: String? = null) {
    if (!update.available) return
    AlertDialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        modifier = Modifier.testTag("required-update"),
        title = { Text("A fresh LifeMate is ready") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Version ${update.release!!.version} · verified official update", style = MaterialTheme.typography.titleMedium)
            Text("Download and install this version to continue. Opening the download alone does not complete the update.")
            Text("Your records stay on this device. Open the APK in Downloads and choose Android's Update. Do not uninstall LifeMate.")
            Text("A backup remains available before updating. Android will always ask you to confirm installation.")
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onBackup) { Text("Export a private backup") }
        } },
        confirmButton = { Button(onDownload, Modifier.testTag("required-download")) { Text("Download update") } },
        dismissButton = { TextButton(onClose) { Text("Close app") } })
}

@Composable fun RequiredUpdateGate(update: UpdateState, vm: LifeViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var backup by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri)
    }
    RequiredUpdateDialog(update, {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.release!!.downloadUrl))); error = null }
        catch (_: Exception) { error = "No browser could open this download. Enable a browser and try again." }
    }, { backup = true }, onClose, error)
    if (backup) ConfirmDialog("Export before updating?", "The ZIP contains your private records and media and is NOT encrypted. Save it in a private location. Nothing is uploaded automatically.", "Choose location", { backup = false }) {
        backup = false; export.launch("LifeMate-backup-${LocalDate.now()}.zip")
    }
}
