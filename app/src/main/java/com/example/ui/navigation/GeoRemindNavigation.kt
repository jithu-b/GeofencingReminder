package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddReminderScreen
import com.example.ui.screens.CompletedScreen
import com.example.ui.screens.ConfirmationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MapOverviewScreen
import com.example.ui.screens.ReminderDetailScreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.ReminderViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Map : Screen("map")
    data object Completed : Screen("completed")
    data object AddReminder : Screen("add_reminder")
    data object Confirmation : Screen("confirmation")
    data object Detail : Screen("detail/{reminderId}") {
        fun createRoute(reminderId: Long) = "detail/$reminderId"
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        label = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_home"
    ),
    BottomNavItem(
        route = Screen.Map.route,
        label = "Map",
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map,
        testTag = "nav_map"
    ),
    BottomNavItem(
        route = Screen.Completed.route,
        label = "Completed",
        selectedIcon = Icons.Filled.CheckCircle,
        unselectedIcon = Icons.Outlined.CheckCircle,
        testTag = "nav_completed"
    )
)

@Composable
fun GeoRemindNavigation(
    viewModel: ReminderViewModel = viewModel(),
    initialReminderId: Long? = null
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in listOf(
        Screen.Home.route,
        Screen.Map.route,
        Screen.Completed.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar(
                    modifier = Modifier
                        .testTag("bottom_navigation_bar")
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                android.util.Log.d("NavDebug", "Tapped: ${item.route}, current: $currentRoute")
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                indicatorColor = BrandPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (initialReminderId != null && initialReminderId > 0) {
                Screen.Detail.createRoute(initialReminderId)
            } else {
                Screen.Home.route
            },
            enterTransition = { fadeIn(animationSpec = tween(250)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
            popExitTransition = { fadeOut(animationSpec = tween(200)) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = { navController.navigate(Screen.AddReminder.route) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                    onNavigateToMap = { navController.navigate(Screen.Map.route) }
                )
            }

            composable(Screen.Map.route) {
                MapOverviewScreen(
                    viewModel = viewModel,
                    onNavigateToAdd = { navController.navigate(Screen.AddReminder.route) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }

            composable(Screen.Completed.route) {
                CompletedScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
                )
            }

            composable(Screen.AddReminder.route) {
                AddReminderScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onReminderCreated = {
                        navController.navigate(Screen.Confirmation.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }

            composable(Screen.Confirmation.route) {
                val lastReminder by viewModel.lastCreatedReminder.collectAsStateWithLifecycle()
                ConfirmationScreen(
                    reminder = lastReminder,
                    onViewOnMap = {
                        navController.navigate(Screen.Map.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBackToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("reminderId") { type = NavType.LongType })
            ) { backStackEntry ->
                val reminderId = backStackEntry.arguments?.getLong("reminderId") ?: 0L
                ReminderDetailScreen(
                    reminderId = reminderId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
