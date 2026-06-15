package com.example.trackifyv1.ui.theme.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import android.widget.Toast
import com.example.trackifyv1.models.ProfileViewModel
import com.example.trackifyv1.models.ThemeViewModel
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.models.BudgetViewModel
import com.example.trackifyv1.models.RecentlyDeletedViewModel
import com.example.trackifyv1.ui.theme.screens.dashboard.monthlyAmount
import com.example.trackifyv1.navigation.ROUTE_LOGIN
import android.net.Uri
import com.example.trackifyv1.navigation.ROUTE_NOTIFICATION_HISTORY
import com.example.trackifyv1.ui.theme.screens.dashboard.SheetFilter
import com.example.trackifyv1.ui.theme.screens.dashboard.SubscriptionDetailSheet

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
    val themeVm          = viewModel<ThemeViewModel>()
    val isDarkMode       by themeVm.isDarkMode.collectAsState()
    val subscriptions    by subscriptionVm.subscriptions.collectAsState()
    val budgetVm         = viewModel<BudgetViewModel>()
    val deletedVm        = viewModel<RecentlyDeletedViewModel>()
    val recentlyDeleted  by deletedVm.deleted.collectAsState()
    var showDeletedSheet by remember { mutableStateOf(false) }
    var showRestoreInfo  by remember { mutableStateOf(false) }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readText() ?: ""
            if (json.isNotBlank()) subscriptionVm.restoreFromJson(context, json)
            else android.widget.Toast.makeText(context, "Empty file.", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Could not read file.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val budgets        by budgetVm.budgets.collectAsState()
    var showBudgetDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            if (!text.isNullOrBlank()) if (!text.isNullOrBlank()) subscriptionVm.restoreFromJson(context, text)
            else Toast.makeText(context, "Could not read file.", Toast.LENGTH_SHORT).show()
        }
    }
    var budgetCategory   by remember { mutableStateOf("") }
    var budgetAmount     by remember { mutableStateOf("") }
    val categories = subscriptions.map { it.category.ifBlank { "Uncategorized" } }.distinct().sorted()
    val profile       by profileVm.profile.collectAsState()
    val isLoading     by profileVm.isLoading.collectAsState()

    var showEditName       by remember { mutableStateOf(false) }
    var showEditEmail      by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var showLogoutConfirm  by remember { mutableStateOf(false) }
    var showClearConfirm1  by remember { mutableStateOf(false) }
    var showClearConfirm2  by remember { mutableStateOf(false) }
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
            SettingRow(Icons.Default.Notifications, "Notification History", "View recent alerts") { navController.navigate(ROUTE_NOTIFICATION_HISTORY) }
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick  = { subscriptionVm.exportToJson(context) },
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, TealAccent),
                colors   = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = TealAccent)
            ) {
                Icon(Icons.Default.FileDownload, "Backup", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Backup", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick  = { importLauncher.launch("application/json") },
                modifier = Modifier.weight(1f).height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, Color(0xFFFF6D00)),
                colors   = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = Color(0xFFFF6D00))
            ) {
                Icon(Icons.Default.FileUpload, "Restore", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedButton(
            onClick  = { subscriptionVm.exportToCsv(context) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(10.dp),
            border   = BorderStroke(1.dp, TealAccent.copy(alpha = 0.6f)),
            colors   = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent, contentColor = TealAccent)
        ) {
            Icon(Icons.Default.TableChart, "CSV", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Export to CSV", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFF1A0808).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Danger Zone",
                color = Color(0xFFE53935),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            HorizontalDivider(color = Color(0xFFE53935).copy(alpha = 0.25f), thickness = 0.5.dp)
            Text(
                "These actions are permanent and cannot be undone.",
                color = Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            OutlinedButton(
                onClick  = { showClearConfirm1 = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, Color(0xFFE53935)),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFE53935).copy(alpha = 0.08f),
                    contentColor   = Color(0xFFE53935)
                )
            ) {
                Icon(Icons.Default.DeleteForever, "Clear", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Clear all subscription data", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            OutlinedButton(
                onClick  = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, Crimson),
                colors   = ButtonDefaults.outlinedButtonColors(
                    containerColor = Crimson.copy(alpha = 0.08f),
                    contentColor   = Color.White
                )
            ) {
                Icon(Icons.Default.Logout, "Logout", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Log Out", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
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

    if (showDeletedSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showDeletedSheet = false },
            containerColor   = CardBg
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("Recently Deleted", color = Gold, fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp))
                Text("Subscriptions are permanently erased after 7 days.",
                    color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp))
                recentlyDeleted.forEach { deleted ->
                    val daysLeft = ((deleted.deletedAt + 7L * 24 * 60 * 60 * 1000 - System.currentTimeMillis()) /
                        (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                    Row(
                        Modifier.fillMaxWidth()
                            .background(Color(0xFF1C1C1C).copy(alpha = 0.8f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(deleted.subscriptionName, color = Color.White,
                                fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("KES ${deleted.subscriptionAmount} · ${deleted.billingCycle} · ${deleted.category}",
                                color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Text("Expires in $daysLeft day${if (daysLeft != 1) "s" else ""}",
                                color = if (daysLeft <= 1) Color(0xFFE53935) else Muted,
                                fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = { subscriptionVm.restoreSubscription(deleted, context) },
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Restore, "Restore", tint = TealAccent, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { subscriptionVm.permanentlyDelete(deleted.id, context) },
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.DeleteForever, "Delete forever", tint = Color(0xFFE53935),
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
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

    if (showClearConfirm1) {
        AlertDialog(
            onDismissRequest = { showClearConfirm1 = false },
            containerColor   = CardBg, titleContentColor = Color(0xFFE53935),
            title = { Text("Clear all data?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This will permanently delete ALL ${subscriptions.size} subscription${if (subscriptions.size != 1) "s" else ""} from your account and from the database.",
                        color = Muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp
                    )
                    Text(
                        "This cannot be undone. Are you sure?",
                        color = Color(0xFFE53935), fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showClearConfirm1 = false; showClearConfirm2 = true },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Yes, continue", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirm1 = false },
                    border  = BorderStroke(1.dp, BorderIdle),
                    colors  = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Cancel", fontFamily = FontFamily.Monospace) }
            }
        )
    }

    if (showClearConfirm2) {
        AlertDialog(
            onDismissRequest = { showClearConfirm2 = false },
            containerColor   = CardBg, titleContentColor = Color(0xFFE53935),
            title = { Text("Final confirmation", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text  = {
                Text(
                    "Tap ERASE NOW to permanently delete all your subscription data from this app and from Firebase. There is no going back.",
                    color = Muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirm2 = false
                        subscriptionVm.clearAllSubscriptions(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    shape  = RoundedCornerShape(8.dp)
                ) { Text("ERASE NOW", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearConfirm2 = false },
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
