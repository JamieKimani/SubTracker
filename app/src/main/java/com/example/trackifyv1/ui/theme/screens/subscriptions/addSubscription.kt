package com.example.trackifyv1.ui.theme.screens.subscriptions

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.helpers.scheduleNotification
import com.example.trackifyv1.models.SubscriptionViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val Gold = Color(0xFFD4A017)
private val Crimson = Color(0xFF8B0000)
private val DarkPurple = Color(0xFF1A0533)
private val DarkGreen = Color(0xFF0D2B1A)
private val DarkYellow = Color(0xFF1A1A00)
private val CardBg = Color(0xFF1C1C1C)
private val Muted = Color(0xFF9E9E9E)
private val BorderIdle = Color(0xFF4A3F6B)

val subscriptionCategories = listOf(
    "Streaming", "Music", "Cloud Storage", "Productivity", "Gaming",
    "News & Magazines", "Fitness & Health", "Education", "Finance",
    "Social Media", "VPN & Security", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionScreen(navController: NavController) {
    var subscriptionName by remember { mutableStateOf("") }
    var subscriptionAmount by remember { mutableStateOf("") }
    var subscriptionDate by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val vm: SubscriptionViewModel = viewModel()

    fun makePicker(onDateSet: (String) -> Unit) = DatePickerDialog(context, { _, y, m, d ->
        onDateSet("%02d/%02d/%04d".format(d, m + 1, y))
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val startPicker = remember { makePicker { subscriptionDate = it } }
    val expiryPicker = remember { makePicker { expiryDate = it } }
    val renewPicker = remember { makePicker { nextRenewalDate = it } }
    val reminderPicker = remember { makePicker { reminderDate = it } }

    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = BorderIdle,
        focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent, cursorColor = Gold, focusedLabelColor = Gold, unfocusedLabelColor = Muted)

    Scaffold(topBar = {
        TopAppBar(title = { Text("Add Subscription", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { navController.navigateUp() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Gold) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple))
    }) { padding ->
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkPurple, DarkGreen, DarkYellow)))
            .padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {

            Column(Modifier.fillMaxWidth().background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Subscription Details", color = Gold, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)

                OutlinedTextField(value = subscriptionName, onValueChange = { subscriptionName = it },
                    label = { Text("Subscription Name") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = subscriptionAmount, onValueChange = { subscriptionAmount = it },
                    label = { Text("Amount (KES)") }, singleLine = true, colors = fieldColors, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                    OutlinedTextField(value = selectedCategory, onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Expand", tint = Gold) },
                        colors = fieldColors, modifier = Modifier.fillMaxWidth().menuAnchor())
                    ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false },
                        modifier = Modifier.background(CardBg)) {
                        subscriptionCategories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat, color = if (cat == selectedCategory) Gold else Color.White,
                                fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                                onClick = { selectedCategory = cat; categoryExpanded = false })
                        }
                    }
                }

                DateField("Start Date", subscriptionDate, fieldColors) { startPicker.show() }
                DateField("Expiry Date", expiryDate, fieldColors) { expiryPicker.show() }
                DateField("Reminder Date", reminderDate, fieldColors) { reminderPicker.show() }
            }

            Spacer(Modifier.height(20.dp))

            Button(onClick = {
                vm.addSubscription(subscriptionName, subscriptionAmount, subscriptionDate, expiryDate, nextRenewalDate, reminderDate, context, selectedCategory)
                if (reminderDate.isNotEmpty()) {
                    try {
                        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(reminderDate)
                        if (date != null) {
                            scheduleNotification(context, "Reminder: $subscriptionName", "Your subscription is due soon!", date.time)
                            Toast.makeText(context, "Reminder set for $reminderDate", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid date format for reminder", Toast.LENGTH_SHORT).show()
                    }
                }
                navController.navigateUp()
            }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Crimson)) {
                Text("Save Subscription", color = Gold, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
            Text("© 2026 Trackify", fontSize = 11.sp, color = BorderIdle, fontFamily = FontFamily.Monospace,
                modifier = Modifier.fillMaxWidth().wrapContentWidth())
        }
    }
}

@Composable
private fun DateField(label: String, value: String, colors: TextFieldColors, onPickerClick: () -> Unit) {
    OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(label) },
        placeholder = { Text("DD/MM/YYYY", color = Muted) },
        trailingIcon = { IconButton(onClick = onPickerClick) { Icon(Icons.Default.DateRange, "Pick $label", tint = Gold) } },
        colors = colors, modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AddSubscriptionScreenPreview() {
    AddSubscriptionScreen(rememberNavController())
}
