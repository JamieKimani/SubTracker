package com.example.trackifyv1.ui.theme.screens.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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


private val Gold       = Color(0xFFD4A017)
private val Crimson    = Color(0xFF8B0000)
private val DarkPurple = Color(0xFF1A0533)
private val DarkGreen  = Color(0xFF0D2B1A)
private val DarkYellow = Color(0xFF1A1A00)
private val CardBg     = Color(0xFF1C1C1C)
private val Muted      = Color(0xFF9E9E9E)
private val BorderIdle = Color(0xFF4A3F6B)


private val categoryColors = listOf(
    Color(0xFFD4A017), Color(0xFF8B0000), Color(0xFF00BCD4),
    Color(0xFF7C4DFF), Color(0xFF00E676), Color(0xFFFF6D00),
    Color(0xFFE91E63), Color(0xFF1DE9B6)
)

private fun categoryColor(category: String): Color {
    val index = subscriptionCategories.indexOf(category).takeIf { it >= 0 }
        ?: (category.hashCode() and 0x7FFFFFFF)
    return categoryColors[index % categoryColors.size]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewSubscriptionsScreen(navController: NavController) {
    val viewModel: SubscriptionViewModel = viewModel()
    val subscriptions by viewModel.subscriptions.collectAsState()

    val gradientBg = Brush.verticalGradient(
        colors = listOf(DarkPurple, DarkGreen, DarkYellow)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Subscriptions",
                        color = Gold,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Gold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkPurple)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(ROUTE_ADD_SUBSCRIPTION) },
                containerColor = Crimson,
                contentColor = Gold
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(padding)
        ) {
            if (subscriptions.isEmpty()) {

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📭", fontSize = 48.sp)
                    Text(
                        "No subscriptions yet",
                        color = Muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                    Text(
                        "Tap + to add your first one",
                        color = BorderIdle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(subscriptions, key = { it.id }) { subscription ->
                        SubscriptionCard(
                            subscription = subscription,
                            onDelete = { viewModel.deleteSubscription(subscription.id) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SubscriptionCard(
    subscription: SubscriptionModel,
    onDelete: () -> Unit
) {
    val chipColor = categoryColor(subscription.category.ifBlank { "Other" })

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subscription.subscriptionName.ifBlank { "Unnamed" },
                    color = Gold,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Crimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }


            if (subscription.category.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(chipColor.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        subscription.category,
                        color = chipColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Divider(color = BorderIdle.copy(alpha = 0.4f), thickness = 0.5.dp)


            DetailRow("Amount",       "KES ${subscription.subscriptionAmount}")
            DetailRow("Start Date",   subscription.subscriptionDate.ifBlank { "—" })
            DetailRow("Expiry",       subscription.expiryDate.ifBlank { "—" })
            DetailRow("Next Renewal", subscription.nextRenewalDate.ifBlank { "—" })
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(value, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ViewSubscriptionsScreenPreview() {
    ViewSubscriptionsScreen(rememberNavController())
}