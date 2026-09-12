package com.lifemate.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import com.lifemate.database.Profile
import java.time.LocalDate

@Composable fun ProfileEditor(profile: Profile?, vm: LifeViewModel, onDone: () -> Unit) {
    val onboarding = profile == null
    var draft by rememberSaveable(profile?.id) { mutableStateOf(profile ?: Profile()) }
    val busy by vm.busy.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.runAction { val (path, _) = vm.repo.copyMedia(uri); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { draft = draft.copy(photo = path) } }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(26.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        if (onboarding) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BrandMark(92.dp)
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("LifeMate", style = MaterialTheme.typography.displaySmall)
                Text("Your Personal Life Assistant.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            PageHeading("A little more you.", "A calmer day starts here. Let's make this space yours.")
        } else PageHeading("Your personal space", "The little details that make you, you.")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(draft, 72.dp)
            Column { TextButton({ picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Outlined.AddAPhoto, null); Text(" Add profile photo") }; if (draft.photo.isNotBlank()) TextButton({ draft = draft.copy(photo = "") }) { Text("Remove photo") } }
        }
        Field(draft.fullName, { draft = draft.copy(fullName = it.take(150)) }, "Full name *")
        Field(draft.preferredName, { draft = draft.copy(preferredName = it.take(80)) }, "What should we call you? (optional)")
        Field(draft.nickname, { draft = draft.copy(nickname = it.take(80)) }, "Nickname (optional)")
        DateField("Your birthday", draft.birthday, optional = true) { draft = draft.copy(birthday = it) }
        Field(draft.introduction, { draft = draft.copy(introduction = it) }, "A little about you (optional)", singleLine = false, minLines = 2)
        Field(draft.information, { draft = draft.copy(information = it) }, "Important personal information (optional)", singleLine = false, minLines = 2)
        SoftCard(color = MaterialTheme.colorScheme.primaryContainer) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Outlined.Lock, null); Text("Just yours. Your profile and records are encrypted on this device. No account, cloud upload, or tracking.", style = MaterialTheme.typography.bodyMedium) }
        }
        Button({
            if (draft.fullName.isBlank()) vm.message("Please enter your name. Everything else is optional.")
            else if (draft.birthday.isNotBlank() && LocalDate.parse(draft.birthday) > LocalDate.now()) vm.message("Your birthday can't be in the future.")
            else vm.runAction(if (onboarding) "Welcome to LifeMate, ${draft.displayName}." else "Profile updated", onDone) { vm.repo.dao.saveProfile(draft.copy(fullName = draft.fullName.trim())) }
        }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) { Text(if (busy) "Saving…" else if (onboarding) "Make yourself at home  →" else "Save profile") }
        if (onboarding) Text("Only your name is required. Leave any optional field blank—you can always come back later.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(30.dp))
    }
}
@Composable fun ProfileScreen(state: LifeState, navigate: (String) -> Unit) {
    val profile = state.profile ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        PageHeading("Your personal space", "One life. A little more intention.")
        SoftCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Avatar(profile, 76.dp)
                Column { Text(profile.fullName, style = MaterialTheme.typography.headlineMedium); Text(profile.nickname.ifBlank { "It's good to have you here." }, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (profile.introduction.isNotBlank()) Text(profile.introduction)
            if (profile.birthday.isNotBlank()) Text("Birthday · ${LocalDate.parse(profile.birthday).format(dateFormat)}")
            Button({ navigate("edit-profile") }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Edit, null); Text(" Edit your profile") }
        }
        if (profile.information.isNotBlank()) SoftCard(Modifier.fillMaxWidth()) { Eyebrow("PERSONAL INFORMATION"); Text(profile.information) }
        SoftCard(Modifier.fillMaxWidth(), MaterialTheme.colorScheme.primaryContainer) { Eyebrow("YOUR JOURNEY SO FAR"); Text("${state.completions.size} little steps forward", style = MaterialTheme.typography.headlineMedium); Text("Every one of them counts.") }
        OutlinedButton({ navigate("statistics") }, Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Outlined.Insights, null); Text(" Your progress & statistics") }
        OutlinedButton({ navigate("menu") }, Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Outlined.GridView, null); Text(" All your LifeMate features") }
        OutlinedButton({ navigate("settings") }, Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Icon(Icons.Outlined.Settings, null); Text(" Settings & privacy") }
        Spacer(Modifier.height(80.dp))
    }
}
