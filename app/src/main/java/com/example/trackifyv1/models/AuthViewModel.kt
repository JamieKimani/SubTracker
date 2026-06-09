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

    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        client.signOut()
        return client.signInIntent
    }

    fun handleGoogleSignInResult(intent: Intent?, navController: NavController, context: Context) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val task    = GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken ?: throw Exception("No ID token received from Google.")
                val credential  = GoogleAuthProvider.getCredential(idToken, null)
                val authResult  = auth.signInWithCredential(credential).await()
                val user        = authResult.user ?: throw Exception("Firebase user is null after sign-in.")
                val isNew       = authResult.additionalUserInfo?.isNewUser == true
                if (isNew) {
                    usersRef.child(user.uid).setValue(
                        UserModel(id = user.uid, name = user.displayName ?: "", email = user.email ?: "")
                    ).await()
                }
                val firstName = user.displayName?.split(" ")?.firstOrNull() ?: ""
                launch(Dispatchers.Main) {
                    toast(context, "Welcome${if (isNew) "" else " back"}${if (firstName.isNotBlank()) ", $firstName" else ""}!")
                    navController.navigate(ROUTE_DASHBOARD) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
                }
            } catch (e: ApiException) {
                val msg = when (e.statusCode) {
                    12501 -> "Google Sign-In was cancelled."
                    12502 -> "Google Sign-In is currently in progress."
                    10    -> "Developer error: check SHA-1 fingerprint in Firebase Console."
                    else  -> "Google Sign-In failed (code ${e.statusCode}). Try again."
                }
                launch(Dispatchers.Main) { toast(context, msg) }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { toast(context, "Sign-in failed: ${e.message ?: "Unknown error"}") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String, navController: NavController, context: Context) {
        when {
            name.isBlank()                           -> { toast(context, "Enter your name"); return }
            email.isBlank()                          -> { toast(context, "Enter your email"); return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { toast(context, "Enter a valid email address"); return }
            password.isBlank()                       -> { toast(context, "Enter a password"); return }
            password.length < 6                      -> { toast(context, "Password must be at least 6 characters"); return }
            password != confirmPassword              -> { toast(context, "Passwords do not match"); return }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid    = result.user?.uid ?: return@launch
                usersRef.child(uid).setValue(UserModel(id = uid, name = name.trim(), email = email.trim())).await()
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

    private fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
