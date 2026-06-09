package com.example.trackifyv1.ui.theme.screens.splash

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackifyv1.ui.theme.AppGradient
import com.example.trackifyv1.ui.theme.BorderIdle
import com.example.trackifyv1.ui.theme.Crimson
import com.example.trackifyv1.ui.theme.Gold
import com.example.trackifyv1.ui.theme.TealAccent
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onLoadingComplete: () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue  = 1f,
        animationSpec = tween(durationMillis = 700, easing = EaseOut),
        label        = "fadeIn"
    )

    Box(
        modifier          = Modifier.fillMaxSize().background(AppGradient).statusBarsPadding(),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            modifier            = Modifier.graphicsLayer { this.alpha = alpha }.offset(y = ((1f - alpha) * 24).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier         = Modifier.size(88.dp).clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Gold, Crimson))),
                contentAlignment = Alignment.Center
            ) {
                Text("TK", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace)
            }

            Spacer(Modifier.height(28.dp))

            Text("Trackify", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)

            Spacer(Modifier.height(6.dp))

            Text("YOUR BILLS, ORGANIZED", color = Muted.copy(alpha = 0.7f), fontSize = 11.sp,
                fontFamily = FontFamily.Monospace, letterSpacing = 3.sp, fontWeight = FontWeight.Light)

            Spacer(Modifier.height(52.dp))

            CircularProgressIndicator(
                modifier   = Modifier.size(36.dp),
                color      = TealAccent,
                trackColor = BorderIdle.copy(alpha = 0.25f),
                strokeWidth = 2.5.dp,
                strokeCap  = StrokeCap.Round
            )
        }

        Text("v1.0.0", color = BorderIdle.copy(alpha = 0.5f), fontSize = 11.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp))
    }

    LaunchedEffect(Unit) {
        delay(2500)
        onLoadingComplete()
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() { SplashScreen {} }

private val Muted = Color(0xFF9E9E9E)
