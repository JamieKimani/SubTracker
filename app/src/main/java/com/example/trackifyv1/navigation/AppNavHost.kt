package com.example.trackifyv1.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.example.trackifyv1.ui.theme.screens.profile.ProfileScreen
import com.example.trackifyv1.ui.theme.screens.dashboard.DashboardScreen
import com.example.trackifyv1.ui.theme.screens.login.LoginScreen
import com.example.trackifyv1.ui.theme.screens.register.RegisterScreen
import com.example.trackifyv1.ui.theme.screens.splash.SplashScreen
import com.example.trackifyv1.ui.theme.screens.subscriptions.AddSubscriptionScreen
import com.example.trackifyv1.ui.theme.screens.subscriptions.ViewSubscriptionsScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ROUTE_SPLASH
) {
    NavHost(modifier = modifier, navController = navController, startDestination = startDestination) {

        composable(ROUTE_SPLASH) {
            SplashScreen(onLoadingComplete = {
                val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                val dest = if (isLoggedIn) ROUTE_DASHBOARD else ROUTE_LOGIN
                navController.navigate(dest) {
                    popUpTo(ROUTE_SPLASH) { inclusive = true }
                }
            })
        }

        composable(ROUTE_LOGIN) {
            LoginScreen(navController = navController)
        }

        composable(ROUTE_REGISTER) {
            RegisterScreen(navController = navController)
        }

        composable(ROUTE_DASHBOARD) {

            BackHandler(true) {}
            DashboardScreen(navController = navController)
        }

        composable(ROUTE_ADD_SUBSCRIPTION) {
            AddSubscriptionScreen(navController = navController)
        }

        composable(ROUTE_VIEW_SUBSCRIPTIONS) {
            ViewSubscriptionsScreen(navController = navController)
        }

        composable(ROUTE_PROFILE) {
            ProfileScreen(navController = navController)
        }
    }
}
