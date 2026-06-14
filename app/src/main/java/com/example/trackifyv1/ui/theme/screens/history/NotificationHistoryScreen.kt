package com.example.trackifyv1.ui.theme.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trackifyv1.data.NotificationHistoryEntity
import com.example.trackifyv1.models.NotificationHistoryViewModel
import com.example.trackifyv1.notifications.NotificationHelper
import com.example.trackifyv1.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationHistoryScreen(navController: NavController) {
    val vm      = viewModel<NotificationHistoryViewModel>()
    val history by vm.history.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault()) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text("Notification History", color = Gold,
                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Gold)
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteForever, "Clear history", tint = Color(0xFFE53935))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().background(AppGradient).padding(padding)
        ) {
            if (history.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Notifications, null, tint = BorderIdle, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No notifications yet", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                    Text("Reminders you receive will appear here.", color = BorderIdle,
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(history, key = { it.id }) { entry ->
                        NotificationHistoryCard(entry, sdf)
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor   = CardBg, titleContentColor = Gold,
            title = { Text("Clear history?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text  = { Text("This will remove all ${history.size} notification records.", color = Muted,
                fontFamily = FontFamily.Monospace, fontSize = 13.sp) },
            confirmButton = {
                Button(onClick = { vm.clearHistory(); showClearDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape  = RoundedCornerShape(8.dp)) {
                    Text("Clear all", color = Color.White, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false },
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderIdle),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkPurple, contentColor = Gold),
                    shape  = RoundedCornerShape(8.dp)) {
                    Text("Cancel", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
private fun NotificationHistoryCard(entry: NotificationHistoryEntity, sdf: SimpleDateFormat) {
    val isTrial = entry.channel == NotificationHelper.CHANNEL_TRIALS
    val accent  = if (isTrial) Color(0xFFFF6D00) else TealAccent

    Column(
        Modifier.fillMaxWidth()
            .background(CardBg.copy(alpha = 0.85f), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                if (isTrial) Icons.Default.Warning else Icons.Default.Notifications,
                null, tint = accent, modifier = Modifier.size(14.dp)
            )
            Text(entry.title, color = accent, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
        Text(entry.message, color = Color.White.copy(alpha = 0.85f), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        Text(sdf.format(Date(entry.timestampMs)), color = Muted, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
    }
}
