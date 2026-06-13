package com.example.trackifyv1.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ServiceBrand(
    val label: String,
    val bg: Color,
    val textColor: Color = Color.White,
    val gradient: List<Color>? = null
)

private val brands: Map<String, ServiceBrand> = mapOf(
    "netflix"      to ServiceBrand("N",   Color(0xFFE50914)),
    "spotify"      to ServiceBrand("S",   Color(0xFF1DB954)),
    "youtube"      to ServiceBrand("▶",  Color(0xFFFF0000)),
    "disney"       to ServiceBrand("D+",  Color(0xFF113CCF)),
    "amazon"       to ServiceBrand("a",   Color(0xFF232F3E), gradient = listOf(Color(0xFF232F3E), Color(0xFF131921))),
    "prime"        to ServiceBrand("P",   Color(0xFF00A8E1)),
    "hbo"          to ServiceBrand("HBO", Color(0xFF9B0CB6)),
    "hulu"         to ServiceBrand("h",   Color(0xFF3DBB3D)),
    "apple tv"     to ServiceBrand("✦",  Color(0xFF000000)),
    "apple"        to ServiceBrand("",   Color(0xFF555555)),
    "icloud"       to ServiceBrand("☁",  Color(0xFF3478F6)),
    "google"       to ServiceBrand("G",   Color(0xFF4285F4), gradient = listOf(Color(0xFF4285F4), Color(0xFF34A853))),
    "gmail"        to ServiceBrand("M",   Color(0xFFEA4335)),
    "google drive" to ServiceBrand("▲",  Color(0xFF0F9D58)),
    "drive"        to ServiceBrand("▲",  Color(0xFF0F9D58)),
    "dropbox"      to ServiceBrand("⬡",  Color(0xFF0061FF)),
    "microsoft"    to ServiceBrand("⊞",  Color(0xFF00A4EF)),
    "office"       to ServiceBrand("O",   Color(0xFFD83B01)),
    "word"         to ServiceBrand("W",   Color(0xFF2B579A)),
    "excel"        to ServiceBrand("X",   Color(0xFF217346)),
    "notion"       to ServiceBrand("N",   Color(0xFF191919)),
    "slack"        to ServiceBrand("S",   Color(0xFF4A154B), gradient = listOf(Color(0xFF4A154B), Color(0xFF36C5F0))),
    "zoom"         to ServiceBrand("Z",   Color(0xFF2D8CFF)),
    "github"       to ServiceBrand("⎇",  Color(0xFF24292E)),
    "figma"        to ServiceBrand("F",   Color(0xFFF24E1E), gradient = listOf(Color(0xFFF24E1E), Color(0xFFFF7262))),
    "canva"        to ServiceBrand("C",   Color(0xFF00C4CC)),
    "adobe"        to ServiceBrand("Ai",  Color(0xFFFF7C00)),
    "photoshop"    to ServiceBrand("Ps",  Color(0xFF31A8FF)),
    "illustrator"  to ServiceBrand("Ai",  Color(0xFFFF9A00)),
    "playstation"  to ServiceBrand("PS",  Color(0xFF003791)),
    "xbox"         to ServiceBrand("X",   Color(0xFF107C10)),
    "steam"        to ServiceBrand("S",   Color(0xFF1B2838), gradient = listOf(Color(0xFF1B2838), Color(0xFF2A475E))),
    "efootball"    to ServiceBrand("eF",  Color(0xFF00ADEE)),
    "fc mobile"    to ServiceBrand("FC",  Color(0xFF00853E)),
    "ea sports"    to ServiceBrand("EA",  Color(0xFFFF4500)),
    "fifa"         to ServiceBrand("FC",  Color(0xFF00853E)),
    "tinder"       to ServiceBrand("♥",  Color(0xFFFD5068), gradient = listOf(Color(0xFFFF6B6B), Color(0xFFFD5068))),
    "linkedin"     to ServiceBrand("in",  Color(0xFF0A66C2)),
    "twitter"      to ServiceBrand("𝕏",  Color(0xFF000000)),
    "x premium"    to ServiceBrand("𝕏",  Color(0xFF000000)),
    "vpn"          to ServiceBrand("VPN", Color(0xFF00C8A0)),
    "nordvpn"      to ServiceBrand("N",   Color(0xFF4687FF)),
    "expressvpn"   to ServiceBrand("E",   Color(0xFFDA3940)),
    "gym"          to ServiceBrand("💪",  Color(0xFFFF6D00)),
    "headspace"    to ServiceBrand("hs",  Color(0xFFFF8A00)),
    "calm"         to ServiceBrand("C",   Color(0xFF4B7BEC)),
    "duolingo"     to ServiceBrand("🦉",  Color(0xFF58CC02)),
    "audible"      to ServiceBrand("A",   Color(0xFFF8991C)),
    "kindle"       to ServiceBrand("K",   Color(0xFF1A1A1A)),
    "patreon"      to ServiceBrand("P",   Color(0xFFFF424D)),
    "twitch"       to ServiceBrand("t",   Color(0xFF9146FF)),
    "discord"      to ServiceBrand("D",   Color(0xFF5865F2)),
    "chatgpt"      to ServiceBrand("⬡",  Color(0xFF74AA9C)),
    "openai"       to ServiceBrand("⬡",  Color(0xFF000000)),
    "claude"       to ServiceBrand("C",   Color(0xFFD97757)),
    "anthropic"    to ServiceBrand("A",   Color(0xFFCC785C)),
    "midjourney"   to ServiceBrand("M",   Color(0xFF000000)),
    "showmax"      to ServiceBrand("S",   Color(0xFFE6001A)),
    "dstv"         to ServiceBrand("D",   Color(0xFF1B5FAE)),
    "internet"     to ServiceBrand("🌐",  Color(0xFF00C8A0)),
    "electricity"  to ServiceBrand("⚡",  Color(0xFFFFC107)),
    "insurance"    to ServiceBrand("🛡",  Color(0xFF607D8B)),
    "rent"         to ServiceBrand("🏠",  Color(0xFF8D6E63)),
    "phone"        to ServiceBrand("📱",  Color(0xFF00C8A0)),
    "bbc"          to ServiceBrand("BBC", Color(0xFFBB1919)),
    "paramount"    to ServiceBrand("P+",  Color(0xFF0064FF)),
    "peacock"      to ServiceBrand("⁋",  Color(0xFF000000), gradient = listOf(Color(0xFF0055A4), Color(0xFF00C0EF))),
    "crunchyroll"  to ServiceBrand("C",   Color(0xFFF47521)),
    "funimation"   to ServiceBrand("F",   Color(0xFF410099)),
    "neon"         to ServiceBrand("N",   Color(0xFF00FF9D), textColor = Color(0xFF000000))
)

private val fallbackColors = listOf(
    Color(0xFFD4A017), Color(0xFF8B0000), Color(0xFF00BCD4), Color(0xFF7C4DFF),
    Color(0xFF00E676), Color(0xFFFF6D00), Color(0xFFE91E63), Color(0xFF1DE9B6),
    Color(0xFF00C8A0), Color(0xFF3F51B5)
)

fun brandFor(subscriptionName: String): ServiceBrand {
    val key = subscriptionName.trim().lowercase()
    brands.entries.sortedByDescending { it.key.length }.forEach { (k, v) ->
        if (key.contains(k)) return v
    }
    val initial = subscriptionName.trim().take(2).uppercase().ifBlank { "?" }
    val colorIndex = (key.hashCode() and 0x7FFFFFFF) % fallbackColors.size
    return ServiceBrand(initial, fallbackColors[colorIndex])
}

@Composable
fun ServiceIcon(subscriptionName: String, size: Dp = 38.dp) {
    val brand    = brandFor(subscriptionName)
    val fontSize = if (brand.label.length >= 3) (size.value * 0.28f).sp
                   else if (brand.label.length == 2) (size.value * 0.34f).sp
                   else (size.value * 0.42f).sp
    val radius   = (size.value * 0.26f).dp

    val bgMod = if (brand.gradient != null)
        Modifier.background(Brush.linearGradient(brand.gradient), RoundedCornerShape(radius))
    else
        Modifier.background(brand.bg, RoundedCornerShape(radius))

    Box(
        modifier         = Modifier.size(size).clip(RoundedCornerShape(radius)).then(bgMod),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = brand.label,
            color      = brand.textColor,
            fontSize   = fontSize,
            fontWeight = FontWeight.Bold,
            lineHeight = fontSize
        )
    }
}
