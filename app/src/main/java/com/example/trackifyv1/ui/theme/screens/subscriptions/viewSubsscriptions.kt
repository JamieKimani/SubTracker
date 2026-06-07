package com.example.trackifyv1.ui.theme.screens.subscriptions

import androidx.compose.foundation.BorderStroke
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.trackifyv1.models.SubscriptionModel
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.navigation.ROUTE_ADD_SUBSCRIPTION

// Private colour palette — does not clash with dashboard's top-level vals
private val SubGold       = Color(0xFFD4A017)
private val SubCrimson    = Color(0xFF8B0000)
private val SubDarkPurple = Color(0xFF1A0533)
private val SubDarkGreen  = Color(0xFF0D2B1A)
private val SubDarkYellow = Color(0xFF1A1A00)
private val SubCardBg     = Color(0xFF1C1C1C)
private val SubMuted      = Color(0xFF9E9E9E)
private val SubBorderIdle = Color(0xFF4A3F6B)

private val subCategoryColors = listOf(
    Color(0xFFD4A017), Color(0xFF8B0000), Color(0xFF00BCD4), Color(0xFF7C4DFF),
    Color(0xFF00E676), Color(0xFFFF6D00), Color(0xFFE91E63), Color(0xFF1DE9B6)
)

private fun catColor(category: String): Color {
    val cats = subscriptionCategories
    val idx  = cats.indexOf(category).takeIf { it >= 0 }
        ?: ((category.hashCode() and 0x7FFFFFFF))
    return subCategoryColors[idx % subCategoryColors.size]
}

/**
 * [isStandalone] = true when navigated to directly; false when embedded as a tab inside Dashboard.
 * FAB is hidden when embedded because Dashboard already provides one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSubscriptionsScreen(navController: NavController, isStandalone: Boolean = true) {
    val vm: SubscriptionViewModel = viewModel()
    val subscriptions by vm.subscriptions.collectAsState()
    val gradientBg    = Brush.verticalGradient(listOf(SubDarkPurple, SubDarkGreen, SubDarkYellow))

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Subscriptions", color = SubGold,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (isStandalone) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SubGold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isStandalone) SubDarkPurple else Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (isStandalone) {
                FloatingActionButton(
                    onClick        = { navController.navigate(ROUTE_ADD_SUBSCRIPTION) },
                    containerColor = SubCrimson,
                    contentColor   = SubGold
                ) {
                    Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(padding)
        ) {
            if (subscriptions.isEmpty()) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📭", fontSize = 48.sp)
                    Text("No subscriptions yet",    color = SubMuted,      fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                    Text("Tap + to add your first", color = SubBorderIdle, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier         = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding   = PaddingValues(bottom = 80.dp)
                ) {
                    items(items = subscriptions, key = { it.id }) { sub ->
                        SubscriptionCard(
                            subscription = sub,
                            onDelete     = { ctx -> vm.deleteSubscription(sub.id, ctx) },
                            onUpdate     = { updated, ctx -> vm.updateSubscription(updated, ctx) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionCard(
    subscription: SubscriptionModel,
    onDelete: (Context) -> Unit,
    onUpdate: (SubscriptionModel, Context) -> Unit
) {
    val context          = LocalContext.current
    val chipColor        = catColor(subscription.category.ifBlank { "Other" })
    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = SubCardBg.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    subscription.subscriptionName.ifBlank { "Unnamed" },
                    color = SubGold, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = { showEditDialog = true },   Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit,   "Edit",   tint = SubGold,   modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDeleteDialog = true }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = SubCrimson, modifier = Modifier.size(18.dp))
                    }
                }
            }

            if (subscription.category.isNotBlank()) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        subscription.category, color = chipColor,
                        fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = SubBorderIdle.copy(alpha = 0.4f), thickness = 0.5.dp)
            SubDetailRow("Amount",     "KES ${subscription.subscriptionAmount}")
            SubDetailRow("Start Date", subscription.subscriptionDate.ifBlank { "—" })
            SubDetailRow("Expiry",     subscription.expiryDate.ifBlank { "—" })
            SubDetailRow("Reminder",   subscription.reminderDate.ifBlank { "—" })
        }
    }

    // ── Edit Dialog ──────────────────────────────────────────────────────────
    if (showEditDialog) {
        var editName    by remember { mutableStateOf(subscription.subscriptionName) }
        var editAmount  by remember { mutableStateOf(subscription.subscriptionAmount) }
        var editCat     by remember { mutableStateOf(subscription.category) }
        var amountErr   by remember { mutableStateOf(false) }
        var catExpanded by remember { mutableStateOf(false) }

        val fColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = SubGold, unfocusedBorderColor    = SubBorderIdle,
            focusedTextColor        = Color.White, unfocusedTextColor  = Color.White,
            focusedContainerColor   = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            cursorColor             = SubGold, focusedLabelColor       = SubGold, unfocusedLabelColor = SubMuted
        )

        AlertDialog(
            onDismissRequest  = { showEditDialog = false },
            containerColor    = SubCardBg,
            titleContentColor = SubGold,
            title = {
                Text("Edit Subscription", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        label = { Text("Name") }, singleLine = true,
                        colors = fColors, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it; amountErr = it.isNotBlank() && it.toDoubleOrNull() == null },
                        label = { Text("Amount (KES)") }, singleLine = true,
                        isError = amountErr,
                        supportingText = {
                            if (amountErr) Text("Enter a valid number", color = Color(0xFFFF6D6D))
                        },
                        colors = fColors, modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded        = catExpanded,
                        onExpandedChange = { catExpanded = !catExpanded }
                    ) {
                        OutlinedTextField(
                            value = editCat, onValueChange = {}, readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = SubGold) },
                            colors = fColors,
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded        = catExpanded,
                            onDismissRequest = { catExpanded = false },
                            modifier        = Modifier.background(SubCardBg)
                        ) {
                            subscriptionCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text    = {
                                        Text(
                                            cat,
                                            color          = if (cat == editCat) SubGold else Color.White,
                                            fontFamily     = FontFamily.Monospace, fontSize = 13.sp
                                        )
                                    },
                                    onClick = { editCat = cat; catExpanded = false }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (amountErr) return@Button
                        if (editName.isBlank()) {
                            Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onUpdate(
                            subscription.copy(
                                subscriptionName   = editName.trim(),
                                subscriptionAmount = editAmount.trim(),
                                category           = editCat
                            ),
                            context
                        )
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SubGold),
                    shape  = RoundedCornerShape(8.dp)
                ) {
                    Text("Save", color = SubDarkPurple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditDialog = false },
                    border  = BorderStroke(1.dp, Color(0xFF4A3F6B)),
                    colors  = ButtonDefaults.outlinedButtonColors(containerColor = SubDarkPurple, contentColor = SubGold),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Cancel", fontFamily = FontFamily.Monospace) }
            }
        )
    }

    // ── Delete Confirm Dialog ─────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest  = { showDeleteDialog = false },
            containerColor    = SubCardBg,
            titleContentColor = SubGold,
            title = {
                Text("Delete Subscription?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to delete \"${subscription.subscriptionName}\"? This cannot be undone.",
                    color = SubMuted, fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                Button(
                    onClick = { onDelete(context); showDeleteDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = SubCrimson),
                    shape   = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", color = SubGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    border  = BorderStroke(1.dp, Color(0xFF4A3F6B)),
                    colors  = ButtonDefaults.outlinedButtonColors(containerColor = SubDarkPurple, contentColor = SubGold),
                    shape   = RoundedCornerShape(8.dp)
                ) { Text("Cancel", fontFamily = FontFamily.Monospace) }
            }
        )
    }
}

@Composable
private fun SubDetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SubMuted,    fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ViewSubscriptionsScreenPreview() { ViewSubscriptionsScreen(rememberNavController()) }
