package com.example.trackifyv1.ui.theme.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import com.example.trackifyv1.ui.theme.DashboardSkeleton
import com.example.trackifyv1.ui.theme.SubscriptionListSkeleton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.models.SubscriptionModel
import com.example.trackifyv1.models.BudgetViewModel
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.navigation.ROUTE_ADD_SUBSCRIPTION
import com.example.trackifyv1.ui.theme.AppGradient
import com.example.trackifyv1.ui.theme.ServiceIcon
import com.example.trackifyv1.ui.theme.LocalAppPalette
import com.example.trackifyv1.ui.theme.BorderIdle
import com.example.trackifyv1.ui.theme.CardBg
import com.example.trackifyv1.ui.theme.Crimson
import com.example.trackifyv1.ui.theme.DarkGreen
import com.example.trackifyv1.ui.theme.DarkPurple
import com.example.trackifyv1.ui.theme.DarkYellow
import com.example.trackifyv1.ui.theme.Gold
import com.example.trackifyv1.ui.theme.Muted
import com.example.trackifyv1.ui.theme.NavBg
import com.example.trackifyv1.ui.theme.TealAccent
import com.example.trackifyv1.ui.theme.screens.profile.ProfileScreen
import com.example.trackifyv1.ui.theme.screens.subscriptions.SubscriptionCard
import com.example.trackifyv1.ui.theme.screens.subscriptions.ViewSubscriptionsScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val categoryColors = listOf(
    Color(0xFFD4A017), Color(0xFF8B0000), Color(0xFF00BCD4), Color(0xFF7C4DFF),
    Color(0xFF00E676), Color(0xFFFF6D00), Color(0xFFE91E63), Color(0xFF1DE9B6)
)

fun monthlyAmount(sub: SubscriptionModel): Double {
    val amt = sub.subscriptionAmount.toDoubleOrNull() ?: 0.0
    return when (sub.billingCycle) {
        "Weekly"    -> amt * 4.33
        "Quarterly" -> amt / 3.0
        "Yearly"    -> amt / 12.0
        else        -> amt
    }
}

private data class NavItem(val label: String, val icon: ImageVector)
private val navItems = listOf(
    NavItem("Home",    Icons.Default.Dashboard),
    NavItem("Subs",    Icons.AutoMirrored.Filled.List),
    NavItem("Profile", Icons.Default.Person)
)

fun daysUntil(dateStr: String): Int? {
    if (dateStr.isBlank()) return null
    return try {
        val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return null
        val diff = date.time - System.currentTimeMillis()
        (diff / (1000 * 60 * 60 * 24)).toInt()
    } catch (_: Exception) { null }
}

enum class SheetFilter { MONTHLY, YEARLY, ACTIVE, PAUSED, NONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    var tab by remember { mutableStateOf(0) }
    var sheetFilter by remember { mutableStateOf(SheetFilter.NONE) }
    val sheetState  = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val palette = LocalAppPalette.current
    Box(Modifier.fillMaxSize().background(palette.background)) {
        Box(Modifier.fillMaxSize().padding(bottom = 90.dp)) {
            AnimatedContent(
                targetState   = tab,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(260)) + slideInVertically(tween(260)) { if (forward) 40 else -40 })
                        .togetherWith(fadeOut(tween(160)))
                },
                label = "tabContent"
            ) { currentTab ->
                when (currentTab) {
                    0 -> DashboardTab(
                        navController       = navController,
                        onViewSubscriptions = { tab = 1 },
                        onStatCardTap       = { sheetFilter = it }
                    )
                    1 -> ViewSubscriptionsScreen(navController, isStandalone = false)
                    2 -> ProfileScreen(navController)
                }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .fillMaxWidth().height(62.dp)
                .shadow(12.dp, RoundedCornerShape(31.dp))
                .clip(RoundedCornerShape(31.dp))
                .background(NavBg)
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                NavPill(navItems[0], tab == 0, Modifier.weight(1f)) { tab = 0 }
                Box(Modifier.weight(1f), Alignment.Center) {
                    NavPill(navItems[1], tab == 1, Modifier.fillMaxWidth()) { tab = 1 }
                }
                NavPill(navItems[2], tab == 2, Modifier.weight(1f)) { tab = 2 }
            }
        }
        if (tab != 2) {
            FloatingActionButton(
                onClick   = { navController.navigate(ROUTE_ADD_SUBSCRIPTION) },
                modifier  = Modifier.align(Alignment.BottomCenter).padding(bottom = 44.dp),
                containerColor = Crimson, contentColor = Gold,
                shape     = RoundedCornerShape(14.dp),
                elevation = FloatingActionButtonDefaults.elevation(10.dp)
            ) { Icon(Icons.Default.Add, "Add Subscription", Modifier.size(20.dp)) }
        }
    }

    if (sheetFilter != SheetFilter.NONE) {
        SubscriptionDetailSheet(
            filter     = sheetFilter,
            sheetState = sheetState,
            onDismiss  = { sheetFilter = SheetFilter.NONE }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailSheet(
    filter: SheetFilter,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val vm      = viewModel<SubscriptionViewModel>()
    val context = LocalContext.current
    val allSubs by vm.subscriptions.collectAsState()
    var renewalTarget by remember { mutableStateOf<SubscriptionModel?>(null) }
    var sheetQuery    by remember { mutableStateOf("") }

    val title = when (filter) {
        SheetFilter.MONTHLY -> "Monthly Subscriptions"
        SheetFilter.YEARLY  -> "Yearly Cost Breakdown"
        SheetFilter.ACTIVE  -> "Active Subscriptions"
        SheetFilter.PAUSED  -> "Paused Subscriptions"
        SheetFilter.NONE    -> ""
    }

    val baseSubs = when (filter) {
        SheetFilter.MONTHLY, SheetFilter.YEARLY -> allSubs.filter { it.isActive }
        SheetFilter.ACTIVE  -> allSubs.filter { it.isActive }
        SheetFilter.PAUSED  -> allSubs.filter { !it.isActive }
        SheetFilter.NONE    -> emptyList()
    }
    val subs = if (sheetQuery.isBlank()) baseSubs
    else baseSubs.filter { it.subscriptionName.contains(sheetQuery, ignoreCase = true) ||
        it.category.contains(sheetQuery, ignoreCase = true) }

    val total = when (filter) {
        SheetFilter.MONTHLY -> subs.sumOf { monthlyAmount(it) }
        SheetFilter.YEARLY  -> subs.sumOf { monthlyAmount(it) * 12 }
        else                -> subs.sumOf { it.subscriptionAmount.toDoubleOrNull() ?: 0.0 }
    }

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = NavBg,
        dragHandle        = {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), Alignment.Center) {
                Box(Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(BorderIdle))
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(title, color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                IconButton(onClick = onDismiss, Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = Muted, modifier = Modifier.size(18.dp))
                }
            }

            Text(
                when (filter) {
                    SheetFilter.MONTHLY -> "Total: KES ${"%.2f".format(total)} / mo"
                    SheetFilter.YEARLY  -> "Total: KES ${"%.2f".format(total)} / yr"
                    else                -> "${subs.size} subscription${if (subs.size != 1) "s" else ""}"
                },
                color = TealAccent, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value           = sheetQuery,
                onValueChange   = { sheetQuery = it },
                placeholder     = { Text("Search subscriptions…", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
                singleLine      = true,
                leadingIcon     = { Icon(Icons.Default.Search, null, tint = Muted, modifier = Modifier.size(18.dp)) },
                trailingIcon    = if (sheetQuery.isNotBlank()) {{ IconButton(onClick = { sheetQuery = "" }) {
                    Icon(Icons.Default.Close, null, tint = Muted, modifier = Modifier.size(16.dp))
                }}} else null,
                modifier        = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors          = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gold, unfocusedBorderColor = BorderIdle,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    cursorColor = Gold
                ),
                shape = RoundedCornerShape(10.dp)
            )

            if (subs.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                    Text(if (sheetQuery.isNotBlank()) "No results for "$sheetQuery"" else "No subscriptions here yet.",
                        color = Muted, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding      = PaddingValues(bottom = 16.dp)
                ) {
                    items(items = subs, key = { it.id }) { sub ->
                        SwipeToDismissCard(
                            onDelete = { vm.deleteSubscription(sub.id, context) }
                        ) {
                            SubscriptionCard(
                                subscription  = sub,
                                onDelete      = { ctx -> vm.deleteSubscription(sub.id, ctx) },
                                onUpdate      = { updated, ctx -> vm.updateSubscription(updated, ctx) },
                                onRenew       = { renewalTarget = sub },
                                onTogglePause = { vm.toggleActive(sub, context) }
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

@Composable
fun SwipeToDismissCard(onDelete: () -> Unit, content: @Composable () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val threshold = -220f

    Box(Modifier.fillMaxWidth()) {
        if (offsetX < -60f) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(72.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Crimson),
                Alignment.Center
            ) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        val view = LocalView.current
        Box(
            Modifier
                .fillMaxWidth()
                .offset(x = offsetX.coerceAtLeast(threshold).dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < threshold) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                onDelete()
                            }
                            offsetX = 0f
                        },
                        onHorizontalDrag = { _, delta ->
                            offsetX = (offsetX + delta).coerceIn(threshold * 1.1f, 0f)
                        }
                    )
                }
        ) { content() }
    }
}

@Composable
private fun NavPill(item: NavItem, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) Gold.copy(alpha = 0.13f) else Color.Transparent), Alignment.Center) {
        IconButton(onClick, Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(item.icon, item.label, tint = if (selected) Gold else Muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(2.dp))
                Text(item.label, color = if (selected) Gold else Muted, fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun DashboardTab(
    navController: NavController,
    onViewSubscriptions: () -> Unit,
    onStatCardTap: (SheetFilter) -> Unit = {}
) {
    val vm        = viewModel<SubscriptionViewModel>()
    val budgetVm  = viewModel<BudgetViewModel>()
    val context   = LocalContext.current
    val subs      by vm.subscriptions.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val budgets   by budgetVm.budgets.collectAsState()
    val activeSubs = subs.filter { it.isActive }

    if (isLoading && subs.isEmpty()) {
        DashboardSkeleton()
        return
    }

    val monthlyTotal = activeSubs.sumOf { monthlyAmount(it) }
    val annualTotal  = monthlyTotal * 12
    val amounts      = activeSubs.groupBy { it.category.ifBlank { "Uncategorized" } }
        .mapValues { e -> e.value.sumOf { it.subscriptionAmount.toDoubleOrNull() ?: 0.0 } }
    val counts       = activeSubs.groupBy { it.category.ifBlank { "Uncategorized" } }.mapValues { it.value.size }

    val upcoming = activeSubs
        .mapNotNull { sub ->
            val days = daysUntil(sub.expiryDate) ?: return@mapNotNull null
            if (days in 0..30) Pair(sub, days) else null
        }
        .sortedBy { it.second }

    var renewalTarget by remember { mutableStateOf<SubscriptionModel?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(56.dp).background(Brush.linearGradient(listOf(Gold, Crimson)), RoundedCornerShape(12.dp)),
                Alignment.Center
            ) { Text("TK", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Column {
                Text("Dashboard", color = Gold, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Your subscription overview", color = Muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        AnimatedVisibility(
            visible       = true,
            enter         = fadeIn(tween(400)) + slideInVertically(tween(400)) { 30 }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TappableSummaryCard(Modifier.weight(1f), "Active",   "${activeSubs.size}",                    Gold,               { onStatCardTap(SheetFilter.ACTIVE) })
                TappableSummaryCard(Modifier.weight(1f), "Monthly",  "KES ${"%.0f".format(monthlyTotal)}",   TealAccent,         { onStatCardTap(SheetFilter.MONTHLY) })
                TappableSummaryCard(Modifier.weight(1f), "Yearly",   "KES ${"%.0f".format(annualTotal)}",    Color(0xFFFF6D00),  { onStatCardTap(SheetFilter.YEARLY) })
                TappableSummaryCard(Modifier.weight(1f), "Paused",   "${subs.size - activeSubs.size}",       Muted,              { onStatCardTap(SheetFilter.PAUSED) })
            }
        }

        if (upcoming.isNotEmpty()) {
            SectionCard("⏰  Renewing soon") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcoming.take(5).forEach { (sub, days) ->
                        UpcomingRenewalRow(sub, days, onRenew = { renewalTarget = sub })
                    }
                    if (upcoming.size > 5) {
                        Text("+${upcoming.size - 5} more", color = Muted,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.End))
                    }
                }
            }
        }

        if (activeSubs.isNotEmpty()) {
            SectionCard("Spending trend (6 months)") {
                SpendingTrendChart(activeSubs, monthlyTotal)
            }
        }

        SectionCard("Spend by category") {
            if (counts.isEmpty()) {
                Text("No subscriptions yet.\nTap + to add your first one.",
                    color = Muted, fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val total = amounts.values.sum()
                    counts.entries.forEachIndexed { i, (cat, count) ->
                        val spent  = amounts[cat] ?: 0.0
                        val budget = budgets[cat]
                        CategoryBar(cat, count, spent,
                            if (total > 0) (spent / total).toFloat() else 0f,
                            categoryColors[i % categoryColors.size],
                            budget = budget)
                    }
                }
            }
        }

        if (counts.isNotEmpty()) {
            SectionCard("Category share") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    counts.entries.forEachIndexed { i, (cat, count) ->
                        val color = categoryColors[i % categoryColors.size]
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
                            Text(cat, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text("$count sub${if (count != 1) "s" else ""}", color = color,
                                fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        SectionCard("Quick actions") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick  = { navController.navigate(ROUTE_ADD_SUBSCRIPTION) },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Crimson)
                ) { Text("+ Add", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick  = onViewSubscriptions, modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, BorderIdle),
                    colors   = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold)
                ) { Text("View All", fontFamily = FontFamily.Monospace) }
            }
        }

        Text("© 2026 Trackify", fontSize = 11.sp, color = BorderIdle,
            fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().wrapContentWidth())
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

@Composable
fun TappableSummaryCard(modifier: Modifier, label: String, value: String, accent: Color, onClick: () -> Unit) {
    Card(
        onClick    = onClick,
        modifier   = modifier,
        shape      = RoundedCornerShape(12.dp),
        colors     = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.85f)),
        elevation  = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ChevronRight, null, tint = accent.copy(alpha = 0.45f), modifier = Modifier.size(10.dp).align(Alignment.End))
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, label: String, value: String, accent: Color) {
    Box(modifier.background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun RenewalConfirmDialog(
    sub: SubscriptionModel,
    onDismiss: () -> Unit,
    onConfirm: (newAmount: String?) -> Unit
) {
    var amount by remember { mutableStateOf(sub.subscriptionAmount) }
    val changed = amount.trim() != sub.subscriptionAmount.trim() && amount.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor    = CardBg,
        titleContentColor = Gold,
        title = {
            Text("Renew \"${sub.subscriptionName}\"?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Confirm the amount for this ${sub.billingCycle.lowercase()} cycle. If the price changed, update it below.",
                    color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (KES)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold, unfocusedBorderColor = BorderIdle,
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        cursorColor = Gold, focusedLabelColor = Gold, unfocusedLabelColor = Muted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (changed) {
                    val old = sub.subscriptionAmount.toDoubleOrNull() ?: 0.0
                    val new = amount.toDoubleOrNull() ?: 0.0
                    val arrow = if (new > old) "↑ increased" else "↓ decreased"
                    val color = if (new > old) Color(0xFFE53935) else TealAccent
                    Text(
                        "Price $arrow from KES ${sub.subscriptionAmount} to KES ${amount.trim()}",
                        color = color, fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(if (changed) amount.trim() else null) },
                colors = ButtonDefaults.buttonColors(containerColor = Crimson),
                shape  = RoundedCornerShape(8.dp)
            ) { Text("Confirm Renewal", color = Gold, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                border  = BorderStroke(1.dp, BorderIdle),
                colors  = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                shape   = RoundedCornerShape(8.dp)
            ) { Text("Cancel", fontFamily = FontFamily.Monospace) }
        }
    )
}

@Composable
fun UpcomingRenewalRow(sub: SubscriptionModel, daysLeft: Int, onRenew: () -> Unit) {
    val urgencyColor = when {
        daysLeft <= 3 -> Color(0xFFE53935)
        daysLeft <= 7 -> Color(0xFFFF6D00)
        else          -> TealAccent
    }
    val label = when (daysLeft) {
        0    -> "Today!"
        1    -> "Tomorrow"
        else -> "In $daysLeft days"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(urgencyColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        ServiceIcon(sub.subscriptionName, size = 34.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(sub.subscriptionName, color = Color.White, fontFamily = FontFamily.Monospace,
                fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("KES ${sub.subscriptionAmount}  •  ${sub.billingCycle}",
                color = Muted, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(label, color = urgencyColor, fontFamily = FontFamily.Monospace,
                fontSize = 11.sp, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = onRenew,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("Mark renewed", color = TealAccent, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = Gold, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        content()
    }
}

@Composable
fun SpendingTrendChart(activeSubs: List<SubscriptionModel>, currentMonthlyTotal: Double) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val monthFmt = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    data class MonthBucket(val label: String, val endOfMonthMillis: Long)

    val monthBuckets: List<MonthBucket> = remember {
        val list = mutableListOf<MonthBucket>()
        for (offset in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -offset)
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            list.add(MonthBucket(monthFmt.format(cal.time), cal.timeInMillis))
        }
        list
    }

    val values: List<Double> = remember(activeSubs) {
        monthBuckets.map { bucket ->
            activeSubs.sumOf { sub ->
                val started = try {
                    val d = sdf.parse(sub.subscriptionDate)
                    d == null || d.time <= bucket.endOfMonthMillis
                } catch (_: Exception) { true }
                if (started) monthlyAmount(sub) else 0.0
            }
        }
    }

    val maxVal = (values.maxOrNull() ?: 0.0).coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(110.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.Bottom
        ) {
            values.forEachIndexed { i, value ->
                val fraction = (value / maxVal).toFloat().coerceIn(0.04f, 1f)
                val isLast   = i == values.lastIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(
                        if (value >= 1000) "%.1fk".format(value / 1000) else "%.0f".format(value),
                        color = if (isLast) TealAccent else Muted, fontSize = 9.sp, fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(22.dp)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (isLast) TealAccent else BorderIdle.copy(alpha = 0.5f))
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            monthBuckets.forEachIndexed { i, bucket ->
                Text(bucket.label, color = if (i == monthBuckets.lastIndex) TealAccent else Muted,
                    fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Text(
            "Projected monthly cost based on active subscriptions and their start dates.",
            color = Muted, fontSize = 10.sp, fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CategoryBar(category: String, count: Int, amount: Double, fraction: Float, color: Color, budget: Double? = null) {
    val overBudget = budget != null && amount > budget
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(category, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                if (overBudget) {
                    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFE53935).copy(alpha = 0.18f)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                        Text("OVER", color = Color(0xFFE53935), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("KES %.2f  ($count)".format(amount), color = if (overBudget) Color(0xFFE53935) else color,
                fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(BorderIdle)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp))
                .background(if (overBudget) Color(0xFFE53935) else color))
        }
        if (budget != null) {
            val budgetFraction = (amount / budget).toFloat().coerceIn(0f, 1f)
            Text(
                if (overBudget) "KES ${"%.0f".format(amount - budget)} over budget (limit: KES ${"%.0f".format(budget)}/mo)"
                else "KES ${"%.0f".format(budget - amount)} remaining of KES ${"%.0f".format(budget)}/mo budget",
                color = if (overBudget) Color(0xFFE53935) else Muted,
                fontFamily = FontFamily.Monospace, fontSize = 10.sp
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() { DashboardScreen(rememberNavController()) }
