package com.lifemate.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lifemate.updates.*
import kotlinx.coroutines.*
import java.util.Locale

@Composable fun DownloadProgress(download: ApkDownloadState) {
    if (download.phase == DownloadPhase.DOWNLOADING) {
        LinearProgressIndicator(progress={ download.progress },modifier=Modifier.fillMaxWidth().testTag("apk-download-progress"))
        Text("${(download.progress*100).toInt()}% · ${String.format(Locale.ROOT,"%.1f",download.received/1048576.0)} / ${String.format(Locale.ROOT,"%.1f",download.total/1048576.0)} MB",Modifier.testTag("apk-download-percent"))
        Text("Keep LifeMate open while downloading. Leaving the app stops an unfinished download; retry starts from the beginning.",style=MaterialTheme.typography.bodyMedium)
    } else if (download.phase == DownloadPhase.VERIFYING) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("Verifying size, SHA-256, app version and signing key…")
    }
    download.message?.let { Text(it,style=MaterialTheme.typography.bodyMedium) }
}

data class NativeInstallUi(val install: () -> Unit, val preparing: Boolean, val message: String?)

@Composable fun rememberNativeInstaller(vm: LifeViewModel): NativeInstallUi {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var preparing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        message = if (context.packageManager.canRequestPackageInstalls()) "Permission is ready. Tap Install update to continue."
            else "Installation permission was not allowed. Tap Install update to try again; your records are unchanged."
    }
    return NativeInstallUi(install = {
        if (!preparing) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                message = "Allow LifeMate as an installation source, then return and tap Install update."
                try { permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:${context.packageName}"))) }
                catch (_: Exception) { message = "Open Android Settings → Install unknown apps → LifeMate, allow this source, then return here." }
            } else scope.launch {
                preparing = true; message = "Rechecking the downloaded APK…"
                try {
                    val file = vm.verifiedUpdateFile()
                    context.startActivity(nativeUpdateIntent(context,file))
                    message = "Confirm Android's Update prompt. If cancelled, tap Install update again."
                } catch (e: CancellationException) { throw e }
                catch (_: Exception) { message = "Android could not open the verified update. Retry the download or check installation permission." }
                finally { preparing = false }
            }
        }
    },preparing=preparing,message=message)
}

@Composable fun NativeDownloadButton(download: ApkDownloadState, onDownload: () -> Unit, installer: NativeInstallUi) {
    if (download.phase == DownloadPhase.READY) {
        Button(installer.install,enabled=!installer.preparing,modifier=Modifier.testTag("install-update")) { Text(if(installer.preparing) "Verifying…" else "Install update") }
    } else {
        Button(onDownload,enabled=!download.busy,modifier=Modifier.testTag("required-download")) {
            Text(when(download.phase) { DownloadPhase.DOWNLOADING -> "Downloading…"; DownloadPhase.VERIFYING -> "Verifying…"; DownloadPhase.ERROR -> "Retry download"; else -> "Download update" })
        }
    }
}
