package com.bouyahya.kmpcall

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bouyahya.kmpcall.ui.CallScreen
import com.bouyahya.kmpcall.ui.JoinScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = "join_screen"
        ) {
            composable("join_screen") {
                JoinScreen(
                    onJoinCall = { navController.navigate("call_screen") }
                )
            }

            composable("call_screen") {
                CallScreen(
                    onDisconnect = { navController.popBackStack() }
                )
            }
        }
    }
}