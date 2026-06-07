package com.example.trackifyv1.ui.theme.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.models.ProfileViewModel
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.navigation.ROUTE_LOGIN

private val Gold       = Color(0xFFD4A017)
private val Crimson    = Color(0xFF8B0000)
private val DarkPurple = Color(0xFF1A0533)
private val CardBg     = Color(0xFF1C1C1C)
private val Muted      = Color(0xFF9E9E9E)
private val BorderIdle = Color(0xFF4A3F6B)

@Composable
fun ProfileScreen(navController: NavController) {
    val context       = LocalContext.current
    val profileVm: ProfileViewModel       = viewModel()
    val subscriptionVm: SubscriptionViewModel = viewModel()

    val profile       by profileVm.profile.collectAsState()
    val isLoading     by profileVm.isLoading.collectAsState()
    val subscriptions by subscriptionVm.subscriptions.collectAsState()

    var showEditName       by remember { mutableStateOf(false) }
    var showEditEmail      by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showLogoutConfirm  by remember { mutableStateOf(false) }

    val monthlySpend = subscriptions.sumOf { it.subscriptionAmount.toDoubleOrNull() ?: 0.0 }
    val yearlySpend  = monthlySpend * 12

    val fColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = Gold,
        unfocusedBorderColor    = BorderIdle,
        focusedTextColor        = Color.White,
        unfocusedTextColor      = Color.White,
        focusedContainerColor   = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        cursorColor             = Gold,
        focusedLabelColor       = Gold,
        unfocusedLabelColor     = Muted
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .statusBarsPadding()
            .padding(top = 20.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile", color = Gold, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Gold, modifier = Modifier.size(48.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Gold, Crimson)))
                        .border(2.dp, BorderIdle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(profile.initials, color = Color.White, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Text(profile.name.ifBlank { "—" }, color = Color.White, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(profile.email.ifBlank { "—" }, color = Muted, fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace)
            }
        }

        // Stats row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "KES %.0f".format(monthlySpend), "Monthly")
            StatCard(Modifier.weight(1f), "KES %.0f".format(yearlySpend), "Yearly")
        }

        // Settings section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Text("Account Settings", color = Gold, fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))

            HorizontalDivider(color = BorderIdle.copy(alpha = 0.3f), thickness = 0.5.dp)
            SettingRow(Icons.Default.Person, "Display Name", profile.name.ifBlank { "Not set" }) { showEditName = true }
            HorizontalDivider(color = BorderIdle.copy(alpha = 0.3f), thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(Icons.Default.Email, "Email Address", profile.email.ifBlank { "Not set" }) { showEditEmail = true }
            HorizontalDivider(color = BorderIdle.copy(alpha = 0.3f), thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(Icons.Default.Lock, "Change Password", "••••••••") { showChangePassword = true }
        }

        Button(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Crimson)
        ) {
            Text("Log Out", color = Gold, fontSize = 15.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Text("© 2026 Trackify", fontSize = 11.sp, color = BorderIdle,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth().wrapContentWidth())
    }

    // Edit Name
    // Snapshot the name once when the dialog opens (stable key prevents re-init on recompose)
    if (showEditName) {
        val initialName = remember(showEditName) { profile.name }
        var draft by remember(showEditName) { mutableStateOf(initialName) }
        RetroDialog("Edit Name", { showEditName = false }, {
            profileVm.updateName(draft, context); showEditName = false
        }) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                label = { Text("Display Name") }, singleLine = true,
                placeholder = { Text("Enter your name", color = Muted) },
                colors = fColors, modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Edit Email
    if (showEditEmail) {
        val initialEmail = remember(showEditEmail) { profile.email }
        var draft by remember(showEditEmail) { mutableStateOf(initialEmail) }
        RetroDialog("Edit Email", { showEditEmail = false }, {
            profileVm.updateEmail(draft, context); showEditEmail = false
        }) {
            OutlinedTextField(
                value = draft, onValueChange = { draft = it },
                label = { Text("Email Address") }, singleLine = true,
                placeholder = { Text("Enter your email", color = Muted) },
                colors = fColors, modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Change Password
    if (showChangePassword) {
        var curPw    by remember { mutableStateOf("") }
        var newPw    by remember { mutableStateOf("") }
        var confPw   by remember { mutableStateOf("") }
        var showCur  by remember { mutableStateOf(false) }
        var showNew  by remember { mutableStateOf(false) }
        var mismatch by remember { mutableStateOf(false) }
        RetroDialog("Change Password", { showChangePassword = false }, {
            if (newPw != confPw) { mismatch = true; return@RetroDialog }
            profileVm.changePassword(curPw, newPw, context)
            showChangePassword = false
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = curPw, onValueChange = { curPw = it },
                    label = { Text("Current Password") }, singleLine = true,
                    visualTransformation = if (showCur) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton({ showCur = !showCur }) {
                        Icon(if (showCur) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Gold)
                    }}, colors = fColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = newPw, onValueChange = { newPw = it; mismatch = false },
                    label = { Text("New Password") }, singleLine = true,
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton({ showNew = !showNew }) {
                        Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = Gold)
                    }}, colors = fColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = confPw, onValueChange = { confPw = it; mismatch = false },
                    label = { Text("Confirm Password") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), isError = mismatch,
                    supportingText = { if (mismatch) Text("Passwords do not match", color = Color(0xFFFF6D6D)) },
                    colors = fColors, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // Logout confirm
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = CardBg, titleContentColor = Gold,
            title = { Text("Log Out?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to log out?", color = Muted, fontFamily = FontFamily.Monospace) },
            confirmButton = {
                Button(onClick = {
                    profileVm.logout()
                    navController.navigate(ROUTE_LOGIN) { popUpTo(0) { inclusive = true } }
                }, colors = ButtonDefaults.buttonColors(containerColor = Crimson), shape = RoundedCornerShape(8.dp)) {
                    Text("Log Out", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false },
                    border = BorderStroke(1.dp, Color(0xFF4A3F6B)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                    shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Column(
        modifier = modifier
            .background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, color = Gold, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SettingRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = label, tint = Gold, modifier = Modifier.size(18.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = Color.White, fontSize = 13.sp,
                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        }
        IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit $label",
                tint = BorderIdle, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun RetroDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit,
                        content: @Composable () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBg, titleContentColor = Gold,
        title = { Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text  = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                shape = RoundedCornerShape(8.dp)) {
                Text("Save", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, border = BorderStroke(1.dp, Color(0xFF4A3F6B)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                shape = RoundedCornerShape(8.dp)) {
                Text("Cancel", fontFamily = FontFamily.Monospace)
            }
        })
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(rememberNavController())
}