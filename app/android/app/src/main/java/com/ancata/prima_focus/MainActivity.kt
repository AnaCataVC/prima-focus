package com.ancata.prima_focus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ancata.prima_focus.ui.screens.HomeScreen
import com.ancata.prima_focus.ui.screens.InboxModal
import com.ancata.prima_focus.ui.screens.TimerScreen
import com.ancata.prima_focus.ui.screens.QuickReviewModal
import com.ancata.prima_focus.ui.screens.SettingsScreen
import com.ancata.prima_focus.ui.screens.TaskListScreen
import com.ancata.prima_focus.ui.theme.PrimaFocusTheme
import com.ancata.prima_focus.ui.viewmodel.TaskViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.ancata.prima_focus.ui.screens.TabletDashboardScreen
import kotlinx.coroutines.launch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.ancata.prima_focus.worker.NotificationWorker
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow

import android.content.Context
class MainActivity : ComponentActivity() {

    private val pendingIntentAction = MutableStateFlow<Intent?>(null)

    @android.annotation.SuppressLint("InvalidFragmentVersionForActivityResult")
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle if needed
    }

    private fun askPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestPermissionLauncher.launch(ungranted.toTypedArray())
        }
    }

    private fun setupWorkManager() {
        val sharedPrefs = getSharedPreferences(com.ancata.prima_focus.utils.Constants.PREF_FILE, android.content.Context.MODE_PRIVATE)
        val frequency = sharedPrefs.getInt(com.ancata.prima_focus.utils.Constants.PREF_NOTIFICATION_FREQUENCY, com.ancata.prima_focus.utils.Constants.DEFAULT_NOTIFICATION_FREQUENCY)
        val workManager = WorkManager.getInstance(this)
        if (frequency <= 0) {
            workManager.cancelUniqueWork("NotificationWorker")
            return
        }
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(frequency.toLong(), TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            "NotificationWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        askPermissions()
        setupWorkManager()
        
        intent?.let { pendingIntentAction.value = it }
        
        setContent {
            PrimaFocusTheme {
                val windowSizeClass = calculateWindowSizeClass(this).widthSizeClass
                MainApp(pendingIntentAction, windowSizeClass)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingIntentAction.value = intent
    }
}

@Composable
fun MainApp(
    pendingIntentAction: MutableStateFlow<Intent?>,
    windowSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val navController = rememberNavController()
    val viewModel: TaskViewModel = viewModel()
    var showInboxModal by remember { mutableStateOf(false) }
    var prefilledCategory by remember { mutableStateOf<String?>(null) }
    var taskToEdit by remember { mutableStateOf<com.ancata.prima_focus.data.local.entity.TaskEntity?>(null) }
    var taskForReview by remember { mutableStateOf<String?>(null) }
    var navigateToTimer by remember { mutableStateOf<Intent?>(null) }
    
    val currentIntent by pendingIntentAction.collectAsState()

    LaunchedEffect(currentIntent) {
        currentIntent?.let { intent ->
            when (intent.action) {
                "com.ancata.prima_focus.ACTION_ADD_TASK" -> {
                    prefilledCategory = intent.getStringExtra(com.ancata.prima_focus.utils.Constants.EXTRA_PREFILLED_CATEGORY)
                    showInboxModal = true
                }
                "com.ancata.prima_focus.ACTION_START_TIMER" -> {
                    navigateToTimer = intent
                }
            }
            pendingIntentAction.value = null
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    selected = currentRoute == "home",
                    onClick = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Tareas") },
                    label = { Text("Tareas") },
                    selected = currentRoute == "list",
                    onClick = {
                        navController.navigate("list") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showInboxModal = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Inbox", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.weight(1f)
            ) {
                composable("home") {
                    if (windowSizeClass == WindowWidthSizeClass.Compact) {
                        HomeScreen(
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState,
                            onStartTimer = { id, title, minutes ->
                                val encodedTitle = android.net.Uri.encode(title)
                                navController.navigate("timer/$id/$encodedTitle/$minutes")
                            },
                            onEditTask = { task ->
                                taskToEdit = task
                                showInboxModal = true
                            },
                            onRequestReview = { taskId ->
                                taskForReview = taskId
                            }
                        )
                    } else {
                        TabletDashboardScreen(
                            viewModel = viewModel,
                            snackbarHostState = snackbarHostState,
                            onStartTimer = { id, title, minutes ->
                                val encodedTitle = android.net.Uri.encode(title)
                                navController.navigate("timer/$id/$encodedTitle/$minutes")
                            },
                            onEditTask = { task ->
                                taskToEdit = task
                                showInboxModal = true
                            },
                            onRequestReview = { taskId ->
                                taskForReview = taskId
                            }
                        )
                    }

                LaunchedEffect(navigateToTimer) {
                    navigateToTimer?.let { intent ->
                        val id = intent.getStringExtra("taskId") ?: return@let
                        val title = intent.getStringExtra("title") ?: return@let
                        val minutes = intent.getIntExtra("minutes", 25)
                        val encodedTitle = android.net.Uri.encode(title)
                        
                        // Prevent multiple navigations
                        if (navController.currentDestination?.route != "timer/{taskId}/{title}/{minutes}") {
                            navController.navigate("timer/$id/$encodedTitle/$minutes") {
                                popUpTo("home")
                            }
                        }
                        navigateToTimer = null
                    }
                }
            }
            composable("list") {
                TaskListScreen(
                    viewModel = viewModel,
                    snackbarHostState = snackbarHostState,
                    onEditTask = { task ->
                        taskToEdit = task
                        showInboxModal = true
                    },
                    onRequestReview = { taskId ->
                        taskForReview = taskId
                    }
                )
            }
            composable("settings") {
                SettingsScreen(viewModel = viewModel)
            }
            composable(
                route = "timer/{taskId}/{title}/{minutes}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType },
                    navArgument("minutes") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: ""
                val decodedTitle = android.net.Uri.decode(title)
                val minutes = backStackEntry.arguments?.getInt("minutes") ?: 25
                
                TimerScreen(
                    taskId = taskId,
                    taskTitle = decodedTitle,
                    estimatedMinutes = minutes,
                    onMinimize = { navController.popBackStack() },
                    onComplete = {
                        navController.popBackStack()
                        if (viewModel.isHistoryTrackingEnabled.value) {
                            taskForReview = taskId
                        } else {
                            viewModel.completeTask(taskId, feeling = 3, result = "completed")
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("¡Tarea completada!")
                            }
                        }
                    }
                )
            }
            }
        }

        if (showInboxModal) {
            InboxModal(
                viewModel = viewModel,
                taskToEdit = taskToEdit,
                initialCategory = prefilledCategory,
                onDismiss = { 
                    showInboxModal = false
                    taskToEdit = null
                    prefilledCategory = null
                }
            )
        }

        taskForReview?.let { taskId ->
            QuickReviewModal(
                viewModel = viewModel,
                taskId = taskId,
                onDismiss = { taskForReview = null }
            )
        }
    }
}