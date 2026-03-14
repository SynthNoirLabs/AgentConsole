package com.example.agentconsole.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.agentconsole.AgentConsoleApp
import com.example.agentconsole.ui.history.HistoryScreen

@Composable
fun AgentConsoleNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = "main"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("main") {
            AgentConsoleApp(
                onNavigateToHistory = {
                    navController.navigate("history")
                }
            )
        }
        composable("history") {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
