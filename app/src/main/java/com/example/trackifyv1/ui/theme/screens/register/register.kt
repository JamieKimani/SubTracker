package com.example.trackifyv1.ui.theme.screens.register

import android.app.Activity.RESULT_OK
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.models.AuthViewModel
import com.example.trackifyv1.navigation.ROUTE_LOGIN
import com.example.trackifyv1.ui.theme.*

@Composable
fun RegisterScreen(navController: NavController) {
    val context: Context = LocalContext.current
    val viewModel        = viewModel<AuthViewModel>()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var name          by remember { mutableStateOf("") }
    var email         by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var confirmPw     by remember { mutableStateOf("") }
    var showPw        by remember { mutableStateOf(false) }
    var showConfirmPw by remember { mutableStateOf(false) }

    val pwMismatch = confirmPw.isNotBlank() && confirmPw != password

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data, navController, context)
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Gold, unfocusedBorderColor = BorderIdle,
        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        cursorColor = Gold, focusedLabelColor = Gold, unfocusedLabelColor = Muted
    )

    Column(
        modifier = Modifier.fillMaxSize().background(AppGradient)
            .statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.size(80.dp)
                .background(Brush.linearGradient(listOf(Gold, Crimson)), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) { Text("TK", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(12.dp))
        Text("Create Account", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Gold, fontFamily = FontFamily.Monospace)
        Text("Sign up to start tracking", fontSize = 14.sp, color = Muted, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Personal Details", color = Gold, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("Full Name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors, enabled = !isLoading)

            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email Address") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors, enabled = !isLoading)

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPw = !showPw }) {
                        Icon(if (showPw) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPw) "Hide" else "Show", tint = Gold)
                    }
                },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors, enabled = !isLoading
            )

            OutlinedTextField(
                value = confirmPw, onValueChange = { confirmPw = it },
                label = { Text("Confirm Password") }, singleLine = true,
                visualTransformation = if (showConfirmPw) VisualTransformation.None else PasswordVisualTransformation(),
                isError = pwMismatch,
                supportingText = { if (pwMismatch) Text("Passwords do not match", color = Color(0xFFFF6D6D)) },
                trailingIcon = {
                    IconButton(onClick = { showConfirmPw = !showConfirmPw }) {
                        Icon(if (showConfirmPw) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showConfirmPw) "Hide" else "Show", tint = Gold)
                    }
                },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors, enabled = !isLoading
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick  = { viewModel.register(name, email, password, confirmPw, navController, context) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Crimson),
            enabled  = !isLoading && !pwMismatch
        ) {
            if (isLoading)
                CircularProgressIndicator(color = Gold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else
                Text("Register", color = Gold, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(Modifier.weight(1f), color = BorderIdle.copy(alpha = 0.5f))
            Text("  or  ", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            HorizontalDivider(Modifier.weight(1f), color = BorderIdle.copy(alpha = 0.5f))
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick  = { googleLauncher.launch(viewModel.getGoogleSignInIntent(context)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp),
            border   = androidx.compose.foundation.BorderStroke(1.dp, BorderIdle),
            colors   = ButtonDefaults.outlinedButtonColors(containerColor = CardBg, contentColor = Color.White),
            enabled  = !isLoading
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("G", color = Color(0xFF4285F4), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Continue with Google", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = { navController.navigate(ROUTE_LOGIN) }, enabled = !isLoading) {
            Text("Already have an account? Login", color = TealAccent, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Text("© 2026 Trackify", fontSize = 11.sp, color = BorderIdle)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() { RegisterScreen(rememberNavController()) }
