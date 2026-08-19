package com.example.motivation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController

@Composable
fun SplashScreen(navController: NavController, userName: String?) {
    LaunchedEffect(userName) {
        if (userName == null) return@LaunchedEffect
        
        if (userName.isNotBlank()) {
            navController.navigate("quotes") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("name_input") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D10))
    )
}
