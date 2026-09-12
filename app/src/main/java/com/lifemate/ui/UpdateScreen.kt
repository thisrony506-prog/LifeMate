package com.lifemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifemate.BuildConfig
import com.lifemate.updates.*

@Composable fun UpdateScreen(state: LifeState, vm: LifeViewModel) {
    val update by vm.updates.collectAsStateWithLifecycle()
    val downloadState by vm.apkDownload.collectAsStateWithLifecycle()
    val download = if (downloadState.code == update.release?.code) downloadState else ApkDownloadState()
    val installer = rememberNativeInstaller(vm)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeading("Keep LifeMate up to date", "Your data stays. Your LifeMate gets better.")
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Installed: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.titleMedium)
            if (update.checking) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Checking the official release…") }
            update.message?.let { Text(it, Modifier.testTag("update-status")) }
            if (update.available) {
                Text("Available: ${update.release!!.version}", style = MaterialTheme.typography.titleLarge)
                DownloadProgress(download)
                installer.message?.let { Text(it) }
                NativeDownloadButton(download,vm::downloadUpdate,installer)

            }
            OutlinedButton({ vm.checkUpdates(true) }, enabled = !update.checking, modifier = Modifier.fillMaxWidth()) { Text("Check for updates") }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Automatic update checks", style = MaterialTheme.typography.titleMedium)
            Text("Checks the official release when you open LifeMate, at most once every 6 hours. A verified newer version requires installation before continuing. No internet or a server error alone never triggers an update lock.")
            Text("Only the app version and normal connection information (such as your IP address) reach GitHub. Your notes, profile and media are never sent. Organizer features work offline unless a verified newer version is already known.", style = MaterialTheme.typography.bodyMedium)
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Install safely", style = MaterialTheme.typography.titleMedium)
            Text("Download directly inside LifeMate and tap Install update. If asked, allow LifeMate in Android's Install unknown apps settings, return here and tap Install update again. Confirm Android's Update prompt. No browser is needed; LifeMate cannot install silently.")
            Text("Do not uninstall to update: uninstalling deletes local data. Official updates use the same app ID and signing key with a higher version code. Keep a private backup before important updates.")
            Text("Requires Android 8.0 or newer. Old unsigned downloads cannot be installed. If Android reports a conflicting package/signature, stop and back up any installed app before changing it.")
        }
        Spacer(Modifier.height(24.dp))
    }
}
