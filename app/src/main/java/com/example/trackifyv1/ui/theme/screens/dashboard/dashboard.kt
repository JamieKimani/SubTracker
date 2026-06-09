package com.example.trackifyv1.ui.theme.screens.dashboard

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
import androidx.compose.ui.platform.LocalContext
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
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.navigation.ROUTE_ADD_SUBSCRIPTION
import com.example.trackifyv1.ui.theme.AppGradient
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

    Box(Modifier.fillMaxSize().background(AppGradient)) {
        Box(Modifier.fillMaxSize().padding(bottom = 90.dp)) {
            when (tab) {
                0 -> DashboardTab(
                    navController      = navController,
                    onViewSubscriptions = { tab = 1 },
                    onStatCardTap      = { sheetFilter = it }
                )
                1 -> ViewSubscriptionsScreen(navController, isStandalone = false)
                2 -> ProfileScreen(navController)
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

    val title = when (filter) {
        SheetFilter.MONTHLY -> "Monthly Subscriptions"
        SheetFilter.YEARLY  -> "Yearly Cost Breakdown"
        SheetFilter.ACTIVE  -> "Active Subscriptions"
        SheetFilter.PAUSED  -> "Paused Subscriptions"
        SheetFilter.NONE    -> ""
    }

    val subs = when (filter) {
        SheetFilter.MONTHLY, SheetFilter.YEARLY -> allSubs.filter { it.isActive }
        SheetFilter.ACTIVE  -> allSubs.filter { it.isActive }
        SheetFilter.PAUSED  -> allSubs.filter { !it.isActive }
        SheetFilter.NONE    -> emptyList()
    }

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

            if (subs.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), Alignment.Center) {
                    Text("No subscriptions here yet.", color = Muted, fontFamily = FontFamily.Monospace)
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
                                onRenew       = { vm.renewSubscription(sub, context) },
                                onTogglePause = { vm.toggleActive(sub, context) }
                            )
                        }
                    }
                }
            }
        }
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
        Box(
            Modifier
                .fillMaxWidth()
                .offset(x = offsetX.coerceAtLeast(threshold).dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < threshold) {
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
    val vm      = viewModel<SubscriptionViewModel>()
    val context = LocalContext.current
    val subs    by vm.subscriptions.collectAsState()
    val activeSubs = subs.filter { it.isActive }

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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TappableSummaryCard(Modifier.weight(1f), "Active",   "${activeSubs.size}",                    Gold,               { onStatCardTap(SheetFilter.ACTIVE) })
            TappableSummaryCard(Modifier.weight(1f), "Monthly",  "KES ${"%.0f".format(monthlyTotal)}",   TealAccent,         { onStatCardTap(SheetFilter.MONTHLY) })
            TappableSummaryCard(Modifier.weight(1f), "Yearly",   "KES ${"%.0f".format(annualTotal)}",    Color(0xFFFF6D00),  { onStatCardTap(SheetFilter.YEARLY) })
            TappableSummaryCard(Modifier.weight(1f), "Paused",   "${subs.size - activeSubs.size}",       Muted,              { onStatCardTap(SheetFilter.PAUSED) })
        }

        if (upcoming.isNotEmpty()) {
            SectionCard("⏰  Renewing soon") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upcoming.take(5).forEach { (sub, days) ->
                        UpcomingRenewalRow(sub, days, onRenew = { vm.renewSubscription(sub, context) })
                    }
                    if (upcoming.size > 5) {
                        Text("+${upcoming.size - 5} more", color = Muted,
                            fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                            modifier = Modifier.align(Alignment.End))
                    }
                }
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
                        CategoryBar(cat, count, amounts[cat] ?: 0.0,
                            if (total > 0) ((amounts[cat] ?: 0.0) / total).toFloat() else 0f,
                            categoryColors[i % categoryColors.size])
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
fun CategoryBar(category: String, count: Int, amount: Double, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text("KES %.2f  ($count)".format(amount), color = color,
                fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(BorderIdle)) {
            Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() { DashboardScreen(rememberNavController()) }
