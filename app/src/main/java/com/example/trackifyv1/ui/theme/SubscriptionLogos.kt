package com.example.trackifyv1.ui.theme

import androidx.compose.ui.graphics.Color

data class ServiceLogo(val emoji: String, val color: Color)

private val knownServices: Map<String, ServiceLogo> = mapOf(
    "netflix"     to ServiceLogo("🎬", Color(0xFFE50914)),
    "spotify"     to ServiceLogo("🎵", Color(0xFF1DB954)),
    "youtube"     to ServiceLogo("▶️", Color(0xFFFF0000)),
    "disney"      to ServiceLogo("🏰", Color(0xFF113CCF)),
    "amazon"      to ServiceLogo("📦", Color(0xFFFF9900)),
    "prime"       to ServiceLogo("📦", Color(0xFFFF9900)),
    "hbo"         to ServiceLogo("🎭", Color(0xFF9B0CB6)),
    "apple"       to ServiceLogo("🍎", Color(0xFFA1A1A6)),
    "icloud"      to ServiceLogo("☁️", Color(0xFFA1A1A6)),
    "google"      to ServiceLogo("🔷", Color(0xFF4285F4)),
    "drive"       to ServiceLogo("☁️", Color(0xFF4285F4)),
    "dropbox"     to ServiceLogo("📦", Color(0xFF0061FF)),
    "microsoft"   to ServiceLogo("🪟", Color(0xFF00A4EF)),
    "office"      to ServiceLogo("📄", Color(0xFFD83B01)),
    "notion"      to ServiceLogo("📝", Color(0xFF000000)),
    "slack"       to ServiceLogo("💬", Color(0xFF4A154B)),
    "zoom"        to ServiceLogo("🎥", Color(0xFF2D8CFF)),
    "github"      to ServiceLogo("🐙", Color(0xFF6E5494)),
    "figma"       to ServiceLogo("🎨", Color(0xFFF24E1E)),
    "canva"       to ServiceLogo("🖌️", Color(0xFF00C4CC)),
    "adobe"       to ServiceLogo("🅰️", Color(0xFFFF0000)),
    "playstation" to ServiceLogo("🎮", Color(0xFF003791)),
    "xbox"        to ServiceLogo("🎮", Color(0xFF107C10)),
    "steam"       to ServiceLogo("🎮", Color(0xFF1B2838)),
    "efootball"   to ServiceLogo("⚽", Color(0xFF00ADEE)),
    "fc mobile"   to ServiceLogo("⚽", Color(0xFF00853E)),
    "fifa"        to ServiceLogo("⚽", Color(0xFF00853E)),
    "tinder"      to ServiceLogo("🔥", Color(0xFFFD5068)),
    "linkedin"    to ServiceLogo("💼", Color(0xFF0A66C2)),
    "twitter"     to ServiceLogo("🐦", Color(0xFF1DA1F2)),
    "x premium"   to ServiceLogo("✖️", Color(0xFF000000)),
    "vpn"         to ServiceLogo("🛡️", Color(0xFF00C8A0)),
    "nordvpn"     to ServiceLogo("🛡️", Color(0xFF4687FF)),
    "expressvpn"  to ServiceLogo("🛡️", Color(0xFFDA3940)),
    "gym"         to ServiceLogo("🏋️", Color(0xFFFF6D00)),
    "headspace"   to ServiceLogo("🧘", Color(0xFFFF8A00)),
    "calm"        to ServiceLogo("🧘", Color(0xFF4B7BEC)),
    "duolingo"    to ServiceLogo("🦉", Color(0xFF58CC02)),
    "audible"     to ServiceLogo("🎧", Color(0xFFFF9900)),
    "kindle"      to ServiceLogo("📚", Color(0xFFFF9900)),
    "patreon"     to ServiceLogo("🅿️", Color(0xFFFF424D)),
    "twitch"      to ServiceLogo("🟣", Color(0xFF9146FF)),
    "discord"     to ServiceLogo("🎮", Color(0xFF5865F2)),
    "chatgpt"     to ServiceLogo("🤖", Color(0xFF74AA9C)),
    "openai"      to ServiceLogo("🤖", Color(0xFF74AA9C)),
    "claude"      to ServiceLogo("🤖", Color(0xFFD97757)),
    "anthropic"   to ServiceLogo("🤖", Color(0xFFD97757)),
    "midjourney"  to ServiceLogo("🖼️", Color(0xFF000000)),
    "showmax"     to ServiceLogo("🎬", Color(0xFFFF0000)),
    "dstv"        to ServiceLogo("📺", Color(0xFF1B5FAE)),
    "phone"       to ServiceLogo("📱", Color(0xFF00C8A0)),
    "internet"    to ServiceLogo("🌐", Color(0xFF00C8A0)),
    "electricity" to ServiceLogo("⚡", Color(0xFFFFC107)),
    "insurance"   to ServiceLogo("🛡️", Color(0xFF607D8B)),
    "rent"        to ServiceLogo("🏠", Color(0xFF8D6E63))
)

private val fallbackPalette = listOf(
    Color(0xFFD4A017), Color(0xFF8B0000), Color(0xFF00BCD4), Color(0xFF7C4DFF),
    Color(0xFF00E676), Color(0xFFFF6D00), Color(0xFFE91E63), Color(0xFF1DE9B6)
)

fun logoFor(subscriptionName: String): ServiceLogo {
    val key = subscriptionName.trim().lowercase()
    knownServices.entries.forEach { (k, v) ->
        if (key.contains(k)) return v
    }
    val initial    = subscriptionName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val colorIndex = (key.hashCode() and 0x7FFFFFFF) % fallbackPalette.size
    return ServiceLogo(initial, fallbackPalette[colorIndex])
}
