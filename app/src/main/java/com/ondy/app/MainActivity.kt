package com.ondy.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ondy.app.data.preferences.PreferencesManager
import com.ondy.app.domain.model.ThemeMode
import com.ondy.app.ui.screens.apps.AppsScreen
import com.ondy.app.ui.screens.notifications.NotificationsScreen
import com.ondy.app.ui.screens.schedule.ScheduleScreen
import com.ondy.app.ui.screens.settings.SettingsScreen
import com.ondy.app.ui.screens.welcome.WelcomeScreen
import com.ondy.app.ui.theme.OndyTheme
import com.ondy.app.util.AppCache
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var appCache: AppCache

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The listener still works; the summary is skipped without it. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        warmAppCache()

        val openNotifications =
            intent?.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false) ?: false

        setContent {
            val themeMode by preferencesManager.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.MATERIAL)

            // Material follows the system; the forced modes ignore it.
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.MATERIAL -> isSystemInDarkTheme()
            }

            OndyTheme(
                darkTheme = darkTheme,
                dynamicColor = themeMode == ThemeMode.MATERIAL
            ) {
                val hasSeenWelcome by preferencesManager.hasSeenWelcome
                    .collectAsStateWithLifecycle(initialValue = false)

                if (hasSeenWelcome) {
                    MainScreen(
                        startDestination = if (openNotifications) {
                            ROUTE_NOTIFICATIONS
                        } else {
                            ROUTE_APPS
                        }
                    )
                } else {
                    WelcomeScreen(
                        onComplete = {
                            lifecycleScope.launch {
                                preferencesManager.setHasSeenWelcome(true)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun warmAppCache() {
        lifecycleScope.launch { runCatching { appCache.ensureLoaded() } }
    }

    companion object {
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"

        const val ROUTE_APPS = "apps"
        const val ROUTE_SCHEDULE = "schedule"
        const val ROUTE_NOTIFICATIONS = "notifications"
        const val ROUTE_SETTINGS = "settings"
    }
}

private data class BottomNavDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector
)

@Composable
fun MainScreen(startDestination: String = MainActivity.ROUTE_APPS) {
    val navController = rememberNavController()
    val destinations = listOf(
        BottomNavDestination(
            MainActivity.ROUTE_APPS,
            R.string.select_apps,
            Icons.Default.Apps
        ),
        BottomNavDestination(
            MainActivity.ROUTE_SCHEDULE,
            R.string.schedule,
            Icons.Default.Schedule
        ),
        BottomNavDestination(
            MainActivity.ROUTE_NOTIFICATIONS,
            R.string.notifications,
            Icons.Default.Notifications
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val openSettings: () -> Unit = {
        navController.navigate(MainActivity.ROUTE_SETTINGS) {
            launchSingleTop = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Settings is a detail screen; hiding the tabs keeps it focused.
            if (currentRoute != MainActivity.ROUTE_SETTINGS) {
                NavigationBar(containerColor = Color.Transparent) {
                    destinations.forEach { destination ->
                        val label = stringResource(destination.labelRes)
                        NavigationBarItem(
                            icon = { Icon(destination.icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            // Follow the theme accent in every mode (including
                            // Material You) instead of the neutral container tint.
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainActivity.ROUTE_APPS) {
                AppsScreen(onOpenSettings = openSettings)
            }
            composable(MainActivity.ROUTE_SCHEDULE) {
                ScheduleScreen(onOpenSettings = openSettings)
            }
            composable(MainActivity.ROUTE_NOTIFICATIONS) {
                NotificationsScreen(onOpenSettings = openSettings)
            }
            composable(MainActivity.ROUTE_SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
