package com.example.motivation.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

object SecureWindowHelper {
    private const val TAG = "SecureWindowHelper"

    /**
     * Traverses the context hierarchy to find the root Activity.
     */
    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    /**
     * Safely adds the FLAG_SECURE flag to the Activity window.
     */
    fun enableSecure(activity: Activity?) {
        if (activity == null) {
            Log.e(TAG, "Cannot enable secure mode: Activity is null")
            return
        }
        try {
            activity.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Log.d(TAG, "FLAG_SECURE added successfully to window of: ${activity.localClassName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding FLAG_SECURE to window", e)
        }
    }

    /**
     * Safely clears the FLAG_SECURE flag from the Activity window.
     */
    fun disableSecure(activity: Activity?) {
        if (activity == null) {
            Log.e(TAG, "Cannot disable secure mode: Activity is null")
            return
        }
        try {
            activity.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Log.d(TAG, "FLAG_SECURE cleared successfully from window of: ${activity.localClassName}")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing FLAG_SECURE from window", e)
        }
    }
}

/**
 * Composable that secures the current window (prevents screenshots/screen recordings).
 * It automatically applies FLAG_SECURE to the Activity window when active, and clears it on dispose.
 */
@Composable
fun SecureScreen(
    onScreenshotAttempted: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(context, view) {
        val activity = SecureWindowHelper.findActivity(context) ?: SecureWindowHelper.findActivity(view.context)
        SecureWindowHelper.enableSecure(activity)

        var isCallbackRegistered = false
        var callback: Any? = null

        // Android 14+ ScreenCaptureCallback
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val screenCaptureCallback = Activity.ScreenCaptureCallback {
                Log.d("SecureScreen", "Screen capture gesture/keystroke detected natively")
                onScreenshotAttempted()
            }
            callback = screenCaptureCallback
            try {
                activity.registerScreenCaptureCallback(context.mainExecutor, screenCaptureCallback)
                isCallbackRegistered = true
                Log.d("SecureScreen", "ScreenCaptureCallback registered successfully")
            } catch (e: Exception) {
                Log.e("SecureScreen", "Failed to register ScreenCaptureCallback", e)
            }
        }

        // Pre-Android 14 MediaStore ContentObserver
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val observer = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null) {
                    val uriStr = uri.toString()
                    if (uriStr.contains("screenshots", ignoreCase = true) || 
                        uriStr.contains("media", ignoreCase = true)) {
                        Log.d("SecureScreen", "Screenshot file change detected via MediaStore ContentObserver")
                        onScreenshotAttempted()
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                context.contentResolver.registerContentObserver(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    observer
                )
                Log.d("SecureScreen", "ContentObserver registered successfully")
            } catch (e: Exception) {
                Log.e("SecureScreen", "Failed to register ContentObserver", e)
            }
        }

        onDispose {
            val cleanActivity = SecureWindowHelper.findActivity(context) ?: SecureWindowHelper.findActivity(view.context)
            SecureWindowHelper.disableSecure(cleanActivity)

            if (cleanActivity != null && isCallbackRegistered && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && callback != null) {
                try {
                    cleanActivity.unregisterScreenCaptureCallback(callback as Activity.ScreenCaptureCallback)
                    Log.d("SecureScreen", "ScreenCaptureCallback unregistered successfully")
                } catch (e: Exception) {
                    Log.e("SecureScreen", "Failed to unregister ScreenCaptureCallback", e)
                }
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    context.contentResolver.unregisterContentObserver(observer)
                    Log.d("SecureScreen", "ContentObserver unregistered successfully")
                } catch (e: Exception) {
                    Log.e("SecureScreen", "Failed to unregister ContentObserver", e)
                }
            }
        }
    }
}
