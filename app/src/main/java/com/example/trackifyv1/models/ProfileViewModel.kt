package com.example.trackifyv1.models

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val initials: String = ""
)

class ProfileViewModel : ViewModel() {

    private val auth     = FirebaseAuth.getInstance()
    private val usersRef = FirebaseDatabase.getInstance().getReference("users")

    private val _profile   = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: return
        _isLoading.value = true

        usersRef.child(uid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name  = snapshot.child("name").getValue(String::class.java)
                    ?: auth.currentUser?.displayName ?: ""
                val email = snapshot.child("email").getValue(String::class.java)
                    ?: auth.currentUser?.email ?: ""
                val initials = name.trim().split(" ")
                    .filter { it.isNotBlank() }.take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
                    .ifBlank { "?" }
                _profile.value = UserProfile(uid, name, email, initials)
                _isLoading.value = false
            }
            override fun onCancelled(error: DatabaseError) { _isLoading.value = false }
        })
    }

    fun updateName(newName: String, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        if (newName.isBlank()) { Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show(); return }
        usersRef.child(uid).child("name").setValue(newName)
            .addOnSuccessListener { Toast.makeText(context, "Name updated!", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
    }

    fun updateEmail(newEmail: String, context: Context) {
        val user = auth.currentUser ?: return
        if (newEmail.isBlank()) { Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show(); return }
        user.updateEmail(newEmail)
            .addOnSuccessListener {
                usersRef.child(user.uid).child("email").setValue(newEmail)
                Toast.makeText(context, "Email updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { Toast.makeText(context, "Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
    }

    fun changePassword(currentPassword: String, newPassword: String, context: Context) {
        val user  = auth.currentUser ?: return
        val email = user.email ?: return
        if (newPassword.length < 6) { Toast.makeText(context, "Password must be ≥ 6 characters", Toast.LENGTH_SHORT).show(); return }
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { Toast.makeText(context, "Password changed!", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { e -> Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
            .addOnFailureListener { Toast.makeText(context, "Current password incorrect", Toast.LENGTH_SHORT).show() }
    }

    fun logout() { auth.signOut() }
}