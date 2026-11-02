package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFontFamily

private const val DEVELOPER_TELEGRAM_URL = "https://t.me/SOLO_dev1"
private const val DEVELOPER_HANDLE = "@SOLO_dev1"

/**
 * Custom vector-drawn Telegram Paper Plane Icon that continuously cycles through a rainbow spectrum.
 */
@Composable
fun AnimatedRainbowTelegramIcon(
    modifier: Modifier = Modifier,
    iconSize: Dp = 26.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "telegram_rainbow_icon")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbow_phase"
    )

    val primaryGlow by infiniteTransition.animateColor(
        initialValue = Color(0xFFFF0055),
        targetValue = Color(0xFF00E5FF),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rainbow_glow"
    )

    val rainbowColors = listOf(
        Color(0xFFFF0055),
        Color(0xFFFF7A00),
        Color(0xFFFFEA00),
        Color(0xFF00E676),
        Color(0xFF00B0FF),
        Color(0xFF7C4DFF),
        Color(0xFFFF0055)
    )

    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val brush = Brush.linearGradient(
            colors = rainbowColors,
            start = Offset(phase % w, 0f),
            end = Offset((phase % w) + w, h)
        )

        // Outer glowing circle ring
        drawCircle(
            brush = brush,
            radius = w * 0.48f,
            style = Stroke(width = w * 0.08f)
        )

        // Subtle inner backdrop
        drawCircle(
            color = primaryGlow.copy(alpha = 0.16f),
            radius = w * 0.44f,
            style = Fill
        )

        // Authentic Telegram Paper Plane Silhouette Path
        val planePath = Path().apply {
            moveTo(w * 0.76f, h * 0.25f)
            lineTo(w * 0.18f, h * 0.48f)
            lineTo(w * 0.36f, h * 0.56f)
            lineTo(w * 0.66f, h * 0.35f)
            lineTo(w * 0.44f, h * 0.60f)
            lineTo(w * 0.44f, h * 0.75f)
            lineTo(w * 0.54f, h * 0.65f)
            lineTo(w * 0.66f, h * 0.74f)
            close()
        }

        drawPath(
            path = planePath,
            brush = brush,
            style = Fill
        )
    }
}

/**
 * Compact TopAppBar Pill Button combining the Rainbow Animated Telegram Icon with `@SOLO_dev1`.
 */
@Composable
fun RainbowTelegramTopBarPill(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "topbar_rainbow_pill")
    val shift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pill_shift"
    )

    val rainbowColors = listOf(
        Color(0xFFFF0055),
        Color(0xFFFF7A00),
        Color(0xFFFFEA00),
        Color(0xFF00E676),
        Color(0xFF00B0FF),
        Color(0xFFAA00FF),
        Color(0xFFFF0055)
    )

    val borderBrush = Brush.linearGradient(
        colors = rainbowColors,
        start = Offset(shift, 0f),
        end = Offset(shift + 300f, 120f)
    )

    Surface(
        color = Color(0xFF0B0F19),
        shape = RoundedCornerShape(50),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(width = 1.6.dp, brush = borderBrush, shape = RoundedCornerShape(50))
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_TELEGRAM_URL)))
                }
            }
            .testTag("developer_telegram_topbar_btn")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedRainbowTelegramIcon(iconSize = 22.dp)
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "تواصل مع المطور",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color(0xFFD1D5DB)
                )
                Text(
                    text = DEVELOPER_HANDLE,
                    style = TextStyle(
                        brush = borderBrush,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

/**
 * Full-width Rainbow Animated Developer Banner Card for Overview & Auth screens.
 */
@Composable
fun RainbowDeveloperContactCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "card_rainbow_border")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "card_offset"
    )

    val rainbowColors = listOf(
        Color(0xFFFF0055),
        Color(0xFFFF7A00),
        Color(0xFFFFEA00),
        Color(0xFF00E676),
        Color(0xFF00B0FF),
        Color(0xFFAA00FF),
        Color(0xFFFF0055)
    )

    val rainbowBrush = Brush.linearGradient(
        colors = rainbowColors,
        start = Offset(offset, 0f),
        end = Offset(offset + 450f, 200f)
    )

    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(width = 2.dp, brush = rainbowBrush, shape = RoundedCornerShape(16.dp))
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_TELEGRAM_URL)))
                }
            }
            .testTag("developer_telegram_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B0F19))
                        .border(1.5.dp, rainbowBrush, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedRainbowTelegramIcon(iconSize = 32.dp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "الدعم الفني والتواصل المباشر مع مطور التطبيق",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "اضغط هنا لفتح محادثة تلجرام مباشرة مع المطور الرسمي",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                color = Color(0xFF0B0F19),
                shape = RoundedCornerShape(50),
                modifier = Modifier.border(1.5.dp, rainbowBrush, RoundedCornerShape(50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedRainbowTelegramIcon(iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = DEVELOPER_HANDLE,
                        style = TextStyle(
                            brush = rainbowBrush,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    )
                }
            }
        }
    }
}
