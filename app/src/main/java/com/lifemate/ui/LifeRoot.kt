package com.lifemate.ui

import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.lifemate.*
import com.lifemate.domain.*
import com.lifemate.navigation.destinations
import kotlinx.coroutines.delay
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LifeRoot(activity: MainActivity, vm: LifeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"
    val snackbar = remember { SnackbarHostState() }
    var adding by remember { mutableStateOf(false) }
    var lockRevision by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(vm.app.secure.hasPin()) }
    var stoppedAt by remember { mutableLongStateOf(0L) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var discard by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current
    fun navigate(path: String) { nav.navigate(path) { launchSingleTop = true } }
    fun back() { if (route.startsWith("edit")) discard = true else if (!nav.popBackStack()) navigate("home") }
    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { while (true) { today = LocalDate.now(); delay(30_000) } }
    DisposableEffect(lifecycle, lockRevision) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) stoppedAt = SystemClock.elapsedRealtime()
            if (event == Lifecycle.Event.ON_START) {
                if (vm.app.secure.hasPin() && stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt >= 30_000) locked = true
                today = LocalDate.now()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lockRevision) {
        if (vm.app.secure.hasPin()) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE); locked = false }
    }
    LaunchedEffect(activity.openItem.value, state.loading, state.profile, locked) {
        val id = activity.openItem.value
        if (id != null && !state.loading && state.profile != null && !locked) { navigate("detail/$id"); activity.openItem.value = null }
    }
    if (route.startsWith("edit") && !locked) BackHandler { discard = true }
    if (locked) BackHandler { activity.moveTaskToBack(true) }
    LifeTheme(state.preferences.theme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when {
                    state.loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) { Icon(Icons.Outlined.Spa, null, Modifier.size(54.dp), tint = MaterialTheme.colorScheme.primary); Text("LifeMate", style = MaterialTheme.typography.displaySmall); CircularProgressIndicator() }
                    state.error != null -> Column(Modifier.padding(26.dp).align(Alignment.Center)) { EmptyState(null, "Your data is still yours", state.error!!); Button({ activity.recreate() }) { Text("Try again") } }
                    state.profile == null -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding -> Box(Modifier.padding(padding)) { ProfileEditor(null, vm) { navigate("home") } } }
                    else -> {
                        val showBottom = route in setOf("home", "list/{kind}", "calendar", "profile", "menu", "statistics")
                        Box(if (locked) Modifier.clearAndSetSemantics { } else Modifier) {
                            Scaffold(containerColor = MaterialTheme.colorScheme.background,
                                snackbarHost = { SnackbarHost(snackbar) },
                                topBar = {
                                    if (route != "home") TopAppBar(title = { Text("LifeMate", style = MaterialTheme.typography.titleLarge) }, navigationIcon = { IconButton({ back() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Go back") } }, actions = {
                                        IconButton({ navigate("search") }) { Icon(Icons.Outlined.Search, "Search everything") }
                                        IconButton({ navigate("menu") }) { Icon(Icons.Outlined.GridView, "All features") }
                                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
                                    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton({ navigate("search") }) { Icon(Icons.Outlined.Search, null, Modifier.size(18.dp)); Text(" Search") }
                                        TextButton({ navigate("menu") }) { Icon(Icons.Outlined.GridView, null, Modifier.size(18.dp)); Text(" Explore") }
                                    }
                                },
                                bottomBar = {
                                    if (showBottom) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                                        destinations.forEach { destination ->
                                            val actualRoute = if (route == "list/{kind}") "list/${entry?.arguments?.getString("kind")}" else route
                                            NavigationBarItem(selected = actualRoute == destination.route, onClick = {
                                                nav.navigate(destination.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                                            }, icon = { Icon(destination.icon, null) }, label = { Text(destination.label, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified)) })
                                        }
                                    }
                                }, floatingActionButton = { if (showBottom) FloatingActionButton({ adding = true }, shape = RoundedCornerShape(20.dp), containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Outlined.Add, "Create something new") } }
                            ) { padding ->
                                Box(Modifier.padding(padding).fillMaxSize()) {
                                    NavHost(navController = nav, startDestination = "home") {
                                        composable("home") { HomeScreen(state, vm, today, ::navigate) }
                                        composable("list/{kind}") { backStack -> CollectionScreen(Kind.valueOf(backStack.arguments!!.getString("kind")!!), state, vm, ::navigate) }
                                        composable("calendar") { CalendarScreen(state, vm, ::navigate) }
                                        composable("profile") { ProfileScreen(state, ::navigate) }
                                        composable("edit-profile") { ProfileEditor(state.profile, vm) { nav.popBackStack() } }
                                        composable("menu") { MenuScreen(::navigate) }
                                        composable("search") { SearchScreen(state, ::navigate) }
                                        composable("statistics") { StatisticsScreen(state) }
                                        composable("settings") { SettingsScreen(activity, state, vm, ::navigate, { lockRevision++ }) { nav.navigate("home") { popUpTo("home") { inclusive = true } } } }
                                        composable("privacy") { PolicyScreen(true) }
                                        composable("terms") { PolicyScreen(false) }
                                        composable("notifications") { NotificationsScreen(state, vm, ::navigate) }
                                        composable("detail/{id}") { backStack ->
                                            val item = state.items.firstOrNull { it.id == backStack.arguments?.getString("id") }
                                            if (item != null) DetailScreen(item, state, vm, ::navigate) { nav.popBackStack() }
                                            else EmptyState(null, "This record isn't here", "It may have been deleted or replaced by a backup.", "Go home") { navigate("home") }
                                        }
                                        composable("wish/{id}") { backStack -> state.items.firstOrNull { it.id == backStack.arguments?.getString("id") }?.let { WishScreen(it, vm) } }
                                        composable("edit/{kind}/{id}?date={date}", arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })) { backStack ->
                                            val kind = Kind.valueOf(backStack.arguments!!.getString("kind")!!)
                                            val id = backStack.arguments!!.getString("id")!!
                                            val original = state.items.firstOrNull { it.id == id }
                                            if (id == "new" || original != null) EditorScreen(kind, original, backStack.arguments?.getString("date"), state, vm) { saved ->
                                                nav.popBackStack(); navigate("detail/$saved")
                                            } else EmptyState(null, "Record not found", "Go back and choose another record.")
                                        }
                                    }
                                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                                }
                            }
                        }
                        if (locked) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { LockScreen(activity, vm.app.secure, state.preferences.biometric) { locked = false; stoppedAt = 0 } }
                    }
                }
                if (adding && !locked) ModalBottomSheet(onDismissRequest = { adding = false }, containerColor = MaterialTheme.colorScheme.background) {
                    LazyColumn(contentPadding = PaddingValues(22.dp, 0.dp, 22.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { PageHeading("Make a little space", "What would you like to add?"); Spacer(Modifier.height(12.dp)) }
                        items(Kind.entries) { kind -> Surface(onClick = { adding = false; navigate("edit/${kind.name}/new") }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { KindBadge(kind); Text("New ${kind.label.lowercase()}", style = MaterialTheme.typography.titleMedium) } } }
                    }
                }
                if (discard) ConfirmDialog("Leave without saving?", "Unsaved edits on this screen will be discarded.", "Discard edits", { discard = false }) { discard = false; nav.popBackStack() }
            }
        }
    }
}
