package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Drafts
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold
import com.example.ui.theme.AiluaMutedLavender

@Composable
fun AppIconItem(
    name: String,
    iconKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    badge: String? = null,
    showLabel: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "icon_press_scale"
    )

    val (iconVector, gradientColors) = getAppVisuals(iconKey)

    Column(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag("app_icon_$iconKey"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            // App squircle container with soft gradient & subtle shadow
            Box(
                modifier = Modifier
                    .size(size)
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(17.dp),
                        ambientColor = gradientColors.first().copy(alpha = 0.25f),
                        spotColor = gradientColors.last().copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(17.dp))
                    .background(Brush.linearGradient(gradientColors))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(17.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = name,
                    modifier = Modifier.size(size * 0.48f),
                    tint = Color.White
                )
            }

            // Notification / Unread Badge
            if (!badge.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFE87A7A), Color(0xFFD45555))
                            )
                        )
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getAppVisuals(iconKey: String): Pair<ImageVector, List<Color>> {
    return when (iconKey) {
        "chat" -> Pair(
            Icons.Default.ChatBubbleOutline,
            listOf(Color(0xFF8CAAC6), Color(0xFF6B8CA8))
        )
        "moments" -> Pair(
            Icons.Default.LocalFlorist,
            listOf(Color(0xFFDCA8A8), Color(0xFFBA8383))
        )
        "living" -> Pair(
            Icons.Default.Spa,
            listOf(Color(0xFFAFA3C8), Color(0xFF8F80AD))
        )
        "memories" -> Pair(
            Icons.Default.Stars,
            listOf(Color(0xFFD8BD8C), Color(0xFFBA9C67))
        )
        "mailbox", "mail", "letter" -> Pair(
            Icons.Default.Drafts,
            listOf(Color(0xFFDCA8B5), Color(0xFFB88291))
        )
        "call", "phone" -> Pair(
            Icons.Default.Call,
            listOf(Color(0xFF86B9A5), Color(0xFF5E9A84))
        )
        "gallery" -> Pair(
            Icons.Default.PhotoAlbum,
            listOf(Color(0xFF86B9B0), Color(0xFF639E94))
        )
        "world" -> Pair(
            Icons.Default.Language,
            listOf(Color(0xFF8CA5C8), Color(0xFF6E8CB4))
        )
        "games" -> Pair(
            Icons.Default.Gamepad,
            listOf(Color(0xFFCCA8B9), Color(0xFFA78294))
        )
        "apps" -> Pair(
            Icons.Default.GridView,
            listOf(Color(0xFF9EA3AF), Color(0xFF7E8492))
        )
        "contacts" -> Pair(
            Icons.Default.Contacts,
            listOf(Color(0xFF8BA5BE), Color(0xFF6E8CA8))
        )
        "relations" -> Pair(
            Icons.Default.Share,
            listOf(Color(0xFF9E95B8), Color(0xFF7E749C))
        )
        "check_phone" -> Pair(
            Icons.Default.Visibility,
            listOf(Color(0xFFD6B57E), Color(0xFFBA975E))
        )
        "diary" -> Pair(
            Icons.Default.BookmarkBorder,
            listOf(Color(0xFFCCA2A2), Color(0xFFAA8080))
        )
        "bridge" -> Pair(
            Icons.Default.Nfc,
            listOf(Color(0xFF90B5C6), Color(0xFF7099AC))
        )
        "agent", "assistant" -> Pair(
            Icons.Default.SmartToy,
            listOf(Color(0xFFA2AEC6), Color(0xFF8190AC))
        )
        "dream" -> Pair(
            Icons.Default.Nightlight,
            listOf(Color(0xFFB1A2C8), Color(0xFF907EAC))
        )
        "creator" -> Pair(
            Icons.Default.Palette,
            listOf(Color(0xFFCE9EAC), Color(0xFFAF7B89))
        )
        "lore" -> Pair(
            Icons.Default.BookmarkBorder,
            listOf(Color(0xFFD1B78C), Color(0xFFB39665))
        )
        "shortcut" -> Pair(
            Icons.Default.Extension,
            listOf(Color(0xFF9DB5C8), Color(0xFF7897AE))
        )
        "im" -> Pair(
            Icons.Default.Send,
            listOf(Color(0xFF8EBDAA), Color(0xFF6DA28E))
        )
        "theater" -> Pair(
            Icons.Default.Movie,
            listOf(Color(0xFFC09EB8), Color(0xFFA27C99))
        )
        "market" -> Pair(
            Icons.Default.Storefront,
            listOf(Color(0xFF4C475A), Color(0xFF332F40))
        )
        "lock" -> Pair(
            Icons.Default.Lock,
            listOf(Color(0xFF5B3C46), Color(0xFF3D252D))
        )
        else -> Pair(
            Icons.Default.AutoAwesome,
            listOf(AiluaMistBlue, AiluaMutedLavender)
        )
    }
}
