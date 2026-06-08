package com.example.trackifyv1.models

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
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
    private val usersRef = FirebaseDatabase.getInstance("https://trackify-aab65-default-rtdb.firebaseio.com").getReference("users")

    private val _profile   = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    private var profileListener: ValueEventListener? = null
    private var profileRef: DatabaseReference? = null

    init { loadProfile() }

    fun loadProfile() {
        val uid = auth.currentUser?.uid ?: run {
            _isLoading.value = false
            return
        }
        _isLoading.value = true
        val ref = usersRef.child(uid)
        val listener = object : ValueEventListener {
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
            override fun onCancelled(error: DatabaseError) {
                _isLoading.value = false

                _profile.value = UserProfile(
                    uid      = uid,
                    name     = auth.currentUser?.displayName ?: "",
                    email    = auth.currentUser?.email ?: "",
                    initials = "?"
                )
            }
        }
        profileListener = listener
        profileRef      = ref
        ref.addValueEventListener(listener)
    }

    fun updateName(newName: String, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        when {
            newName.isBlank()   -> { toast(context, "Name cannot be empty."); return }
            newName.length < 2  -> { toast(context, "Name must be at least 2 characters."); return }
        }
        usersRef.child(uid).child("name").setValue(newName.trim())
            .addOnSuccessListener { toast(context, "Name updated successfully!") }
            .addOnFailureListener { toast(context, "Could not update name. Check your connection.") }
    }

    fun updateEmail(newEmail: String, context: Context) {
        val user = auth.currentUser ?: return
        when {
            newEmail.isBlank() -> { toast(context, "Email cannot be empty."); return }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() -> {
                toast(context, "Please enter a valid email address."); return
            }
        }
        usersRef.child(user.uid).child("email").setValue(newEmail.trim())
            .addOnSuccessListener { toast(context, "Email updated successfully!") }
            .addOnFailureListener { toast(context, "Could not update email. Check your connection.") }
    }

    fun changePassword(currentPassword: String, newPassword: String, context: Context) {
        val user  = auth.currentUser ?: return
        val email = user.email ?: return
        when {
            currentPassword.isBlank()          -> { toast(context, "Please enter your current password."); return }
            newPassword.isBlank()              -> { toast(context, "Please enter a new password."); return }
            newPassword.length < 6             -> { toast(context, "New password must be at least 6 characters."); return }
            newPassword == currentPassword     -> { toast(context, "New password must be different from the current one."); return }
        }
        val credential = EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener { toast(context, "Password changed successfully!") }
                    .addOnFailureListener { toast(context, "Could not change password. Please try again.") }
            }
            .addOnFailureListener { e ->
                val msg = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Current password is incorrect."
                    else -> "Authentication failed. Please try again."
                }
                toast(context, msg)
            }
    }

    fun logout() { auth.signOut() }

    override fun onCleared() {
        super.onCleared()

        profileListener?.let { profileRef?.removeEventListener(it) }
    }

    private fun toast(context: Context, msg: String) =
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}
