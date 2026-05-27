package com.example.mamzleintencje.ui.navigation

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
        startDestination = Screen.Dashboard.route
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