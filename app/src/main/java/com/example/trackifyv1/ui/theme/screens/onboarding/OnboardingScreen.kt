package com.example.trackifyv1.ui.theme.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trackifyv1.ui.theme.*

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

private val pages = listOf(
    OnboardingPage("📋", "Track every subscription",
        "Add Netflix, Spotify, gym memberships or any recurring bill. Trackify keeps them all in one place so nothing catches you off guard.",
        Gold),
    OnboardingPage("⏰", "Never miss a renewal",
        "Set reminders before each renewal date. Get notified when free trials are ending so you can cancel before being charged.",
        TealAccent),
    OnboardingPage("📊", "See where your money goes",
        "Monthly and yearly totals, category breakdowns, and 6-month spending trends — all on your dashboard at a glance.",
        Color(0xFFFF6D00))
)

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    Box(
        Modifier.fillMaxSize().background(AppGradient)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(top = 60.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState   = page,
                transitionSpec = {
                    (slideInHorizontally(tween(320)) { it / 2 } + fadeIn(tween(320)))
                        .togetherWith(slideOutHorizontally(tween(320)) { -it / 2 } + fadeOut(tween(160)))
                },
                label = "onboard"
            ) { idx ->
                val p = pages[idx]
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.size(120.dp).clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(
                                p.accentColor.copy(alpha = 0.25f),
                                p.accentColor.copy(alpha = 0.08f)
                            ))),
                        contentAlignment = Alignment.Center
                    ) { Text(p.emoji, fontSize = 56.sp) }

                    Spacer(Modifier.height(40.dp))

                    Text(p.title, color = p.accentColor, fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center, lineHeight = 32.sp)

                    Spacer(Modifier.height(16.dp))

                    Text(p.subtitle, color = Muted, fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center, lineHeight = 22.sp)
                }
            }

            Row(Modifier.padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 22.dp else 8.dp, 8.dp)
                            .clip(if (i == page) RoundedCornerShape(4.dp) else CircleShape)
                            .background(if (i == page) current.accentColor else BorderIdle)
                    )
                }
            }

            if (page < pages.lastIndex) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onFinish) {
                        Text("Skip", color = Muted, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    }
                    Button(
                        onClick = { page++ },
                        shape   = RoundedCornerShape(12.dp),
                        colors  = ButtonDefaults.buttonColors(containerColor = current.accentColor)
                    ) {
                        Text("Next →", color = Color.White, fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else {
                Button(
                    onClick   = onFinish,
                    modifier  = Modifier.fillMaxWidth().height(52.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = ButtonDefaults.buttonColors(containerColor = current.accentColor)
                ) {
                    Text("Get started", color = Color.White, fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
