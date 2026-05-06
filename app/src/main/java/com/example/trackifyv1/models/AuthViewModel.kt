package com.example.trackifyv1.models

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.trackifyv1.navigation.ROUTE_DASHBOARD
import com.example.trackifyv1.navigation.ROUTE_LOGIN
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    private val _isLoggedIn = MutableStateFlow(auth.currentUser != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun register(
        name: String,
        email: String,
        password: String,
        confirmpassword: String,
        navController: NavController,
        context: Context
    ) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmpassword.isBlank()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmpassword) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid ?: return@launch

                val user = UserModel(
                    id    = uid,
                    name  = name,
                    email = email
                )
                usersRef.child(uid).setValue(user).await()

                _isLoggedIn.value = true
                _message.value = "Registration successful"
                
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                    navController.navigate(ROUTE_LOGIN)
                }
            } catch (e: Exception) {
                _message.value = "Registration failed: ${e.message}"
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _isLoggedIn.value = true
                _message.value = "Login successful"
                
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                    navController.navigate(ROUTE_DASHBOARD)
                }
            } catch (e: Exception) {
                _message.value = "Login failed: ${e.message}"
                launch(Dispatchers.Main) {
                    Toast.makeText(context, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }



}
