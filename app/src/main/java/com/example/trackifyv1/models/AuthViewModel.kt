package com.example.trackifyv1.models

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.trackifyv1.navigation.ROUTE_DASHBOARD
import com.example.trackifyv1.navigation.ROUTE_LOGIN
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
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
        const val WEB_CLIENT_ID = "YOUR_WEB_CLIENT_ID_HERE"
    }

    fun register(name: String, email: String, password: String, confirmPassword: String, navController: NavController, context: Context) {
        when {
            name.isBlank()                -> { toast(context, "Enter your name"); return }
            email.isBlank()               -> { toast(context, "Enter your email"); return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> { toast(context, "Enter a valid email address"); return }
            password.isBlank()            -> { toast(context, "Enter a password"); return }
            password.length < 6           -> { toast(context, "Password must be at least 6 characters"); return }
            password != confirmPassword   -> { toast(context, "Passwords do not match"); return }
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
                    else -> "Registration failed. Check your connection."
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
                    else -> "Login failed. Check your connection."
                }
                launch(Dispatchers.Main) { toast(context, msg) }
            } finally { _isLoading.value = false }
        }
    }

    fun signInWithGoogle(navController: NavController, context: Context) {
        if (WEB_CLIENT_ID == "YOUR_WEB_CLIENT_ID_HERE") {
            toast(context, "Google Sign-In not configured yet. See Firebase Console.")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption    = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
                val request  = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
                val result   = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user ?: return@launch
                    val isNew = authResult.additionalUserInfo?.isNewUser == true
                    if (isNew) {
                        usersRef.child(user.uid).setValue(
                            UserModel(id = user.uid, name = user.displayName ?: "", email = user.email ?: "")
                        ).await()
                    }
                    toast(context, "Welcome${if (isNew) "" else " back"}, ${user.displayName?.split(" ")?.firstOrNull() ?: ""}!")
                    navController.navigate(ROUTE_DASHBOARD) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
                } else {
                    toast(context, "Unexpected credential type. Try again.")
                }
            } catch (e: GetCredentialException) {
                toast(context, "Google Sign-In cancelled or unavailable.")
            } catch (e: Exception) {
                toast(context, "Google Sign-In failed: ${e.message ?: "Unknown error"}")
            } finally { _isLoading.value = false }
        }
    }

    private fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
