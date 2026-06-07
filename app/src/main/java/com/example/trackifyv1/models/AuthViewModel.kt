package com.example.trackifyv1.models

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.trackifyv1.navigation.ROUTE_DASHBOARD
import com.example.trackifyv1.navigation.ROUTE_LOGIN
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth     = FirebaseAuth.getInstance()
    private val usersRef = FirebaseDatabase.getInstance("https://trackify-aab65-default-rtdb.firebaseio.com").getReference("users")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun register(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        navController: NavController,
        context: Context
    ) {
        when {
            name.isBlank()            -> { toast(context, "Please enter your name"); return }
            email.isBlank()           -> { toast(context, "Please enter your email"); return }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
                                      -> { toast(context, "Please enter a valid email address"); return }
            password.isBlank()        -> { toast(context, "Please enter a password"); return }
            password.length < 6       -> { toast(context, "Password must be at least 6 characters"); return }
            password != confirmPassword -> { toast(context, "Passwords do not match"); return }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val uid    = result.user?.uid ?: return@launch

                usersRef.child(uid).setValue(UserModel(id = uid, name = name.trim(), email = email.trim())).await()

                launch(Dispatchers.Main) {
                    toast(context, "Account created! Please log in.")
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_LOGIN) { inclusive = false }
                    }
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthWeakPasswordException      -> "Password is too weak. Use at least 6 characters."
                    is FirebaseAuthUserCollisionException     -> "An account with this email already exists."
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email address. Please check and try again."
                    else -> "Registration failed. Please check your connection and try again."
                }
                launch(Dispatchers.Main) { toast(context, msg) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(
        email: String,
        password: String,
        navController: NavController,
        context: Context
    ) {
        when {
            email.isBlank()    -> { toast(context, "Please enter your email"); return }
            password.isBlank() -> { toast(context, "Please enter your password"); return }
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
                launch(Dispatchers.Main) {
                    toast(context, "Welcome back!")
                    navController.navigate(ROUTE_DASHBOARD) {
                        popUpTo(ROUTE_LOGIN) { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is FirebaseAuthInvalidUserException        -> "No account found with this email."
                    is FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
                    else -> "Login failed. Please check your connection and try again."
                }
                launch(Dispatchers.Main) { toast(context, msg) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
