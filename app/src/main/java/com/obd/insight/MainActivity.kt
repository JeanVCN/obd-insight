package com.obd.insight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.obd.insight.ui.connection.ConnectionScreen
import com.obd.insight.ui.dashboard.DashboardScreen
import com.obd.insight.ui.history.TripHistoryScreen
import com.obd.insight.ui.terminal.AtTerminalScreen
import com.obd.insight.ui.theme.ObdInsightTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ObdInsightTheme {
                ObdInsightNavHost()
            }
        }
    }
}

@Composable
private fun ObdInsightNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "connection") {
        composable("connection") {
            ConnectionScreen(
                onNavigateToAtTerminal = { navController.navigate("at_terminal") },
                onNavigateToDashboard = { navController.navigate("dashboard") },
                onNavigateToTripHistory = { navController.navigate("trip_history") }
            )
        }
        composable("at_terminal") {
            AtTerminalScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("trip_history") {
            TripHistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
