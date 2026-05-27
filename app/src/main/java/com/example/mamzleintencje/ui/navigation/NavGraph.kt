package com.example.mamzleintencje.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.mamzleintencje.ui.screens.DashboardScreen
import com.example.mamzleintencje.ui.screens.LogListScreen
import com.example.mamzleintencje.ui.screens.SettingsScreen
import com.example.mamzleintencje.ui.viewmodel.MainViewModel

@Composable
fun NavGraph(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(
        navController = navController,
        startDestination = Screen.Logs.route,
        enterTransition = {
            fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + slideInVertically(
                initialOffsetY = { it / 10 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        },
        exitTransition = {
            fadeOut(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        },
        popEnterTransition = {
            fadeIn(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + slideInVertically(
                initialOffsetY = { it / 10 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        },
        popExitTransition = {
            fadeOut(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    ) {
        composable(Screen.Logs.route) {
            LogListScreen(viewModel = viewModel)
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(viewModel = viewModel)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
