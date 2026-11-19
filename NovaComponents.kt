package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.SensitiveActionRequest
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaBorderGlow
import com.example.ui.theme.NovaCoralError
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaDeepSpace
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaIsDarkTheme
import com.example.ui.theme.NovaMagentaGlow
import com.example.ui.theme.NovaOfflineSlate
import com.example.ui.theme.NovaOnAccent
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaSurfaceGlass
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaVoidBlack
import com.example.ui.viewmodel.OrbVisualState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NovaCosmicBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "cosmicBg")
    val drift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )

    val isDark = NovaIsDarkTheme
    val bgBase = NovaVoidBlack
    val bgMid = NovaDeepSpace
    val auraBlue = NovaElectricBlue
    val auraPurple = NovaPurple
    val auraCyan = NovaCyan

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(bgBase, bgMid, bgBase)
                )
            )
            // Electric blue top-right aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraBlue.copy(alpha = if (isDark) 0.22f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * (0.85f - 0.1f * drift), size.height * 0.14f),
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = Offset(size.width * (0.85f - 0.1f * drift), size.height * 0.14f)
            )
            // Purple bottom-left nebula
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraPurple.copy(alpha = if (isDark) 0.18f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * (0.15f + 0.12f * drift), size.height * 0.82f),
                    radius = size.width * 0.9f
                ),
                radius = size.width * 0.9f,
                center = Offset(size.width * (0.15f + 0.12f * drift), size.height * 0.82f)
            )
            // Cyan center subtle glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        auraCyan.copy(alpha = if (isDark) 0.08f else 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * 0.5f, size.height * 0.42f)
            )
        }
        content()
    }
}

@Composable
fun NovaGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = NovaBorderGlow.copy(alpha = if (NovaIsDarkTheme) 0.7f else 0.9f),
    containerColor: Color = NovaSurfaceGlass.copy(alpha = if (NovaIsDarkTheme) 0.74f else 0.92f),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = NovaIsDarkTheme
    val shape = RoundedCornerShape(22.dp)
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .clip(shape)
            .then(clickMod),
        shape = shape,
        color = containerColor,
        shadowElevation = if (isDark) 0.dp else 3.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    NovaCyan.copy(alpha = if (isDark) 0.25f else 0.30f),
                    NovaPurple.copy(alpha = if (isDark) 0.3f else 0.25f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun NovaOrbView(
    state: OrbVisualState,
    rmsLevel: Float = 0f,
    size: Dp = 190.dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "novaOrb")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbVisualState.PROCESSING -> 1400
                    OrbVisualState.LISTENING -> 2600
                    OrbVisualState.SPEAKING -> 2200
                    else -> 6500
                },
                easing = LinearEasing
            )
        ),
        label = "orbAngle"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbVisualState.LISTENING -> 520
                    OrbVisualState.SPEAKING -> 420
                    OrbVisualState.PROCESSING -> 650
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    val primaryColor = when (state) {
        OrbVisualState.IDLE -> NovaCyan
        OrbVisualState.LISTENING -> NovaElectricBlue
        OrbVisualState.PROCESSING -> NovaPurple
        OrbVisualState.SPEAKING -> NovaMagentaGlow
        OrbVisualState.ERROR -> NovaCoralError
        OrbVisualState.OFFLINE -> NovaOfflineSlate
    }
    val secondaryColor = when (state) {
        OrbVisualState.IDLE -> NovaPurple
        OrbVisualState.LISTENING -> NovaCyan
        OrbVisualState.PROCESSING -> NovaCyan
        OrbVisualState.SPEAKING -> NovaCyan
        OrbVisualState.ERROR -> NovaAmber
        OrbVisualState.OFFLINE -> NovaElectricBlue.copy(alpha = 0.4f)
    }

    val dynamicScale = if (state == OrbVisualState.LISTENING) {
        pulse + (rmsLevel * 0.12f)
    } else {
        pulse
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer ambient glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = if (state == OrbVisualState.OFFLINE) 0.16f else 0.36f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Expanding listening sonar rings
            if (state == OrbVisualState.LISTENING || state == OrbVisualState.SPEAKING) {
                val ringScale = 0.72f + ((pulse - 0.94f) / 0.13f) * 0.24f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.35f),
                    radius = radius * ringScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Rotating dual energy arcs
            rotate(degrees = angle, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, primaryColor, secondaryColor, Color.Transparent)
                    ),
                    startAngle = 0f,
                    sweepAngle = if (state == OrbVisualState.PROCESSING) 250f else 180f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.84f, center.y - radius * 0.84f),
                    size = Size(radius * 1.68f, radius * 1.68f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            rotate(degrees = -angle * 1.3f, pivot = center) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(Color.Transparent, secondaryColor, primaryColor, Color.Transparent)
                    ),
                    startAngle = 90f,
                    sweepAngle = 135f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius * 0.76f, center.y - radius * 0.76f),
                    size = Size(radius * 1.52f, radius * 1.52f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Speaking radial equalizer ticks
            if (state == OrbVisualState.SPEAKING) {
                val bars = 24
                for (i in 0 until bars) {
                    val rad = Math.toRadians((i * (360.0 / bars)) + angle)
                    val wave = 0.5f + 0.5f * sin(rad * 3 + pulse * 6f).toFloat()
                    val innerR = radius * 0.70f
                    val outerR = innerR + (6.dp.toPx() + wave * 12.dp.toPx())
                    drawLine(
                        color = primaryColor.copy(alpha = 0.8f),
                        start = Offset(
                            center.x + (innerR * cos(rad)).toFloat(),
                            center.y + (innerR * sin(rad)).toFloat()
                        ),
                        end = Offset(
                            center.x + (outerR * cos(rad)).toFloat(),
                            center.y + (outerR * sin(rad)).toFloat()
                        ),
                        strokeWidth = 2.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Core 3D generated orb image
        Image(
            painter = painterResource(id = R.drawable.img_nova_orb),
            contentDescription = stringResource(R.string.cd_nova_orb),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size * 0.64f)
                .scale(dynamicScale.coerceIn(0.9f, 1.15f))
                .clip(CircleShape)
                .alpha(if (state == OrbVisualState.OFFLINE) 0.55f else 1f)
                .border(
                    width = 1.5.dp,
                    color = primaryColor.copy(alpha = 0.75f),
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun NovaSpeakingWaveform(
    active: Boolean,
    color: Color = NovaCyan,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "waveform")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "wavePhase"
    )

    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(9) { idx ->
            val factor = if (active) {
                (0.28f + 0.72f * ((sin(phase + idx * 0.75f) + 1f) / 2f))
            } else {
                0.2f
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((26 * factor).dp.coerceAtLeast(5.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(color, NovaPurple)
                        )
                    )
            )
        }
    }
}

@Composable
fun NovaThinkingSpinner(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val infinite = rememberInfiniteTransition(label = "thinking")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "spin"
    )
    val spinCyan = NovaCyan
    val spinPurple = NovaPurple
    Canvas(modifier = modifier.size(size)) {
        rotate(spin) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, spinCyan, spinPurple)),
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun NovaOnlineStatusBadge(
    isOnline: Boolean,
    transportType: String,
    wakeActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (isOnline) NovaEmerald.copy(alpha = 0.14f) else NovaOfflineSlate.copy(alpha = 0.18f),
            border = BorderStroke(
                1.dp,
                if (isOnline) NovaEmerald.copy(alpha = 0.55f) else NovaOfflineSlate.copy(alpha = 0.55f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                    contentDescription = if (isOnline) "Online" else "Offline",
                    tint = if (isOnline) NovaEmerald else NovaOfflineSlate,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (isOnline) "ONLINE • $transportType" else "OFFLINE MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOnline) NovaEmerald else NovaOfflineSlate
                )
            }
        }

        AnimatedVisibility(visible = wakeActive) {
            Surface(
                shape = RoundedCornerShape(50),
                color = NovaCyan.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, NovaCyan.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Wake microphone active",
                        tint = NovaCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "MIC: \"NOVA\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaCyan
                    )
                }
            }
        }
    }
}

@Composable
fun NovaTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_button),
                    tint = NovaTextPrimary
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = NovaTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        actions()
    }
}

@Composable
fun NovaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NovaCyan,
            contentColor = NovaOnAccent,
            disabledContainerColor = NovaSurfaceGlass,
            disabledContentColor = NovaTextSecondary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SensitiveActionConfirmCard(
    action: SensitiveActionRequest,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, detail, icon) = when (action) {
        is SensitiveActionRequest.MakeCall -> Triple(
            "Confirm Phone Call",
            "Call ${action.displayName} (${action.phoneNumber})?",
            Icons.Filled.Call
        )
        is SensitiveActionRequest.SendSms -> Triple(
            "Confirm SMS Message",
            "Send to ${action.displayName} (${action.phoneNumber}): \"${action.message}\"",
            Icons.Filled.Sms
        )
        is SensitiveActionRequest.PrepareWhatsApp -> Triple(
            "Confirm WhatsApp Draft",
            "Open WhatsApp for ${action.displayName}: \"${action.message}\"",
            Icons.Filled.Sms
        )
    }

    NovaGlassCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = NovaAmber,
        containerColor = NovaDeepSpace.copy(alpha = 0.94f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = "Confirmation required",
                tint = NovaAmber
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = NovaAmber
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = NovaTextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("cancel_sensitive_action"),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, NovaBorderGlow)
            ) {
                Text("Cancel", color = NovaTextSecondary)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("confirm_sensitive_action"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovaCyan,
                    contentColor = NovaOnAccent
                )
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        }
    }
}
