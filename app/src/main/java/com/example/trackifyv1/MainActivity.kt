package com.example.trackifyv1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.trackifyv1.navigation.AppNavHost
import com.example.trackifyv1.ui.theme.Trackifyv1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Trackifyv1Theme {
                AppNavHost()
            }
        }
    }
}
