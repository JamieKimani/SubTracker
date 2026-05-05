package com.example.trackifyv1.ui.theme.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trackifyv1.models.SubscriptionViewModel
import com.example.trackifyv1.navigation.ROUTE_ADD_SUBSCRIPTION
import com.example.trackifyv1.navigation.ROUTE_PROFILE
import com.example.trackifyv1.navigation.ROUTE_VIEW_SUBSCRIPTIONS

// ── Brand colors (internal to file; shared via import in other files) ─────────
val Gold       = Color(0xFFD4A017)
val Crimson    = Color(0xFF8B0000)
val DarkPurple = Color(0xFF1A0533)
val DarkGreen  = Color(0xFF0D2B1A)
val DarkYellow = Color(0xFF1A1A00)
val CardBg     = Color(0xFF1C1C1C)
val Muted      = Color(0xFF9E9E9E)
val BorderIdle = Color(0xFF4A3F6B)

val categoryColors = listOf(
    Color(0xFFD4A017), Color(0xFF8B0000), Color(0xFF00BCD4),
    Color(0xFF7C4DFF), Color(0xFF00E676), Color(0xFFFF6D00),
    Color(0xFFE91E63), Color(0xFF1DE9B6)
)

data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard",     Icons.Default.Dashboard,    "dashboard"),
    BottomNavItem("Subscriptions", Icons.Default.List,         ROUTE_VIEW_SUBSCRIPTIONS),
    BottomNavItem("Profile",       Icons.Default.Person,       ROUTE_PROFILE)
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val viewModel: SubscriptionViewModel = viewModel()
    val subscriptions by viewModel.subscriptions.collectAsState()

    val totalAmount     = subscriptions.sumOf { it.subscriptionAmount.toDoubleOrNull() ?: 0.0 }
    val categoryCounts  = subscriptions.groupBy { it.category.ifBlank { "Uncategorized" } }.mapValues { it.value.size }
    val categoryAmounts = subscriptions.groupBy { it.category.ifBlank { "Uncategorized" } }
        .mapValues { e -> e.value.sumOf { it.subscriptionAmount.toDoubleOrNull() ?: 0.0 } }

    Box(modifier = Modifier.fillMaxSize()
        .background(Brush.verticalGradient(listOf(DarkPurple, DarkGreen, DarkYellow)))) {

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Trackify", color = Gold, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
                )
            },
            bottomBar = { TrackifyBottomNav(navController, currentRoute = "dashboard") },
            floatingActionButton = {
                FloatingActionButton(onClick = { navController.navigate(ROUTE_ADD_SUBSCRIPTION) },
                    containerColor = Crimson, contentColor = Gold) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subscription")
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp)
                            .background(Brush.linearGradient(listOf(Gold, Crimson)), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text("TK", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                    Column {
                        Text("Dashboard", color = Gold, fontSize = 20.sp,
                            fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Your subscription overview", color = Muted,
                            fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(Modifier.weight(1f), "Total Subs", "${subscriptions.size}", Gold)
                    SummaryCard(Modifier.weight(1f), "Monthly", "KES %.2f".format(totalAmount), Color(0xFF00BCD4))
                }

                SectionCard("Spend by Category") {
                    if (categoryCounts.isEmpty()) {
                        Text("No subscriptions yet. Tap + to add one.", color = Muted,
                            fontFamily = FontFamily.Monospace, fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            categoryCounts.entries.forEachIndexed { i, (cat, count) ->
                                val amount   = categoryAmounts[cat] ?: 0.0
                                val fraction = if (totalAmount > 0) (amount / totalAmount).toFloat() else 0f
                                CategoryBar(cat, count, amount, fraction, categoryColors[i % categoryColors.size])
                            }
                        }
                    }
                }

                if (categoryCounts.isNotEmpty()) {
                    SectionCard("Category Share") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categoryCounts.entries.forEachIndexed { i, (cat, count) ->
                                val color = categoryColors[i % categoryColors.size]
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(color))
                                    Text(cat, color = Color.White, fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text("$count sub${if (count != 1) "s" else ""}", color = color,
                                        fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }


                Text("© 2026 Trackify", fontSize = 11.sp, color = BorderIdle,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.fillMaxWidth().wrapContentWidth())
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}


@Composable
fun TrackifyBottomNav(navController: NavController, currentRoute: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)),
            containerColor = DarkPurple,
            tonalElevation = 0.dp
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo("dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        }
                    },
                    icon = { Icon(item.icon, contentDescription = item.label,
                        tint = if (selected) Gold else Muted) },
                    label = { Text(item.label, color = if (selected) Gold else Muted,
                        fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = Gold,
                        unselectedIconColor = Muted,
                        indicatorColor      = Crimson.copy(alpha = 0.35f)
                    )
                )
            }
        }
    }
}


@Composable
fun SummaryCard(modifier: Modifier = Modifier, label: String, value: String, accent: Color) {
    Box(modifier = modifier.background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, color = Gold, fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        content()
    }
}

@Composable
fun CategoryBar(category: String, count: Int, amount: Double, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            Text("KES %.2f  ($count)".format(amount), color = color,
                fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(BorderIdle)) {
            Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight()
                .clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    DashboardScreen(rememberNavController())
}