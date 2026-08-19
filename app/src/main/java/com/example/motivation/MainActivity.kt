package com.example.motivation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
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
import androidx.compose.runtime.remember
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
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private var onBiometricAuthSuccess: (() -> Unit)? = null
    private var onBiometricAuthError: ((String) -> Unit)? = null

    fun triggerBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        onBiometricAuthSuccess = onSuccess
        onBiometricAuthError = onError
        
        runOnUiThread {
            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (e: Exception) {
                onError("Failed to initiate biometric: ${e.message}")
            }
        }
    }

    private fun setupBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onBiometricAuthSuccess?.invoke()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED) {
                    onBiometricAuthError?.invoke(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onBiometricAuthError?.invoke("Authentication failed. Please try again.")
            }
        })

        // Use the strongest available biometric authenticator
        val strength = com.example.motivation.security.BiometricCapability.getAvailableStrength(this)
        val authenticator = when (strength) {
            com.example.motivation.security.BiometricCapability.BiometricStrength.STRONG ->
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
            com.example.motivation.security.BiometricCapability.BiometricStrength.WEAK ->
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
            else ->
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
        }

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Private Journal")
            .setSubtitle("Use your biometric credentials to decrypt your logs")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(authenticator)
            .build()
    }

    private val startDestination = MutableStateFlow<String?>(null)

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
        val sharedPrefs = getSharedPreferences("motivation_prefs", android.content.Context.MODE_PRIVATE)
        
        // Get the current version code of the app
        val currentVersionCode = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            20L // Fallback to current build versionCode
        }
        
        val lastRunVersion = sharedPrefs.getLong("last_run_version_code", -1L)
        if (lastRunVersion != currentVersionCode) {
            sharedPrefs.edit()
                .putBoolean("onboarding_completed", false)
                .putLong("last_run_version_code", currentVersionCode)
                .apply()
        }

        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)
        if (!onboardingCompleted) {
            val intent = Intent(this, com.example.motivation.ui.onboarding.OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        setupBiometricPrompt()

        enableEdgeToEdge()
        askNotificationPermission()

        // Aggressive purge on EVERY app launch (moved to IO to avoid blocking main thread):
        // Cancels ALL zombie workers, stale alarms (request codes 1-20),
        // and legacy MotivationNotificationWorker instances, then reschedules fresh.
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("MainActivity", "[STARTUP] Running aggressive notification purge...")
            com.example.motivation.receiver.QuoteNotificationScheduler.aggressivePurgeOnStartup(this@MainActivity)

            // Ensure the Mood Reminder is scheduled correctly if enabled
            com.example.motivation.receiver.MoodReminderScheduler.schedule(this@MainActivity)
        }

        // Synchronize DND alarms and AppBlockerService on startup
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = com.example.motivation.data.local.AppDatabase.getDatabase(this@MainActivity)
                val dao = db.focusGuardDao()
                
                // Reschedule DND alarms and immediately apply active DND state
                val dndSchedules = dao.getAllDndSchedules().first()
                val dndManager = com.example.motivation.focusguard.DndScheduleManager(this@MainActivity)
                dndManager.scheduleAll(dndSchedules)
                dndManager.checkAndApplyCurrentDndState(dndSchedules)
                
                // Start AppBlockerService if there are enabled block rules
                val appRules = dao.getEnabledAppBlockRules()
                if (appRules.isNotEmpty()) {
                    com.example.motivation.focusguard.AppBlockerService.start(this@MainActivity)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to sync Focus Guard on startup", e)
            }
        }

        val quoteText = intent.getStringExtra("quote_text")
        val quoteCategory = intent.getStringExtra("quote_category")
        if (quoteText != null && quoteCategory != null) {
            val mainViewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.motivation.viewmodel.MainViewModel::class.java]
            mainViewModel.setQuote(quoteText, quoteCategory)
            intent.removeExtra("quote_text")
            intent.removeExtra("quote_category")
        }

        startDestination.value = intent.getStringExtra("start_destination")

        setContent {
            val settingsDataStore = remember { com.example.motivation.data.SettingsDataStore(applicationContext) }
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = com.example.motivation.ui.theme.ThemeMode.DARK)

            MotivationTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val personalizationViewModel: PersonalizationViewModel = viewModel()
                val userName by personalizationViewModel.userName.collectAsState()

                val destination by startDestination.collectAsState()
                val destinationVal = destination
                LaunchedEffect(destinationVal) {
                    if (destinationVal != null) {
                        navController.navigate(destinationVal)
                        startDestination.value = null // clear so we don't navigate again on recomposition
                    }
                }

                LaunchedEffect(userName) {
                    if (userName?.isNotBlank() == true && navController.currentBackStackEntry?.destination?.route == "name_input") {
                         navController.navigate("quotes") { popUpTo(0) }
                    }
                }

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isHomeScreen = currentRoute == "quotes" || currentRoute == "name_input" || currentRoute == "splash" || currentRoute?.startsWith("weekly_report") == true || currentRoute?.startsWith("wallpaper_generator") == true || currentRoute == "history" || currentRoute == "search" || currentRoute == "favorites" || currentRoute == "streak" || currentRoute == "achievements" || currentRoute?.startsWith("focus_guard") == true || currentRoute == "settings"

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
                        composable("splash") { SplashScreen(navController, userName) }
                        composable("quotes") { QuoteScreen(navController, userName ?: "") }
                        composable("search") { SearchScreen(navController = navController) }
                        composable("favorites") { FavoritesScreen(navController = navController) }
                        composable("history") { HistoryScreen(navController = navController) }
                        composable("journal") { JournalScreen() }
                        composable("name_input") { NameInputScreen(onNameSaved = { navController.navigate("quotes") { popUpTo(0) } }) }
                        composable("streak") { StreakScreen(onNavigateHome = { navController.navigate("quotes") { popUpTo("quotes") { inclusive = true } } }) }
                        composable("achievements") { AchievementsScreen(onNavigateHome = { navController.navigate("quotes") { popUpTo("quotes") { inclusive = true } } }) }
                        composable("settings") { SettingsScreen(navController = navController) }
                        composable("my_profile") { MyProfileScreen(onNavigateBack = { navController.popBackStack() }) }
                        composable(
                            route = "focus_guard/{initialTab}",
                            arguments = listOf(androidx.navigation.navArgument("initialTab") { type = androidx.navigation.NavType.IntType })
                        ) { backStackEntry ->
                            val initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0
                            com.example.motivation.ui.focusguard.FocusGuardScreen(navController = navController, initialTab = initialTab)
                        }
                        composable("mood") { MoodScreen(navController = navController) }
                        composable(
                            route = "weekly_report/{isPartial}",
                            arguments = listOf(androidx.navigation.navArgument("isPartial") { type = androidx.navigation.NavType.BoolType })
                        ) { backStackEntry ->
                            val isPartial = backStackEntry.arguments?.getBoolean("isPartial") ?: false
                            ReportCardScreen(navController = navController, isPartialWeekParam = isPartial)
                        }
                        composable(
                            route = "wallpaper_generator?quoteText={quoteText}&author={author}",
                            arguments = listOf(
                                androidx.navigation.navArgument("quoteText") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = ""
                                },
                                androidx.navigation.navArgument("author") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = "Focus"
                                }
                            )
                        ) { backStackEntry ->
                            val rawQuoteText = backStackEntry.arguments?.getString("quoteText") ?: ""
                            val rawAuthor = backStackEntry.arguments?.getString("author") ?: "Focus"
                            val decodedQuote = try {
                                java.net.URLDecoder.decode(rawQuoteText, "UTF-8")
                            } catch (e: Exception) {
                                rawQuoteText
                            }
                            val decodedAuthor = try {
                                java.net.URLDecoder.decode(rawAuthor, "UTF-8")
                            } catch (e: Exception) {
                                rawAuthor
                            }
                            WallpaperGeneratorScreen(navController, decodedQuote, decodedAuthor)
                        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startDestination.value = intent.getStringExtra("start_destination")

        val quoteText = intent.getStringExtra("quote_text")
        val quoteCategory = intent.getStringExtra("quote_category")
        if (quoteText != null && quoteCategory != null) {
            val mainViewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.motivation.viewmodel.MainViewModel::class.java]
            mainViewModel.setQuote(quoteText, quoteCategory)
            intent.removeExtra("quote_text")
            intent.removeExtra("quote_category")
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
        "my_profile" -> "My Profile"
        else -> {
            if (route?.startsWith("focus_guard") == true) "Focus Guard" else ""
        }
    }
}
