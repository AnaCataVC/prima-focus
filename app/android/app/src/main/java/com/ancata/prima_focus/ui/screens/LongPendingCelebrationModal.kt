package com.ancata.prima_focus.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ancata.prima_focus.ui.theme.AccentSage
import com.ancata.prima_focus.ui.theme.LocalPremiumGlows
import com.ancata.prima_focus.ui.theme.PrimaryRose
import com.ancata.prima_focus.ui.theme.RoseAccent
import com.ancata.prima_focus.ui.theme.RoseGlow
import com.ancata.prima_focus.ui.viewmodel.CelebrationEvent
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val isCircle: Boolean,
    val swayPhase: Float
)

@Composable
fun ConfettiCanvas(
    modifier: Modifier = Modifier,
    particleCount: Int = 75
) {
    val confettiColors = remember {
        listOf(
            PrimaryRose,
            RoseGlow,
            RoseAccent,
            AccentSage,
            Color(0xFFFBBF24), // Warm gold
            Color(0xFFC084FC), // Lavender violet
            Color(0xFF67E8F9)  // Soft cyan
        )
    }

    val particles = remember {
        List(particleCount) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = -Random.nextFloat() * 0.4f, // Start slightly above screen
                speedX = (Random.nextFloat() - 0.5f) * 0.003f,
                speedY = 0.003f + Random.nextFloat() * 0.006f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 8f,
                width = 8f + Random.nextFloat() * 10f,
                height = 5f + Random.nextFloat() * 8f,
                color = confettiColors.random(),
                isCircle = Random.nextBoolean(),
                swayPhase = Random.nextFloat() * 6.28f
            )
        }
    }

    val tick = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        var previousTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (previousTimeNanos != 0L) {
                    val dt = ((frameTimeNanos - previousTimeNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    for (p in particles) {
                        p.y += p.speedY
                        p.x += p.speedX + (sin(p.y * 12f + p.swayPhase) * 0.0015f)
                        p.rotation += p.rotationSpeed

                        // Respawn gently from top if it fell below screen
                        if (p.y > 1.05f) {
                            p.y = -0.05f
                            p.x = Random.nextFloat()
                        }
                    }
                }
                previousTimeNanos = frameTimeNanos
            }
            tick.snapTo((tick.value + 1f) % 1000f)
        }
    }

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (p in particles) {
            val px = p.x * canvasWidth
            val py = p.y * canvasHeight

            rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                if (p.isCircle) {
                    drawCircle(
                        color = p.color,
                        radius = p.width / 2f,
                        center = Offset(px, py)
                    )
                } else {
                    drawRoundRect(
                        color = p.color,
                        topLeft = Offset(px - p.width / 2f, py - p.height / 2f),
                        size = Size(p.width, p.height),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun LongPendingCelebrationModal(
    event: CelebrationEvent,
    onDismiss: () -> Unit
) {
    val glows = LocalPremiumGlows.current

    // Auto-dismiss after 6 seconds
    LaunchedEffect(event.taskId) {
        delay(6000L)
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            // Background full-screen confetti shower
            ConfettiCanvas(modifier = Modifier.fillMaxSize())

            // Modal dialog card with canonical modifier order: background -> border -> clip
            val cardShape = RoundedCornerShape(28.dp)
            val borderBrush = Brush.linearGradient(
                colors = listOf(glows.glassBorderStart, glows.glassBorderEnd)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 420.dp)
                    .background(glows.backgroundCenter.copy(alpha = 0.96f), shape = cardShape)
                    .border(width = 1.dp, brush = borderBrush, shape = cardShape)
                    .clip(cardShape)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Glowing celebratory emoji icon badge
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .background(glows.primaryGlow.copy(alpha = 0.2f), shape = CircleShape)
                            .border(1.dp, glows.primaryAccent.copy(alpha = 0.4f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎉",
                            fontSize = 38.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "¡Al fin!",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tenías esto pendiente desde hace",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = event.formattedDuration,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = glows.primaryAccent,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Task title pill
                    val pillShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(glows.glassSurface, shape = pillShape)
                            .border(1.dp, glows.glassBorderStart.copy(alpha = 0.35f), shape = pillShape)
                            .clip(pillShape)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = event.taskTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = glows.primaryAccent,
                            contentColor = MaterialTheme.colorScheme.onBackground
                        )
                    ) {
                        Text(
                            text = "¡Genial!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
