package com.example.trackifyv1.ui.theme.screens.subscriptions

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.models.SubscriptionModel
import com.example.trackifyv1.ui.theme.SubscriptionListSkeleton
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.navigation.ROUTE_ADD_SUBSCRIPTION
import com.example.trackifyv1.ui.theme.screens.dashboard.categoryColors
import com.example.trackifyv1.ui.theme.screens.dashboard.daysUntil
import android.view.HapticFeedbackConstants
import com.example.trackifyv1.ui.theme.ServiceIcon
import com.example.trackifyv1.ui.theme.screens.dashboard.RenewalConfirmDialog

private val SubGold        = Color(0xFFD4A017)
private val SubCrimson     = Color(0xFF8B0000)
private val SubDarkPurple  = Color(0xFF1A0533)
private val SubDarkGreen   = Color(0xFF0D2B1A)
private val SubDarkYellow  = Color(0xFF1A1A00)
private val SubCardBg      = Color(0xFF1C1C1C)
private val SubMuted       = Color(0xFF9E9E9E)
private val SubBorderIdle  = Color(0xFF4A3F6B)
private val SubTeal        = Color(0xFF00C8A0)

private enum class SortOrder { NAME, AMOUNT_HIGH, AMOUNT_LOW, EXPIRY, NEWEST }

private fun catColor(category: String): Color {
    val idx = subscriptionCategories.indexOf(category).takeIf { it >= 0 }
        ?: (category.hashCode() and 0x7FFFFFFF)
    return categoryColors[idx % categoryColors.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSubscriptionsScreen(navController: NavController, isStandalone: Boolean = true) {
    val vm: SubscriptionViewModel    = viewModel()
    val context                      = LocalContext.current
    val view                         = LocalView.current
    var renewalTarget by remember { mutableStateOf<SubscriptionModel?>(null) }
    val allSubs by vm.subscriptions.collectAsState()

    var query       by remember { mutableStateOf("") }
    var sortOrder   by remember { mutableStateOf(SortOrder.NEWEST) }
    var sortExpanded by remember { mutableStateOf(false) }
    var filterActive by remember { mutableStateOf<Boolean?>(null) }

    val displayed = allSubs
        .filter { sub ->
            (query.isBlank() || sub.subscriptionName.contains(query, ignoreCase = true) ||
                    sub.category.contains(query, ignoreCase = true))
                    && (filterActive == null || sub.isActive == filterActive)
        }
        .let { list ->
            when (sortOrder) {
                SortOrder.NAME         -> list.sortedBy { it.subscriptionName.lowercase() }
                SortOrder.AMOUNT_HIGH  -> list.sortedByDescending { it.subscriptionAmount.toDoubleOrNull() ?: 0.0 }
                SortOrder.AMOUNT_LOW   -> list.sortedBy { it.subscriptionAmount.toDoubleOrNull() ?: 0.0 }
                SortOrder.EXPIRY       -> list.sortedBy { daysUntil(it.expiryDate) ?: Int.MAX_VALUE }
                SortOrder.NEWEST       -> list
            }
        }

    val gradientBg = Brush.verticalGradient(listOf(SubDarkPurple, SubDarkGreen, SubDarkYellow))

    Scaffold(
        modifier = Modifier.statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("My Subscriptions", color = SubGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (isStandalone) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SubGold)
                        }
                    }
                },
                actions = {

                    Box {
                        IconButton(onClick = { sortExpanded = true }) {
                            Icon(Icons.Default.Sort, "Sort", tint = SubGold)
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false },
                            modifier = Modifier.background(SubCardBg)) {
                            listOf(
                                SortOrder.NEWEST      to "Newest first",
                                SortOrder.NAME        to "Name A–Z",
                                SortOrder.AMOUNT_HIGH to "Amount: high–low",
                                SortOrder.AMOUNT_LOW  to "Amount: low–high",
                                SortOrder.EXPIRY      to "Expiry: soonest"
                            ).forEach { (order, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = if (sortOrder == order) SubGold else Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                                    onClick = { sortOrder = order; sortExpanded = false },
                                    leadingIcon = { if (sortOrder == order) Icon(Icons.Default.Check, null, tint = SubGold, modifier = Modifier.size(16.dp)) }
                                )
                            }
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
                    onClick = { navController.navigate(ROUTE_ADD_SUBSCRIPTION) },
                    containerColor = SubCrimson, contentColor = SubGold
                ) { Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(Modifier.fillMaxSize().background(gradientBg).padding(padding)) {
            Column(Modifier.fillMaxSize()) {

                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search by name or category…", color = SubMuted, fontSize = 13.sp) },
                    leadingIcon  = { Icon(Icons.Default.Search, null, tint = SubMuted, modifier = Modifier.size(18.dp)) },
                    trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear", tint = SubMuted, modifier = Modifier.size(16.dp)) } },
                    singleLine   = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = SubGold,       unfocusedBorderColor   = SubBorderIdle,
                        focusedTextColor     = Color.White,   unfocusedTextColor     = Color.White,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        cursorColor          = SubGold
                    ),
                    shape    = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    Modifier.padding(horizontal = 16.dp).padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(null to "All", true to "Active", false to "Paused").forEach { (value, label) ->
                        FilterChip(
                            selected = filterActive == value,
                            onClick  = { filterActive = value },
                            label    = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor    = SubGold.copy(alpha = 0.18f),
                                selectedLabelColor        = SubGold,
                                containerColor            = Color.Transparent,
                                labelColor                = SubMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled          = true,
                                selected         = filterActive == value,
                                borderColor      = SubBorderIdle,
                                selectedBorderColor = SubGold
                            )
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${displayed.size} sub${if (displayed.size != 1) "s" else ""}",
                        color = SubMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterVertically))
                }

                if (displayed.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(if (query.isNotBlank()) "😕" else "📭", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(if (query.isNotBlank()) "No results for \"$query\"" else "No subscriptions yet",
                            color = SubMuted, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                        if (query.isBlank())
                            Text("Tap + to add your first one", color = SubBorderIdle, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                    ) {
                        items(items = displayed, key = { it.id }) { sub ->
                            SubscriptionCard(
                                subscription = sub,
                                onDelete     = { ctx -> view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); vm.deleteSubscription(sub.id, ctx) },
                                onUpdate     = { updated, ctx -> vm.updateSubscription(updated, ctx) },
                                onRenew      = { renewalTarget = sub },
                                onTogglePause = { view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); vm.toggleActive(sub, context) }
                            )
                        }
                    }
                }
            }
        }
    }

    renewalTarget?.let { sub ->
        RenewalConfirmDialog(
            sub       = sub,
            onDismiss = { renewalTarget = null },
            onConfirm = { newAmount ->
                vm.renewSubscription(sub, context, newAmount)
                renewalTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionCard(
    subscription: SubscriptionModel,
    onDelete: (Context) -> Unit,
    onUpdate: (SubscriptionModel, Context) -> Unit,
    onRenew: () -> Unit = {},
    onTogglePause: () -> Unit = {}
) {
    val context          = LocalContext.current
    val chipColor        = catColor(subscription.category.ifBlank { "Other" })
    var showEditDialog   by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val daysLeft         = daysUntil(subscription.expiryDate)
    val urgencyColor     = when {
        daysLeft != null && daysLeft <= 3  -> Color(0xFFE53935)
        daysLeft != null && daysLeft <= 7  -> Color(0xFFFF6D00)
        else                               -> null
    }

    Card(
        Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (!subscription.isActive) SubCardBg.copy(alpha = 0.5f) else SubCardBg.copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ServiceIcon(subscription.subscriptionName.ifBlank { "?" }, size = 36.dp)
                    Text(
                        subscription.subscriptionName.ifBlank { "Unnamed" },
                        color = if (subscription.isActive) SubGold else SubMuted,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!subscription.isActive) {
                        Box(
                            Modifier.clip(RoundedCornerShape(6.dp)).background(SubMuted.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) { Text("PAUSED", color = SubMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                    }
                }
                Row {
                    IconButton(onClick = onTogglePause, Modifier.size(32.dp)) {
                        Icon(
                            if (subscription.isActive) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            if (subscription.isActive) "Pause" else "Resume",
                            tint = if (subscription.isActive) SubMuted else SubTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = { showEditDialog = true },   Modifier.size(32.dp)) { Icon(Icons.Default.Edit,   "Edit",   tint = SubGold,   modifier = Modifier.size(18.dp)) }
                    IconButton(onClick = { showDeleteDialog = true }, Modifier.size(32.dp)) { Icon(Icons.Default.Delete, "Delete", tint = SubCrimson, modifier = Modifier.size(18.dp)) }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (subscription.category.isNotBlank()) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(chipColor.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(subscription.category, color = chipColor, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Box(Modifier.clip(RoundedCornerShape(20.dp)).background(SubBorderIdle.copy(alpha = 0.25f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text(subscription.billingCycle.ifBlank { "Monthly" }, color = SubMuted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                if (subscription.isTrial) {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFF6D00).copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text("TRIAL", color = Color(0xFFFF6D00), fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = SubBorderIdle.copy(alpha = 0.4f), thickness = 0.5.dp)
            SubDetailRow("Amount",   "KES ${subscription.subscriptionAmount}")
            SubDetailRow("Start",    subscription.subscriptionDate.ifBlank { "—" })
            SubDetailRow("Expiry",   subscription.expiryDate.ifBlank { "—" },
                valueColor = urgencyColor ?: Color.White)
            SubDetailRow("Reminder", subscription.reminderDate.ifBlank { "—" })
            if (subscription.isTrial && subscription.trialEndDate.isNotBlank()) {
                SubDetailRow("Trial ends", subscription.trialEndDate, valueColor = Color(0xFFFF6D00))
            }

            val trialDaysLeft = if (subscription.isTrial) daysUntil(subscription.trialEndDate) else null
            AnimatedVisibility(visible = trialDaysLeft != null && trialDaysLeft in 0..7, enter = expandVertically(), exit = shrinkVertically()) {
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFFFF6D00).copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        when (trialDaysLeft) {
                            0    -> "⏳ Trial ends today!"
                            1    -> "⏳ Trial ends tomorrow"
                            else -> "⏳ Trial ends in $trialDaysLeft days"
                        },
                        color = Color(0xFFFF6D00), fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = urgencyColor != null, enter = expandVertically(), exit = shrinkVertically()) {
                Row(
                    Modifier.fillMaxWidth().background(urgencyColor?.copy(alpha = 0.1f) ?: Color.Transparent, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        when (daysLeft) {
                            0    -> "⚠ Renews today!"
                            1    -> "⚠ Renews tomorrow"
                            else -> "⚠ Renews in $daysLeft days"
                        },
                        color = urgencyColor ?: SubGold, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onRenew, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(28.dp)) {
                        Text("Mark renewed ✓", color = SubTeal, fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var editName    by remember { mutableStateOf(subscription.subscriptionName) }
        var editAmount  by remember { mutableStateOf(subscription.subscriptionAmount) }
        var editCat     by remember { mutableStateOf(subscription.category) }
        var editCycle   by remember { mutableStateOf(subscription.billingCycle.ifBlank { "Monthly" }) }
        var editIsTrial by remember { mutableStateOf(subscription.isTrial) }
        var editTrialEnd by remember { mutableStateOf(subscription.trialEndDate) }
        var amountErr   by remember { mutableStateOf(false) }
        var catExpanded by remember { mutableStateOf(false) }
        var cycleExpanded by remember { mutableStateOf(false) }

        val fColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SubGold, unfocusedBorderColor = SubBorderIdle,
            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            cursorColor = SubGold, focusedLabelColor = SubGold, unfocusedLabelColor = SubMuted
        )
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = SubCardBg, titleContentColor = SubGold,
            title = { Text("Edit Subscription", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Name") }, singleLine = true, colors = fColors, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it; amountErr = it.isNotBlank() && it.toDoubleOrNull() == null },
                        label = { Text("Amount (KES)") }, singleLine = true, isError = amountErr,
                        supportingText = { if (amountErr) Text("Enter a valid number", color = Color(0xFFFF6D6D)) },
                        colors = fColors, modifier = Modifier.fillMaxWidth()
                    )
                    ExposedDropdownMenuBox(expanded = cycleExpanded, onExpandedChange = { cycleExpanded = !cycleExpanded }) {
                        OutlinedTextField(value = editCycle, onValueChange = {}, readOnly = true, label = { Text("Billing Cycle") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = SubGold) }, colors = fColors,
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true))
                        ExposedDropdownMenu(expanded = cycleExpanded, onDismissRequest = { cycleExpanded = false }, modifier = Modifier.background(SubCardBg)) {
                            billingCycles.forEach { cycle ->
                                DropdownMenuItem(
                                    text = { Text(cycle, color = if (cycle == editCycle) SubGold else Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                                    onClick = { editCycle = cycle; cycleExpanded = false }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                        OutlinedTextField(value = editCat, onValueChange = {}, readOnly = true, label = { Text("Category") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = SubGold) }, colors = fColors,
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true))
                        ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }, modifier = Modifier.background(SubCardBg)) {
                            subscriptionCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = if (cat == editCat) SubGold else Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                                    onClick = { editCat = cat; catExpanded = false }
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Free Trial", color = SubGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Switch(checked = editIsTrial, onCheckedChange = { editIsTrial = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = SubGold, checkedTrackColor = SubGold.copy(alpha = 0.4f)))
                    }
                    if (editIsTrial) {
                        val ctx2 = context
                        val cal2 = java.util.Calendar.getInstance()
                        val trialPicker = remember {
                            android.app.DatePickerDialog(
                                ctx2, { _, y, m, d -> editTrialEnd = "%02d/%02d/%04d".format(d, m + 1, y) },
                                cal2.get(java.util.Calendar.YEAR), cal2.get(java.util.Calendar.MONTH), cal2.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                        }
                        OutlinedTextField(
                            value = editTrialEnd, onValueChange = {}, readOnly = true, label = { Text("Trial Ends") },
                            placeholder = { Text("DD/MM/YYYY", color = SubMuted) },
                            trailingIcon = { IconButton(onClick = { trialPicker.show() }) { Icon(Icons.Default.DateRange, "Pick trial end date", tint = SubGold) } },
                            colors = fColors, modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (amountErr) return@Button
                    if (editName.isBlank()) { Toast.makeText(context, "Name cannot be empty.", Toast.LENGTH_SHORT).show(); return@Button }
                    onUpdate(subscription.copy(
                        subscriptionName = editName.trim(), subscriptionAmount = editAmount.trim(),
                        category = editCat, billingCycle = editCycle,
                        isTrial = editIsTrial, trialEndDate = if (editIsTrial) editTrialEnd else ""
                    ), context)
                    showEditDialog = false
                }, colors = ButtonDefaults.buttonColors(containerColor = SubGold), shape = RoundedCornerShape(8.dp)) {
                    Text("Save", color = SubDarkPurple, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }, border = BorderStroke(1.dp, SubBorderIdle),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SubDarkPurple, contentColor = SubGold), shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SubCardBg, titleContentColor = SubGold,
            title = { Text("Delete Subscription?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text  = { Text("Delete \"${subscription.subscriptionName}\"? This cannot be undone.", color = SubMuted, fontFamily = FontFamily.Monospace) },
            confirmButton = {
                Button(onClick = { onDelete(context); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SubCrimson), shape = RoundedCornerShape(8.dp)) {
                    Text("Delete", color = SubGold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }, border = BorderStroke(1.dp, SubBorderIdle),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = SubDarkPurple, contentColor = SubGold), shape = RoundedCornerShape(8.dp)) {
                    Text("Cancel", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun SubDetailRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = SubMuted,   fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = valueColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ViewSubscriptionsScreenPreview() { ViewSubscriptionsScreen(rememberNavController()) }
