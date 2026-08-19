package com.example.motivation.focusguard

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.motivation.ui.focusguard.BlockOverlayScreen
import com.example.motivation.ui.theme.MotivationTheme

class BlockOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val blockedAppName = intent.getStringExtra("blocked_app_name") ?: "This app"
        val blockedPkg = intent.getStringExtra("blocked_pkg") ?: ""
        val endHour = intent.getIntExtra("end_hour", 0)
        val endMinute = intent.getIntExtra("end_minute", 0)
        val untilTime = String.format("%02d:%02d", endHour, endMinute)

        setContent {
            MotivationTheme {
                BlockOverlayScreen(
                    appName = blockedAppName,
                    packageName = blockedPkg,
                    untilTime = untilTime,
                    onGoBack = {
                        // Navigate to Focus home screen instead of back to blocked app
                        val homeIntent = packageManager.getLaunchIntentForPackage(packageName)
                        startActivity(homeIntent)
                        finish()
                    },
                    onOverrideActive = {
                        // When override is active, register in override manager and finish
                        AppBlockOverrideManager.setOverride(blockedPkg, 5 * 60 * 1000L)
                        finish()
                    }
                )
            }
        }
    }

    override fun onBackPressed() {
        // Intercept back press — go to Focus home, NOT the blocked app
        val homeIntent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(homeIntent)
        finish()
    }
}
