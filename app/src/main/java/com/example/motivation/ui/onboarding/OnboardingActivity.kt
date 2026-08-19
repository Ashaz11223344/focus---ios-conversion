package com.example.motivation.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.motivation.MainActivity
import com.example.motivation.ui.theme.MotivationTheme

class OnboardingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Seamless transparent edge-to-edge status bars configuration
        enableEdgeToEdge()

        val isRestart = intent.getBooleanExtra("is_restart", false)

        setContent {
            MotivationTheme {
                val viewModel: OnboardingViewModel = viewModel()
                OnboardingScreen(
                    viewModel = viewModel,
                    isRestart = isRestart,
                    onFinished = {
                        if (isRestart) {
                            // After completing restarted guide, navigate back to main quotes screen
                            val mainIntent = Intent(this, MainActivity::class.java).apply {
                                putExtra("start_destination", "quotes")
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            startActivity(mainIntent)
                            finish()
                        } else {
                            // First launch finished, go to MainActivity
                            val mainIntent = Intent(this, MainActivity::class.java)
                            startActivity(mainIntent)
                            finish()
                        }
                    }
                )
            }
        }
    }
}
