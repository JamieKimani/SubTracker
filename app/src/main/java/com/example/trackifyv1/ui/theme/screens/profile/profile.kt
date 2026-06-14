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
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.models.ProfileViewModel
import com.example.trackifyv1.models.ThemeViewModel
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.models.BudgetViewModel
import com.example.trackifyv1.ui.theme.screens.dashboard.monthlyAmount
import com.example.trackifyv1.navigation.ROUTE_LOGIN
import com.example.trackifyv1.ui.theme.screens.dashboard.SheetFilter
import com.example.trackifyv1.ui.theme.screens.dashboard.SubscriptionDetailSheet
import com.example.trackifyv1.ui.theme.screens.dashboard.monthlyAmount

private val Gold       = Color(0xFFD4A017)
private val Crimson    = Color(0xFF8B0000)
private val DarkPurple = Color(0xFF1A0533)
private val CardBg     = Color(0xFF1C1C1C)
private val Muted      = Color(0xFF9E9E9E)
private val BorderIdle = Color(0xFF4A3F6B)
private val TealAccent = Color(0xFF00C8A0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context       = LocalContext.current
    val profileVm     = viewModel<ProfileViewModel>()
    val subscriptionVm = viewModel<SubscriptionViewModel>()
    val themeVm        = viewModel<ThemeViewModel>()
    val isDarkMode     by themeVm.isDarkMode.collectAsState()
    val subscriptions  by subscriptionVm.subscriptions.collectAsState()
    val budgetVm       = viewModel<BudgetViewModel>()
    val budgets        by budgetVm.budgets.collectAsState()
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetCategory   by remember { mutableStateOf("") }
    var budgetAmount     by remember { mutableStateOf("") }

    val categories = subscriptions.map { it.category.ifBlank { "Uncategorized" } }.distinct().sorted()

    val profile       by profileVm.profile.collectAsState()
    val isLoading     by profileVm.isLoading.collectAsState()
    val subscriptions by subscriptionVm.subscriptions.collectAsState()

    var showEditName       by remember { mutableStateOf(false) }
    var showEditEmail      by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showLogoutConfirm  by remember { mutableStateOf(false) }
    var sheetFilter        by remember { mutableStateOf(SheetFilter.NONE) }
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val activeSubs   = subscriptions.filter { it.isActive }
    val monthlySpend = activeSubs.sumOf { monthlyAmount(it) }
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
        Text("Profile", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

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
                Text(profile.email.ifBlank { "—" }, color = Muted, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TappableStatCard(
                modifier = Modifier.weight(1f),
                value    = "${activeSubs.size}",
                label    = "Active Subs",
                accent   = Gold,
                onClick  = { sheetFilter = SheetFilter.ACTIVE }
            )
            TappableStatCard(
                modifier = Modifier.weight(1f),
                value    = "KES ${"%.0f".format(monthlySpend)}",
                label    = "Monthly",
                accent   = TealAccent,
                onClick  = { sheetFilter = SheetFilter.MONTHLY }
            )
            TappableStatCard(
                modifier = Modifier.weight(1f),
                value    = "KES ${"%.0f".format(yearlySpend)}",
                label    = "Yearly",
                accent   = Color(0xFFFF6D00),
                onClick  = { sheetFilter = SheetFilter.YEARLY }
            )
        }

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
            SettingRow(Icons.Default.Person,  "Display Name",    profile.name.ifBlank { "Not set" })  { showEditName = true }
            HorizontalDivider(color = BorderIdle.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(Icons.Default.Email,   "Email Address",   profile.email.ifBlank { "Not set" }) { showEditEmail = true }
            HorizontalDivider(color = BorderIdle.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            SettingRow(Icons.Default.Lock,    "Change Password", "••••••••")                          { showChangePassword = true }
            HorizontalDivider(color = BorderIdle.copy(alpha = 0.3f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = "Theme", tint = Gold, modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("Appearance", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(if (isDarkMode) "Dark" else "Light", color = Color.White, fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { themeVm.toggle(context) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Gold, checkedTrackColor = Gold.copy(alpha = 0.4f))
                )
            }
        }

        if (categories.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Monthly Budgets", color = Gold, fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    TextButton(onClick = {
                        budgetCategory = categories.first(); budgetAmount = ""; showBudgetDialog = true
                    }) {
                        Text("Set budget", color = TealAccent, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = BorderIdle.copy(alpha = 0.3f), thickness = 0.5.dp)
                categories.forEach { cat ->
                    val budget = budgets[cat]
                    val spent  = subscriptions.filter { it.isActive && it.category.ifBlank { "Uncategorized" } == cat }
                        .sumOf { monthlyAmount(it) }
                    val over   = budget != null && spent > budget
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(cat, color = if (over) Color(0xFFE53935) else Color.White,
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            Text(
                                if (budget != null) "KES ${"%.0f".format(spent)} / ${"%.0f".format(budget)} limit"
                                else "KES ${"%.0f".format(spent)} / mo — no limit",
                                color = if (over) Color(0xFFE53935) else Muted,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = {
                            budgetCategory = cat
                            budgetAmount   = budget?.let { "%.0f".format(it) } ?: ""
                            showBudgetDialog = true
                        }, Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Edit budget", tint = BorderIdle, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (cat != categories.last()) {
                        HorizontalDivider(color = BorderIdle.copy(alpha = 0.2f), thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        OutlinedButton(
            onClick  = { subscriptionVm.exportToCsv(context) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp),
            border   = BorderStroke(1.dp, TealAccent),
            colors   = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = TealAccent)
        ) {
            Icon(Icons.Default.FileDownload, "Export", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Export to CSV", fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick  = { showLogoutConfirm = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Crimson)
        ) {
            Text("Log Out", color = Gold, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Text("© 2026 Trackify", fontSize = 11.sp, color = BorderIdle,
            fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().wrapContentWidth())
    }

    if (sheetFilter != SheetFilter.NONE) {
        SubscriptionDetailSheet(
            filter     = sheetFilter,
            sheetState = sheetState,
            onDismiss  = { sheetFilter = SheetFilter.NONE }
        )
    }

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

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            containerColor    = CardBg, titleContentColor = Gold,
            title = { Text("Budget for $budgetCategory", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set a monthly spending limit for this category.", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    OutlinedTextField(
                        value = budgetAmount, onValueChange = { budgetAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Monthly limit (KES)") }, singleLine = true,
                        placeholder = { Text("e.g. 2000", color = Muted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Gold, unfocusedBorderColor = BorderIdle,
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            cursorColor = Gold, focusedLabelColor = Gold, unfocusedLabelColor = Muted
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = budgetAmount.toDoubleOrNull() ?: 0.0
                        budgetVm.setBudget(budgetCategory, amt, context)
                        showBudgetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                    shape  = RoundedCornerShape(8.dp)
                ) { Text("Save", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showBudgetDialog = false },
                    border  = BorderStroke(1.dp, BorderIdle),
                    colors  = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Cancel", fontFamily = FontFamily.Monospace) }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor   = CardBg, titleContentColor = Gold,
            title = { Text("Log Out?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to log out?", color = Muted, fontFamily = FontFamily.Monospace) },
            confirmButton = {
                Button(onClick = {
                    profileVm.logout()
                    com.google.android.gms.auth.api.signin.GoogleSignIn
                        .getClient(context,
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            ).build()
                        ).signOut()
                    navController.navigate(ROUTE_LOGIN) { popUpTo(0) { inclusive = true } }
                }, colors = ButtonDefaults.buttonColors(containerColor = Crimson), shape = RoundedCornerShape(8.dp)) {
                    Text("Log Out", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirm = false },
                    border = BorderStroke(1.dp, Color(0xFF4A3F6B)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                    shape  = RoundedCornerShape(8.dp)) {
                    Text("Cancel", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun TappableStatCard(modifier: Modifier, value: String, label: String, accent: Color, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Icon(Icons.Default.ChevronRight, null, tint = accent.copy(alpha = 0.4f), modifier = Modifier.size(10.dp))
        }
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
private fun RetroDialog(title: String, onDismiss: () -> Unit, onConfirm: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = CardBg, titleContentColor = Gold,
        title = { Text(title, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text  = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() } },
        confirmButton = {
            Button(onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                shape  = RoundedCornerShape(8.dp)) {
                Text("Save", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss,
                border = BorderStroke(1.dp, Color(0xFF4A3F6B)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                shape  = RoundedCornerShape(8.dp)) {
                Text("Cancel", fontFamily = FontFamily.Monospace)
            }
        })
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() { ProfileScreen(rememberNavController()) }
