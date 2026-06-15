package com.example.trackifyv1

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trackifyv1.models.ThemeViewModel
import com.example.trackifyv1.navigation.AppNavHost
import com.example.trackifyv1.ui.theme.DarkAppPalette
import com.example.trackifyv1.ui.theme.LightAppPalette
import com.example.trackifyv1.ui.theme.LocalAppPalette
import com.example.trackifyv1.ui.theme.Trackifyv1Theme
import com.example.trackifyv1.ui.theme.screens.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeVm         = viewModel<ThemeViewModel>()
            val isDark          by themeVm.isDarkMode.collectAsState()
            val onboardingSeen  by themeVm.onboardingSeen.collectAsState()
            val context         = this

            LaunchedEffect(Unit) { themeVm.init(context) }

            val palette = if (isDark) DarkAppPalette else LightAppPalette

            Trackifyv1Theme(darkTheme = isDark) {
                CompositionLocalProvider(LocalAppPalette provides palette) {
                    if (!onboardingSeen) {
                        OnboardingScreen(onFinish = { themeVm.markOnboardingSeen(context) })
                    } else {
                        AppNavHost()
                    }
                }
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
