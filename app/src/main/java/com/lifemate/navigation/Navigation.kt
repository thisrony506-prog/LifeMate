package com.lifemate.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

data class Destination(val route: String, val label: String, val icon: ImageVector)
val destinations = listOf(Destination("home", "Home", Icons.Outlined.Home), Destination("list/MISSION", "Missions", Icons.Outlined.Flag),
    Destination("calendar", "Calendar", Icons.Outlined.CalendarMonth), Destination("list/MEMORY", "Memories", Icons.Outlined.PhotoLibrary), Destination("profile", "Profile", Icons.Outlined.Person))
