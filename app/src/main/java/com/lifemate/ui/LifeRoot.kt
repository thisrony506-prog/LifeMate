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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import androidx.lifecycle.*
import androidx.lifecycle.compose.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.lifemate.*
import com.lifemate.domain.*
import com.lifemate.navigation.destinations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun LifeRoot(activity: MainActivity, vm: LifeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val update by vm.updates.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val unlockedContent = rememberSaveableStateHolder()
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
    // Only public release metadata is requested; debug/instrumentation runs never auto-fetch.
    val lifecycleState by lifecycle.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(state.loading, locked, lifecycleState) {
        if (!state.loading && !locked &&
            lifecycleState == Lifecycle.State.RESUMED && !BuildConfig.DEBUG) {
            while (true) { vm.checkUpdates(); delay(6 * 60 * 60 * 1000L) }
        }
    }
    LaunchedEffect(state.preferences.soundEffects) { if(state.preferences.soundEffects) vm.feedback.prepare() else vm.feedback.stop() }
    LaunchedEffect(lifecycleState,locked) { vm.feedback.foreground=lifecycleState==Lifecycle.State.RESUMED && !locked; if(!vm.feedback.foreground) vm.feedback.stop() }
    fun navigate(path: String) {
        val destination = when (path) { "list/MISSION" -> "missions"; "list/MEMORY" -> "memories"; else -> path }
        if(destination=="posts" && route=="post/{id}") nav.popBackStack()
        nav.navigate(destination) { launchSingleTop = true }
    }
    LaunchedEffect(locked, update.available) { if (locked || update.available) drawer.close() }
    fun back() { if ((route.startsWith("edit") || route=="post/{id}")) discard = true else if (!nav.popBackStack()) navigate("home") }
    LaunchedEffect(Unit) { vm.events.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(Unit) { while (true) { today = LocalDate.now(); delay(30_000) } }
    DisposableEffect(lifecycle, lockRevision) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { stoppedAt = SystemClock.elapsedRealtime(); vm.stopUpdateDownload() }
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
    LaunchedEffect(activity.nativeRoute.value) {
        activity.nativeRoute.value?.let { navigate(it); activity.nativeRoute.value=null }
    }
    LaunchedEffect(activity.openItem.value, state.loading, state.profile, locked) {
        val id = activity.openItem.value
        if (id != null && !state.loading && state.profile != null && !locked) { activity.flutterMode.value=false; navigate("detail/$id"); activity.openItem.value = null }
    }
    if ((route.startsWith("edit") || route=="post/{id}") && !locked) BackHandler { discard = true }
    if (locked) BackHandler { activity.moveTaskToBack(true) }
    LifeTheme(state.preferences.theme) {
        val lightSystemBars = MaterialTheme.colorScheme.background.luminance() > .5f
        SideEffect {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = lightSystemBars
                isAppearanceLightNavigationBars = lightSystemBars
            }
        }
        Surface(Modifier.fillMaxSize().testTag("theme-${state.preferences.theme}"), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when {
                    state.loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) { BrandMark(64.dp); Text("LifeMate", style = MaterialTheme.typography.displaySmall); CircularProgressIndicator() }
                    state.error != null -> Column(Modifier.padding(26.dp).align(Alignment.Center)) { EmptyState(null, "Your data is still yours", state.error!!); Button({ activity.recreate() }) { Text("Try again") } }
                    state.profile == null && activity.flutterMode.value -> com.lifemate.flutter.FlutterHome(activity)
                    state.profile == null -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding -> Box(Modifier.padding(padding)) { ProfileEditor(null, vm) { } } }
                    else -> {
                        val showBottom = route in setOf("home", "missions", "memories", "list/{kind}", "calendar", "profile", "menu", "statistics")
                        // Dialogs have their own Android windows; hiding semantics alone cannot lock them.
                        // Remove private UI while locked, but retain form/navigation saveable state.
                        if (!locked) unlockedContent.SaveableStateProvider("private-content") {
                            if(activity.flutterMode.value && !update.available) com.lifemate.flutter.FlutterHome(activity)
                            else if(!activity.flutterMode.value) {
                            ModalNavigationDrawer(drawerState=drawer, gesturesEnabled=!update.available,
                                drawerContent={ SideMenu(if(route=="list/{kind}") "list/${entry?.arguments?.getString("kind")}" else route) { target -> scope.launch { drawer.close() }; navigate(target) } }) {
                            Scaffold(containerColor = MaterialTheme.colorScheme.background,
                                snackbarHost = { SnackbarHost(snackbar) },
                                topBar = {
                                    if (route != "home") TopAppBar(title = { BrandTitle() }, navigationIcon = { if (showBottom) IconButton({ scope.launch { drawer.open() } }) { Icon(Icons.Outlined.Menu,"Open menu") } else IconButton({ back() }) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Go back") } }, actions = {
                                        IconButton({ activity.flutterMode.value=true }) { Icon(Icons.Outlined.Spa,"Open Life Mate") }
                                        IconButton({ navigate("search") }) { Icon(Icons.Outlined.Search, "Search everything") }
                                        IconButton({ navigate("menu") }) { Icon(Icons.Outlined.GridView, "All features") }
                                    }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))

                                },
                                bottomBar = {
                                    if (showBottom) NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                                        destinations.forEach { destination ->
                                            val actualRoute = if (route == "list/{kind}") "list/${entry?.arguments?.getString("kind")}" else route
                                            NavigationBarItem(colors=NavigationBarItemDefaults.colors(selectedIconColor=MaterialTheme.colorScheme.primary,selectedTextColor=MaterialTheme.colorScheme.primary,indicatorColor=MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.testTag("nav-${destination.label}"), selected = actualRoute == destination.route, onClick = {
                                                nav.navigate(destination.route) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
                                            }, icon = { Icon(destination.icon, null) }, label = { Text(destination.label, maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)) })
                                        }
                                    }
                                }, floatingActionButton = { if (showBottom) Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { FloatingActionButton({activity.flutterMode.value=true},containerColor=MaterialTheme.colorScheme.primaryContainer) {Icon(Icons.Outlined.Spa,"Open Life Mate")}; FloatingActionButton({ adding = true }, shape = RoundedCornerShape(20.dp), containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Outlined.Add, "Create something new") } } }
                            ) { padding ->
                                Box(Modifier.padding(padding).fillMaxSize()) {
                                    NavHost(navController = nav, startDestination = "home") {
                                        composable("home") { HomeScreen(state, vm, today, ::navigate) { scope.launch { drawer.open() } } }
                                        composable("missions") { FeatureTheme(Kind.MISSION) { CollectionScreen(Kind.MISSION, state, vm, ::navigate) } }
                                        composable("memories") { MemoryGridScreen(state, ::navigate) }
                                        composable("list/{kind}") { backStack -> val kind = Kind.valueOf(backStack.arguments!!.getString("kind")!!); FeatureTheme(kind) { CollectionScreen(kind, state, vm, ::navigate) } }
                                        composable("calendar") { CalendarScreen(state, vm, ::navigate) }
                                        composable("profile") { ProfileScreen(state, ::navigate) }
                                        composable("edit-profile") { ProfileEditor(state.profile, vm) { nav.popBackStack() } }
                                        composable("menu") { MenuScreen(::navigate) }
                                        composable("search") { SearchScreen(state, ::navigate) }
                                        composable("statistics") { StatisticsScreen(state) }
                                        composable("settings") { SettingsScreen(activity, state, vm, ::navigate, { lockRevision++ }) { nav.navigate("home") { popUpTo("home") { inclusive = true } } } }
                                        composable("updates") { UpdateScreen(state, vm) }
                                        composable("studio") { BirthdayTheme { CardStudioScreen(vm = vm) } }
                                        composable("about") { AboutScreen(::navigate) }
                                        composable("posts") { FacebookPostsScreen(vm, ::navigate) }
                                        composable("post/{id}") { PostComposerScreen(it.arguments?.getString("id") ?: "new", vm, ::navigate) }
                                        composable("post-card/{id}") { PostGraphicScreen(it.arguments?.getString("id") ?: "",vm) }
                                        composable("privacy") { PolicyScreen(true) }
                                        composable("terms") { PolicyScreen(false) }
                                        composable("notifications") { NotificationsScreen(state, vm, ::navigate) }
                                        composable("detail/{id}") { backStack ->
                                            val item = state.items.firstOrNull { it.id == backStack.arguments?.getString("id") }
                                            if (item != null) DetailScreen(item, state, vm, ::navigate) { nav.popBackStack() }
                                            else EmptyState(null, "This record isn't here", "It may have been deleted or replaced by a backup.", "Go home") { navigate("home") }
                                        }
                                        composable("wish/{id}") { backStack ->
                                            val person = state.items.firstOrNull { it.id == backStack.arguments?.getString("id") }
                                            if (person != null) BirthdayTheme { WishScreen(person, vm) }
                                            else EmptyState(Kind.BIRTHDAY, "Birthday not found", "This person's record may have been deleted.", "Birthdays") { navigate("list/BIRTHDAY") }
                                        }
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
                        }
                        }
                        if (locked) Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { LockScreen(activity, vm.app.secure, state.preferences.biometric) { locked = false; stoppedAt = 0 } }
                    }
                }
                if (adding && !locked && !update.available) ModalBottomSheet(onDismissRequest = { adding = false }, containerColor = MaterialTheme.colorScheme.background) {
                    LazyColumn(contentPadding = PaddingValues(22.dp, 0.dp, 22.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { PageHeading("Create", ""); Spacer(Modifier.height(12.dp)) }
                        item { TextButton({ adding=false; navigate("post/new") },Modifier.fillMaxWidth()) { Icon(Icons.Outlined.EditNote,null); Text("  Facebook Post") } }
                        items(Kind.entries) { kind -> Surface(onClick = { adding = false; navigate("edit/${kind.name}/new") }, shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { KindBadge(kind); Text("New ${kind.label.lowercase()}", style = MaterialTheme.typography.titleMedium) } } }
                    }
                }
                if (!state.loading && !locked && update.available) RequiredUpdateGate(update, vm) { activity.moveTaskToBack(true) }
                if (discard && !locked && !update.available) ConfirmDialog("Leave without saving?", "Unsaved edits on this screen will be discarded.", "Discard edits", { discard = false }) { discard = false; nav.popBackStack() }
            }
        }
    }
}
