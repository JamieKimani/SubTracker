package com.example.trackifyv1.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.ui.theme.screens.dashboard.DashboardScreen
import com.example.trackifyv1.ui.theme.screens.login.LoginScreen
import com.example.trackifyv1.ui.theme.screens.profile.ProfileScreen
import com.example.trackifyv1.ui.theme.screens.register.RegisterScreen
import com.example.trackifyv1.ui.theme.screens.splash.SplashScreen
import com.example.trackifyv1.ui.theme.screens.subscriptions.AddSubscriptionScreen
import com.example.trackifyv1.ui.theme.screens.subscriptions.ViewSubscriptionsScreen
import com.google.firebase.auth.FirebaseAuth

private const val ANIM_MS = 320

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = ROUTE_SPLASH
) {
    NavHost(
        modifier        = modifier,
        navController   = navController,
        startDestination = startDestination,
        enterTransition  = { fadeIn(tween(ANIM_MS)) },
        exitTransition   = { fadeOut(tween(ANIM_MS / 2)) },
        popEnterTransition  = {
            slideInHorizontally(tween(ANIM_MS)) { -it / 4 } + fadeIn(tween(ANIM_MS))
        },
        popExitTransition   = {
            slideOutHorizontally(tween(ANIM_MS)) { it / 2 } + fadeOut(tween(ANIM_MS / 2))
        }
    ) {
        composable(ROUTE_SPLASH) {
            SplashScreen(onLoadingComplete = {
                val dest = if (FirebaseAuth.getInstance().currentUser != null) ROUTE_DASHBOARD else ROUTE_LOGIN
                navController.navigate(dest) { popUpTo(ROUTE_SPLASH) { inclusive = true } }
            })
        }

        composable(
            route           = ROUTE_LOGIN,
            enterTransition = { slideInHorizontally(tween(ANIM_MS)) { it } + fadeIn(tween(ANIM_MS)) },
            exitTransition  = { slideOutHorizontally(tween(ANIM_MS)) { -it / 4 } + fadeOut(tween(ANIM_MS / 2)) }
        ) { LoginScreen(navController) }

        composable(
            route           = ROUTE_REGISTER,
            enterTransition = { slideInHorizontally(tween(ANIM_MS)) { it } + fadeIn(tween(ANIM_MS)) },
            exitTransition  = { slideOutHorizontally(tween(ANIM_MS)) { -it / 4 } + fadeOut(tween(ANIM_MS / 2)) }
        ) { RegisterScreen(navController) }

        composable(ROUTE_DASHBOARD) {
            BackHandler(true) {}
            DashboardScreen(navController)
        }

        composable(
            route           = ROUTE_ADD_SUBSCRIPTION,
            enterTransition = {
                slideInHorizontally(tween(ANIM_MS)) { it } + fadeIn(tween(ANIM_MS))
            },
            popExitTransition = {
                slideOutHorizontally(tween(ANIM_MS)) { it } + fadeOut(tween(ANIM_MS / 2))
            }
        ) { AddSubscriptionScreen(navController) }

        composable(ROUTE_VIEW_SUBSCRIPTIONS) { ViewSubscriptionsScreen(navController) }
        composable(ROUTE_PROFILE)            { ProfileScreen(navController) }
        composable(
    }
}
