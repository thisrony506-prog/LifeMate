package com.lifemate.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifemate.BuildConfig
import com.lifemate.updates.UpdateState

@Composable fun UpdateBanner(update: UpdateState, onOpen: () -> Unit) {
    if (update.available) {
        SoftCard(Modifier.fillMaxWidth().testTag("update-banner"), MaterialTheme.colorScheme.primaryContainer) {
            Text("LifeMate ${update.release!!.version} is ready", style = MaterialTheme.typography.titleMedium)
            Text("A newer version is available. Keep your data and update in place.", style = MaterialTheme.typography.bodyMedium)
            Button(onOpen) { Text("View update") }
        }
    } else {
        TextButton(onOpen, Modifier.testTag("open-updates")) { Text("Version ${BuildConfig.VERSION_NAME} · Updates") }
    }
}

@Composable fun UpdateScreen(state: LifeState, vm: LifeViewModel) {
    val update by vm.updates.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        PageHeading("Keep LifeMate up to date", "Your data stays. Your LifeMate gets better.")
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Installed: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.titleMedium)
            if (update.checking) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Checking the official release…") }
            update.message?.let { Text(it, Modifier.testTag("update-status")) }
            if (update.available) {
                Text("Available: ${update.release!!.version}", style = MaterialTheme.typography.titleLarge)
                Button({
                    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.release!!.downloadUrl))) }
                    catch (_: ActivityNotFoundException) { vm.message("No browser is available. Install a browser to download the update.") }
                }, Modifier.fillMaxWidth().testTag("download-update")) { Text("Download update") }
            }
            OutlinedButton({ vm.checkUpdates(true) }, enabled = !update.checking, modifier = Modifier.fillMaxWidth()) { Text("Check for updates") }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            ToggleRow("Automatic update checks", "Checks public GitHub release information when you open the app, at most once every 6 hours.", state.preferences.automaticUpdates) { vm.preference("automaticUpdates", it) }
            Text("Only the app version and normal connection information (such as your IP address) reach GitHub. Your notes, profile and media are never sent. All organizer features work offline.", style = MaterialTheme.typography.bodyMedium)
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Text("Install safely", style = MaterialTheme.typography.titleMedium)
            Text("Download the APK, open it from your browser or Downloads, and confirm Android's Update prompt. If asked, allow installs from that browser for this download. LifeMate cannot install silently.")
            Text("Do not uninstall to update: uninstalling deletes local data. Official updates use the same app ID and signing key with a higher version code. Keep a private backup before important updates.")
            Text("Requires Android 8.0 or newer. Old unsigned downloads cannot be installed. If Android reports a conflicting package/signature, stop and back up any installed app before changing it.")
        }
        Spacer(Modifier.height(24.dp))
    }
}
