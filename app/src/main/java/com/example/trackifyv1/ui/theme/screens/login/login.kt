package com.example.trackifyv1.ui.theme.screens.login

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.trackifyv1.navigation.ROUTE_REGISTER

@Composable
fun LoginScreen(navController: NavController) {
    val context: Context = LocalContext.current
    val viewModel: AuthViewModel = viewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = Color(0xFFD4A017),
        unfocusedBorderColor    = Color(0xFF4A3F6B),
        focusedTextColor        = Color.White,
        unfocusedTextColor      = Color.White,
        focusedContainerColor   = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        cursorColor             = Color(0xFFD4A017),
        focusedLabelColor       = Color(0xFFD4A017),
        unfocusedLabelColor     = Color(0xFF9E9E9E)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A0533), Color(0xFF0D2B1A), Color(0xFF1A1A00))))
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Brush.linearGradient(listOf(Color(0xFFD4A017), Color(0xFF8B0000))), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("TK", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = Color(0xFFD4A017), fontFamily = FontFamily.Monospace)
        Text("Login to continue", fontSize = 14.sp, color = Color(0xFF9E9E9E),
            fontFamily = FontFamily.Monospace)

        Spacer(Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1C1C1C).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Your Information", color = Color(0xFFD4A017), fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace)

            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), colors = fieldColors, enabled = !isLoading)

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide" else "Show", tint = Color(0xFFD4A017))
                    }
                },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors, enabled = !isLoading
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.login(email, password, navController, context) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000)),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFFD4A017), modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Login", color = Color(0xFFD4A017), fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { navController.navigate(ROUTE_REGISTER) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFF4A3F6B)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF1A0533), contentColor = Color(0xFFD4A017)),
            enabled = !isLoading
        ) {
            Text("Create an account", color = Color(0xFFD4A017), fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.weight(1f))
        Text("© 2026 Trackify", fontSize = 11.sp, color = Color(0xFF4A3F6B))
        Spacer(Modifier.height(16.dp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() { LoginScreen(rememberNavController()) }
