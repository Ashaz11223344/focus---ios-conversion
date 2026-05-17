package com.example.motivation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.motivation.ui.*
import com.example.motivation.ui.theme.MotivationTheme
import com.example.motivation.viewmodel.PersonalizationViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("Permission", "POST_NOTIFICATIONS permission granted.")
            } else {
                Log.d("Permission", "POST_NOTIFICATIONS permission denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        askNotificationPermission()

        setContent {
            MotivationTheme {
                val navController = rememberNavController()
                val personalizationViewModel: PersonalizationViewModel = viewModel()
                val userName by personalizationViewModel.userName.collectAsState()

                val startDestinationFromIntent = intent.getStringExtra("start_destination")
                LaunchedEffect(startDestinationFromIntent) {
                    if (startDestinationFromIntent != null) {
                        navController.navigate(startDestinationFromIntent)
                    }
                }

                LaunchedEffect(userName) {
                    if (userName?.isNotBlank() == true && navController.currentBackStackEntry?.destination?.route == "name_input") {
                         navController.navigate("quotes") { popUpTo(0) }
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isHomeScreen = currentRoute == "quotes" || currentRoute == "name_input" || currentRoute == "splash"

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (userName?.isNotBlank() == true && !isHomeScreen) {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = getScreenTitle(currentRoute),
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontStyle = FontStyle.Italic
                                        ),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                navigationIcon = {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.background,
                                    titleContentColor = MaterialTheme.colorScheme.secondary,
                                    navigationIconContentColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    ) {
                        composable("splash") { SplashScreen(navController, userName ?: "") }
                        composable("quotes") { QuoteScreen(navController, userName ?: "") }
                        composable("search") { SearchScreen() }
                        composable("favorites") { FavoritesScreen() }
                        composable("history") { HistoryScreen() }
                        composable("journal") { JournalScreen() }
                        composable("name_input") { NameInputScreen(viewModel = personalizationViewModel, onNameSaved = { navController.navigate("quotes") { popUpTo(0) } }) }
                        composable("streak") { StreakScreen(onNavigateHome = { navController.navigate("quotes") { popUpTo("quotes") { inclusive = true } } }) }
                        composable("achievements") { AchievementsScreen(onNavigateHome = { navController.navigate("quotes") { popUpTo("quotes") { inclusive = true } } }) }
                        composable("settings") { SettingsScreen() }
                        composable("mood") { MoodScreen() }
                    }
                }
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {}
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {}
                else -> { requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
        }
    }
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}

fun getScreenTitle(route: String?): String {
    return when (route) {
        "search" -> "Search"
        "favorites" -> "Favorites"
        "history" -> "History"
        "journal" -> "Journal"
        "streak" -> "Streak"
        "achievements" -> "Achievements"
        "settings" -> "Settings"
        "mood" -> "Mood"
        else -> ""
    }
}
