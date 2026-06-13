package com.example.trackifyv1.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class AppPalette(
    val gold: Color,
    val crimson: Color,
    val background: Brush,
    val cardBg: Color,
    val navBg: Color,
    val muted: Color,
    val borderIdle: Color,
    val tealAccent: Color,
    val textPrimary: Color,
    val isDark: Boolean
)

val DarkAppPalette = AppPalette(
    gold        = Gold,
    crimson     = Crimson,
    background  = AppGradient,
    cardBg      = CardBg.copy(alpha = 0.85f),
    navBg       = NavBg,
    muted       = Muted,
    borderIdle  = BorderIdle,
    tealAccent  = TealAccent,
    textPrimary = Color.White,
    isDark      = true
)

private val LightBg       = Color(0xFFF5F1E8)
private val LightCardBg   = Color(0xFFFFFFFF)
private val LightMuted    = Color(0xFF7A7A7A)
private val LightBorder   = Color(0xFFD8CFC0)
private val LightNav      = Color(0xFFFFFFFF)
private val LightTextDark = Color(0xFF1A0533)

val LightAppPalette = AppPalette(
    gold        = Color(0xFFB8860B),
    crimson     = Crimson,
    background  = Brush.verticalGradient(listOf(LightBg, LightBg, Color(0xFFEDE6D8))),
    cardBg      = LightCardBg.copy(alpha = 0.95f),
    navBg       = LightNav,
    muted       = LightMuted,
    borderIdle  = LightBorder,
    tealAccent  = Color(0xFF00917A),
    textPrimary = LightTextDark,
    isDark      = false
)

val LocalAppPalette = staticCompositionLocalOf { DarkAppPalette }
