package com.example.trackifyv1.ui.theme.screens.subscriptions

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.notifications.NotificationHelper
import java.util.Calendar

private val AddGold       = Color(0xFFD4A017)
private val AddCrimson    = Color(0xFF8B0000)
private val AddDarkPurple = Color(0xFF1A0533)
private val AddDarkGreen  = Color(0xFF0D2B1A)
private val AddDarkYellow = Color(0xFF1A1A00)
private val AddCardBg     = Color(0xFF1C1C1C)
private val AddMuted      = Color(0xFF9E9E9E)
private val AddBorderIdle = Color(0xFF4A3F6B)

val subscriptionCategories = listOf(
    "Streaming", "Music", "Cloud Storage", "Productivity", "Gaming",
    "News & Magazines", "Fitness & Health", "Education", "Finance",
    "Social Media", "VPN & Security", "Other"
)

val billingCycles = listOf("Weekly", "Monthly", "Quarterly", "Yearly")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionScreen(navController: NavController) {
    var subscriptionName   by remember { mutableStateOf("") }
    var subscriptionAmount by remember { mutableStateOf("") }
    var subscriptionDate   by remember { mutableStateOf("") }
    var expiryDate         by remember { mutableStateOf("") }
    var reminderDate       by remember { mutableStateOf("") }
    var selectedCategory   by remember { mutableStateOf("") }
    var selectedCycle      by remember { mutableStateOf("Monthly") }
    var categoryExpanded   by remember { mutableStateOf(false) }
    var cycleExpanded      by remember { mutableStateOf(false) }
    var amountError        by remember { mutableStateOf(false) }
    var isTrial            by remember { mutableStateOf(false) }
    var trialEndDate       by remember { mutableStateOf("") }

    val context  = LocalContext.current
    val calendar = Calendar.getInstance()
    val vm: SubscriptionViewModel = viewModel()

    fun makePicker(onDateSet: (String) -> Unit) = DatePickerDialog(
        context, { _, y, m, d -> onDateSet("%02d/%02d/%04d".format(d, m + 1, y)) },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val startPicker     = remember { makePicker { subscriptionDate = it } }
    val expiryPicker    = remember { makePicker { expiryDate = it } }
    val reminderPicker  = remember { makePicker { reminderDate = it } }
    val trialEndPicker  = remember { makePicker { trialEndDate = it } }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = AddGold,        unfocusedBorderColor    = AddBorderIdle,
        focusedTextColor        = Color.White,    unfocusedTextColor      = Color.White,
        focusedContainerColor   = Color.Transparent, unfocusedContainerColor = Color.Transparent,
        cursorColor             = AddGold,        focusedLabelColor       = AddGold,
        unfocusedLabelColor     = AddMuted
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Subscription", color = AddGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AddGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AddDarkPurple)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(AddDarkPurple, AddDarkGreen, AddDarkYellow)))
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(AddCardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Subscription Details", color = AddGold,
                    fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)

                OutlinedTextField(
                    value = subscriptionName, onValueChange = { subscriptionName = it },
                    label = { Text("Subscription Name") }, singleLine = true,
                    colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = subscriptionAmount,
                    onValueChange = { subscriptionAmount = it; amountError = it.isNotBlank() && it.toDoubleOrNull() == null },
                    label = { Text("Amount (KES)") }, singleLine = true,
                    isError = amountError,
                    supportingText = { if (amountError) Text("Enter a valid number e.g. 500 or 9.99", color = Color(0xFFFF6D6D)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors, modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(expanded = cycleExpanded, onExpandedChange = { cycleExpanded = !cycleExpanded }) {
                    OutlinedTextField(
                        value = selectedCycle, onValueChange = {}, readOnly = true,
                        label = { Text("Billing Cycle") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = AddGold) },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(expanded = cycleExpanded, onDismissRequest = { cycleExpanded = false },
                        modifier = Modifier.background(AddCardBg)) {
                        billingCycles.forEach { cycle ->
                            DropdownMenuItem(
                                text = { Text(cycle, color = if (cycle == selectedCycle) AddGold else Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                                onClick = { selectedCycle = cycle; cycleExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(
                        value = selectedCategory, onValueChange = {}, readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = AddGold) },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(AddCardBg)) {
                        subscriptionCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat, color = if (cat == selectedCategory) AddGold else Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                                onClick = { selectedCategory = cat; categoryExpanded = false }
                            )
                        }
                    }
                }

                AddDateField("Start Date",    subscriptionDate, fieldColors) { startPicker.show() }
                AddDateField("Expiry Date",   expiryDate,       fieldColors) { expiryPicker.show() }
                AddDateField("Reminder Date", reminderDate,     fieldColors) { reminderPicker.show() }

                if (reminderDate.isNotBlank()) {
                    Text(
                        "📅 Reminder set for $reminderDate at ${NotificationHelper.REMINDER_HOUR}:00 AM. Repeats $selectedCycle.",
                        color = AddGold.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace, fontSize = 11.sp
                    )
                }

                HorizontalDivider(color = AddBorderIdle.copy(alpha = 0.4f), thickness = 0.5.dp)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Free Trial", color = AddGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Track this as a free trial with an urgent reminder", color = AddMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isTrial, onCheckedChange = { isTrial = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AddGold, checkedTrackColor = AddGold.copy(alpha = 0.4f))
                    )
                }

                if (isTrial) {
                    AddDateField("Trial Ends", trialEndDate, fieldColors) { trialEndPicker.show() }
                    if (trialEndDate.isNotBlank()) {
                        Text(
                            "⏳ You'll get an urgent reminder as the trial ends on $trialEndDate.",
                            color = Color(0xFFFF6D00), fontFamily = FontFamily.Monospace, fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (amountError) return@Button
                    vm.addSubscription(
                        subscriptionName, subscriptionAmount, subscriptionDate,
                        expiryDate, reminderDate, context, selectedCategory, selectedCycle,
                        isTrial = isTrial, trialEndDate = trialEndDate,
                        onSuccess = { navController.navigateUp() }
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape  = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AddCrimson)
            ) {
                Text("Save Subscription", color = AddGold, fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            Text("© 2026 Trackify", fontSize = 11.sp, color = AddBorderIdle,
                fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().wrapContentWidth())
        }
    }
}

@Composable
private fun AddDateField(label: String, value: String, colors: TextFieldColors, onPickerClick: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
        placeholder = { Text("DD/MM/YYYY", color = AddMuted) },
        trailingIcon = { IconButton(onClick = onPickerClick) { Icon(Icons.Default.DateRange, "Pick $label", tint = AddGold) } },
        colors = colors, modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddSubscriptionScreenPreview() { AddSubscriptionScreen(rememberNavController()) }
