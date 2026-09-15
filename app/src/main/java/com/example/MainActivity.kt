package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FinanceViewModel
import com.example.ui.navigation.FinanceApp
import com.example.ui.theme.FinanceTrackerTheme

class MainActivity : FragmentActivity() {

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ask for POST_NOTIFICATIONS on Android 13+ (TIRAMISU) using 16-bit requestCode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        setContent {
            val viewModel: FinanceViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isScreenPrivacyEnabled by viewModel.isScreenPrivacyEnabled.collectAsState()
            val lifecycleOwner = LocalLifecycleOwner.current

            // Dynamically apply/clear FLAG_SECURE for screenshot blocking & app switcher protection
            LaunchedEffect(isScreenPrivacyEnabled) {
                if (isScreenPrivacyEnabled) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Monitor background elapsed time for auto-lock
            DisposableEffect(lifecycleOwner) {
                var pauseTime = 0L
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE -> {
                            pauseTime = System.currentTimeMillis()
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            if (pauseTime > 0L) {
                                val elapsed = System.currentTimeMillis() - pauseTime
                                viewModel.onAppResumed(elapsed)
                            }
                        }
                        else -> {}
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            FinanceTrackerTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FinanceApp(viewModel = viewModel)
                }
            }
        }
    }
}


