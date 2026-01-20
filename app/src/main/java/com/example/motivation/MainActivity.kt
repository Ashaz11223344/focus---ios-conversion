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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.motivation.ui.* 
import com.example.motivation.ui.theme.MotivationTheme
import com.example.motivation.viewmodel.PersonalizationViewModel

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
                    if (userName == "") {
                        navController.navigate("name_input") { popUpTo(0) }
                    } else if (navController.currentBackStackEntry?.destination?.route == "name_input") {
                         navController.navigate("quotes") { popUpTo(0) }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        if (userName?.isNotBlank() == true) {
                            FocusTopAppBar(navController = navController)
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "quotes",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("quotes") { QuoteScreen() }
                        composable("name_input") { NameInputScreen(viewModel = personalizationViewModel, onNameSaved = { navController.navigate("quotes") { popUpTo(0) } }) }
                        composable("streak") { StreakScreen(onNavigateHome = { navController.navigate("quotes") { popUpTo("quotes") { inclusive = true } } }) }
                        composable("achievements") { AchievementsScreen(onNavigateHome = { navController.navigate("quotes") { popUpTo("quotes") { inclusive = true } } }) }
                        composable("settings") { SettingsScreen() }
                        composable("name_affirmations") { NameAffirmationsScreen(personalizationViewModel, onNavigateHome = { navController.navigate("quotes") { popUpTo(0) } }) }
                        composable("mirror_mode") { MirrorModeScreen(personalizationViewModel, onNavigateHome = { navController.navigate("quotes") { popUpTo(0) } }) }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusTopAppBar(navController: NavController) {
    TopAppBar(title = { Text(stringResource(id = R.string.app_name)) },
        actions = {
            IconButton(onClick = { navController.navigate("name_affirmations") }) {
                Icon(painterResource(id = R.drawable.ic_affirmations), contentDescription = "Your Affirmations")
            }
            IconButton(onClick = { navController.navigate("mirror_mode") }) {
                Icon(painterResource(id = R.drawable.ic_mirror_mode), contentDescription = "Mirror Mode")
            }
            IconButton(onClick = { navController.navigate("streak") }) {
                Icon(painterResource(id = R.drawable.ic_streak), contentDescription = "Streak")
            }
            IconButton(onClick = { navController.navigate("achievements") }) {
                Icon(painterResource(id = R.drawable.ic_achievements), contentDescription = "Achievements")
            }
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(painterResource(id = R.drawable.ic_settings), contentDescription = "Settings")
            }
        }
    )
}
