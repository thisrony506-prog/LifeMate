package com.lifemate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.lifemate.domain.Kind

@Composable fun SideMenu(route: String, navigate: (String) -> Unit) {
    ModalDrawerSheet(drawerContainerColor=MaterialTheme.colorScheme.surface,modifier=Modifier.widthIn(max=320.dp)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) { BrandMark(36.dp,Modifier.clearAndSetSemantics {}); Text("LifeMate",style=MaterialTheme.typography.titleLarge) }
            Text("Your Personal Life Assistant",Modifier.padding(horizontal=12.dp),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(18.dp))
            Eyebrow("Plan & grow")
            listOf(Kind.ROUTINE,Kind.HABIT,Kind.REMINDER,Kind.GOAL).forEach { kind -> DrawerEntry(kind.plural,kind.icon(),"list/${kind.name}",route,navigate) }
            Spacer(Modifier.height(8.dp)); Eyebrow("Keep & create")
            listOf(Kind.BIRTHDAY,Kind.NOTE).forEach { kind -> DrawerEntry(kind.plural,kind.icon(),"list/${kind.name}",route,navigate) }
            DrawerEntry("Facebook Posts",Icons.Outlined.EditNote,"posts",route,navigate)
            DrawerEntry("Statistics",Icons.Outlined.Insights,"statistics",route,navigate)
            HorizontalDivider(Modifier.padding(vertical=12.dp))
            DrawerEntry("Settings",Icons.Outlined.Settings,"settings",route,navigate)
            DrawerEntry("About",Icons.Outlined.Info,"about",route,navigate)
        }
    }
}
@Composable private fun DrawerEntry(label: String, icon: ImageVector, destination: String, route: String, navigate: (String)->Unit) {
    NavigationDrawerItem(colors=NavigationDrawerItemDefaults.colors(selectedContainerColor=MaterialTheme.colorScheme.primaryContainer,selectedIconColor=MaterialTheme.colorScheme.onPrimaryContainer,selectedTextColor=MaterialTheme.colorScheme.onPrimaryContainer),label={Text(label)},icon={Icon(icon,null)},selected=route==destination,onClick={navigate(destination)},modifier=Modifier.testTag("drawer-$destination"))
}
@Composable fun AboutScreen(navigate: (String)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        BrandMark(64.dp); Text("LifeMate",style=MaterialTheme.typography.headlineLarge)
        Text("Your Personal Life Assistant")
        Text("Version ${com.lifemate.BuildConfig.VERSION_NAME}",color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Private by default. Made for your everyday life.")
        OutlinedButton({navigate("privacy")},Modifier.fillMaxWidth()) {Text("Privacy & security")}
        OutlinedButton({navigate("terms")},Modifier.fillMaxWidth()) {Text("Terms & reliability")}
        OutlinedButton({navigate("updates")},Modifier.fillMaxWidth()) {Text("App updates")}
    }
}
