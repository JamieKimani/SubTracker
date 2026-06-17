package com.example.trackifyv1.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SkeletonBase      = Color(0xFF1E1E2E)
private val SkeletonHighlight = Color(0xFF2E2E44)

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue   = 0f,
        targetValue    = 1000f,
        animationSpec  = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label          = "shimmerOffset"
    )
    return remember(progress) {
        Brush.linearGradient(
            colors = listOf(SkeletonBase, SkeletonHighlight, SkeletonBase),
            start  = Offset(progress - 300f, 0f),
            end    = Offset(progress, 0f)
        )
    }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp         = 16.dp,
    radius: Dp         = 8.dp
) {
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(shimmerBrush())
    )
}

@Composable
fun SkeletonStatRow() {
    val brush = shimmerBrush()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(4) {
            Box(
                Modifier.weight(1f).height(62.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
fun SkeletonCard(lineCount: Int = 3) {
    val brush = shimmerBrush()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SkeletonBase)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.width(120.dp).height(14.dp).clip(RoundedCornerShape(7.dp)).background(brush))
        repeat(lineCount) { i ->
            Box(
                Modifier
                    .fillMaxWidth(if (i == lineCount - 1) 0.6f else 1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(brush)
            )
        }
    }
}

@Composable
fun SkeletonSubscriptionCard() {
    val brush = shimmerBrush()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SkeletonBase)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.width(140.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(brush))
            Box(Modifier.width(60.dp).height(16.dp).clip(RoundedCornerShape(8.dp)).background(brush))
        }
        Box(Modifier.width(80.dp).height(10.dp).clip(RoundedCornerShape(5.dp)).background(brush))
        Spacer(Modifier.height(2.dp))
        repeat(3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.width(70.dp).height(9.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Box(Modifier.width(90.dp).height(9.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }
    }
}

@Composable
fun DashboardSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonBox(Modifier.width(120.dp), height = 18.dp)
                SkeletonBox(Modifier.width(180.dp), height = 12.dp)
            }
        }
        SkeletonStatRow()
        SkeletonCard(lineCount = 4)
        SkeletonCard(lineCount = 5)
    }
}

@Composable
fun SubscriptionListSkeleton() {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(5) { SkeletonSubscriptionCard() }
    }
}
