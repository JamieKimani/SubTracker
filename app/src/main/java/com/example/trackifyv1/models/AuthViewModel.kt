package com.example.trackifyv1.models

import android.content.Context
import android.content.Intent
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.trackifyv1.navigation.ROUTE_DASHBOARD
import com.example.trackifyv1.navigation.ROUTE_LOGIN
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth     = FirebaseAuth.getInstance()
    private val usersRef = FirebaseDatabase
        .getInstance("https://trackify-aab65-default-rtdb.firebaseio.com")
        .getReference("users")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    companion object {
        const val WEB_CLIENT_ID = "1012390110692-5fffhm18to5qfbp8cd4di964v1rhaa2f.apps.googleusercontent.com"
    }

    private fun buildGoogleSignInClient(context: Context) =
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build()
        )

    fun getGoogleSignInIntent(context: Context): Intent =
        buildGoogleSignInClient(context).signInIntent

    fun handleGoogleSignInResult(intent: Intent?, navController: NavController, context: Context) {
        if (intent == null) {
            toast(context, "Google Sign-In was cancelled.")
            return
        }
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val task    = GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                    ?: throw Exception("No ID token from Google. Check SHA-1 in Firebase Console.")

                val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
                val authResult   = auth.signInWithCredential(firebaseCred).await()
                val user         = authResult.user
                    ?: throw Exception("Firebase returned null user after Google sign-in.")

                if (authResult.additionalUserInfo?.isNewUser == true) {
                    usersRef.child(user.uid).setValue(
                        UserModel(
                            id    = user.uid,
                            name  = user.displayName ?: "",
                            email = user.email ?: ""
                        )
                    ).await()
                }

                val firstName = user.displayName?.split(" ")?.firstOrNull() ?: ""
                val isNew     = authResult.additionalUserInfo?.isNewUser == true
                launch(Dispatchers.Main) {
                    toast(context, "Welcome${if (!isNew) " back" else ""}${if (firstName.isNotBlank()) ", $firstName!" else "!"}")
                    navController.navigate(ROUTE_DASHBOARD) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                }
            } catch (e: ApiException) {
                val msg = when (e.statusCode) {
                    12501 -> "Sign-In cancelled."
                    12502 -> "Sign-In already in progress. Please wait."
                    10    -> "Configuration error: SHA-1 fingerprint not registered in Firebase Console."
                    7     -> "No internet connection. Check your network."
                    else  -> "Google Sign-In failed (code ${e.statusCode})."
                }
                launch(Dispatchers.Main) { toast(context, msg) }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    toast(context, "Sign-In failed: ${e.message ?: "Unknown error"}")
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signOutGoogle(context: Context) {
        buildGoogleSignInClient(context).signOut()
    }

    fun register(
        name: String, email: String, password: String,
        confirmPassword: String, navController: NavController, context: Context
    ) {
        when {
            name.isBlank()              -> { toast(context, "Enter your name"); return }
            email.isBlank()             -> { toast(context, "Enter your email"); return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { toast(context, "Enter a valid email"); return }
            password.isBlank()          -> { toast(context, "Enter a password"); return }
            password.length < 6         -> { toast(context, "Password must be at least 6 characters"); return }
            password != confirmPassword -> { toast(context, "Passwords do not match"); return }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid    = result.user?.uid ?: return@launch
                usersRef.child(uid).setValue(
                    UserModel(id = uid, name = name.trim(), email = email.trim())
                ).await()
                launch(Dispatchers.Main) {
                    toast(context, "Account created! Please log in.")
                    navController.navigate(ROUTE_LOGIN) { popUpTo(ROUTE_LOGIN) { inclusive = false } }
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthWeakPasswordException       -> "Password is too weak."
                    is FirebaseAuthUserCollisionException      -> "An account with this email already exists."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email address."
                    else                                       -> "Registration failed. Check your connection."
                }
                launch(Dispatchers.Main) { toast(context, msg) }
            } finally { _isLoading.value = false }
        }
    }

    fun login(email: String, password: String, navController: NavController, context: Context) {
        when {
            email.isBlank()    -> { toast(context, "Enter your email"); return }
            password.isBlank() -> { toast(context, "Enter your password"); return }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                launch(Dispatchers.Main) {
                    toast(context, "Welcome back!")
                    navController.navigate(ROUTE_DASHBOARD) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthInvalidUserException        -> "No account found with this email."
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect password."
                    else                                       -> "Login failed. Check your connection."
                }
                launch(Dispatchers.Main) { toast(context, msg) }
            } finally { _isLoading.value = false }
        }
    }

    private fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
