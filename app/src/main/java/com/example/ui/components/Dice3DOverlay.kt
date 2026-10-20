package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dice.DiceType
import com.example.dice.RollResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Particle3D(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    var size: Float,
    var color: Color
)

/**
 * 3D Animated Dice Roller overlay with realistic perspective tumble,
 * dynamic face shading, particle impact sparks, and narrative action result display.
 */
@Composable
fun Dice3DOverlay(
    rollResult: RollResult,
    actionText: String? = null,
    onConfirm: (RollResult) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRolling by remember { mutableStateOf(true) }
    var showResultCard by remember { mutableStateOf(false) }

    val animProgress = remember { Animatable(0f) }
    val bounceY = remember { Animatable(-320f) }
    val rotX = remember { Animatable(0f) }
    val rotY = remember { Animatable(0f) }
    val rotZ = remember { Animatable(0f) }

    // Particles list for celebratory impact
    val particles = remember { mutableListOf<Particle3D>() }
    var particleTick by remember { mutableFloatStateOf(0f) }

    // Colors according to outcome
    val baseDiceColor = when {
        rollResult.isCriticalSuccess -> Color(0xFFEAB308) // Golden
        rollResult.isCriticalFailure -> Color(0xFFDC2626) // Crimson
        rollResult.isSuccess == true -> Color(0xFF10B981) // Emerald
        rollResult.isSuccess == false -> Color(0xFF6B7280) // Muted slate
        else -> MaterialTheme.colorScheme.primary
    }

    LaunchedEffect(Unit) {
        // Initial vibration
        triggerRollHaptic(context, 40)

        // Generate tumble parameters
        val targetRotX = 720f + Random.nextInt(180, 360)
        val targetRotY = 1080f + Random.nextInt(180, 540)
        val targetRotZ = 540f + Random.nextInt(90, 270)

        // Launch animations in parallel
        launch {
            bounceY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        launch {
            rotX.animateTo(
                targetValue = targetRotX,
                animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing)
            )
        }

        launch {
            rotY.animateTo(
                targetValue = targetRotY,
                animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing)
            )
        }

        launch {
            rotZ.animateTo(
                targetValue = targetRotZ,
                animationSpec = tween(durationMillis = 1300, easing = FastOutSlowInEasing)
            )
        }

        launch {
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1450, easing = LinearEasing)
            )
            isRolling = false

            // Impact vibration on settle
            triggerRollHaptic(context, 90)

            // Spawn celebratory impact particles
            for (i in 0 until 35) {
                val angle = Random.nextFloat() * 2f * PI.toFloat()
                val speed = Random.nextFloat() * 9f + 3f
                particles.add(
                    Particle3D(
                        x = 0f,
                        y = 0f,
                        vx = cos(angle) * speed,
                        vy = sin(angle) * speed,
                        alpha = 1f,
                        size = Random.nextFloat() * 6f + 3f,
                        color = if (Random.nextBoolean()) baseDiceColor else Color.White
                    )
                )
            }

            delay(150)
            showResultCard = true
        }
    }

    // Particle update loop
    LaunchedEffect(isRolling) {
        if (!isRolling) {
            while (particles.isNotEmpty()) {
                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.25f // Gravity
                    p.alpha -= 0.035f
                    if (p.alpha <= 0f) {
                        iterator.remove()
                    }
                }
                particleTick += 1f
                delay(16)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // If user taps outside when finished, confirm
                if (showResultCard) onConfirm(rollResult)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header: Action Description if provided
            if (!actionText.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACCIÓN NARRATIVA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"$actionText\"",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3D Canvas Box
            Box(
                modifier = Modifier
                    .size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerX = size.width / 2f
                    val centerY = (size.height / 2f) + bounceY.value

                    // Draw shadow beneath die
                    val shadowScale = (1f - (bounceY.value / -320f)).coerceIn(0.2f, 1f)
                    drawOval(
                        color = Color.Black.copy(alpha = 0.45f * shadowScale),
                        topLeft = Offset(centerX - 70f * shadowScale, size.height / 2f + 85f),
                        size = androidx.compose.ui.geometry.Size(140f * shadowScale, 35f * shadowScale)
                    )

                    // Draw 3D Polyhedral Die (Icosahedron for d20, Cube for d6/others)
                    draw3DPolyhedron(
                        centerX = centerX,
                        centerY = centerY,
                        diceType = rollResult.diceType,
                        rotXDeg = rotX.value,
                        rotYDeg = rotY.value,
                        rotZDeg = rotZ.value,
                        isRolling = isRolling,
                        finalValue = rollResult.naturalRoll,
                        themeColor = baseDiceColor
                    )

                    // Draw particles if exploding
                    particles.forEach { p ->
                        if (p.alpha > 0f) {
                            drawCircle(
                                color = p.color.copy(alpha = p.alpha),
                                radius = p.size,
                                center = Offset(centerX + p.x, centerY + p.y)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Result Reveal Card
            AnimatedVisibility(
                visible = showResultCard,
                enter = fadeIn(tween(300)) + scaleIn(tween(350), initialScale = 0.85f)
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = when {
                            rollResult.isCriticalSuccess -> Color(0xFFEAB308)
                            rollResult.isCriticalFailure -> Color(0xFFEF4444)
                            rollResult.isSuccess == true -> Color(0xFF10B981)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(22.dp))
                        .testTag("dice_result_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Badge / Outcome Title
                        val badgeText = when {
                            rollResult.isCriticalSuccess -> "⚔️ ¡20 NATURAL - ÉXITO CRÍTICO!"
                            rollResult.isCriticalFailure -> "💀 ¡1 NATURAL - PIFIA CRÍTICA!"
                            rollResult.isSuccess == true -> "✅ ¡ÉXITO SUPERADO!"
                            rollResult.isSuccess == false -> "❌ FALLO EN LA TIRADA"
                            else -> "🎲 RESULTADO DE TIRADA"
                        }

                        val badgeColor = when {
                            rollResult.isCriticalSuccess -> Color(0xFFEAB308)
                            rollResult.isCriticalFailure -> Color(0xFFEF4444)
                            rollResult.isSuccess == true -> Color(0xFF10B981)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeColor.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large Natural Roll & Total Display
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${rollResult.naturalRoll}",
                                fontSize = 52.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 54.sp
                            )
                            if (rollResult.modifierValue != 0) {
                                val sign = if (rollResult.modifierValue >= 0) "+${rollResult.modifierValue}" else "${rollResult.modifierValue}"
                                Text(
                                    text = " $sign",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                            Text(
                                text = " = ${rollResult.total}",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor,
                                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                            )
                        }

                        // Modifier breakdown & DC
                        Text(
                            text = buildString {
                                append("Dado ${rollResult.diceType.label.uppercase()}")
                                if (rollResult.modifierValue != 0) {
                                    append(" • Modificador: ${rollResult.modifierName} (${if (rollResult.modifierValue >= 0) "+" else ""}${rollResult.modifierValue})")
                                }
                                if (rollResult.difficultyClass != null) {
                                    append(" • vs DC ${rollResult.difficultyClass}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Confirm & Close Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDismiss() }
                            ) {
                                Text(
                                    text = "Cancelar",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Button(
                                onClick = { onConfirm(rollResult) },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("confirm_dice_result_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = badgeColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Registrar",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Procedural 3D polyhedral mesh projection in DrawScope with specular lighting,
 * dynamic vertex rotations, and face depth-sorting.
 */
private fun DrawScope.draw3DPolyhedron(
    centerX: Float,
    centerY: Float,
    diceType: DiceType,
    rotXDeg: Float,
    rotYDeg: Float,
    rotZDeg: Float,
    isRolling: Boolean,
    finalValue: Int,
    themeColor: Color
) {
    val rotX = (rotXDeg * PI / 180f).toFloat()
    val rotY = (rotYDeg * PI / 180f).toFloat()
    val rotZ = (rotZDeg * PI / 180f).toFloat()

    // 3D Point class
    data class Point3D(val x: Float, val y: Float, val z: Float)

    // Polyhedron vertices definitions (Icosahedron for d20 / d12 / d10 / d8 / d4; Cube for d6)
    val radius = 100f
    val (rawVertices, rawFaces) = if (diceType == DiceType.D6) {
        // Cube
        val s = radius * 0.75f
        val v = listOf(
            Point3D(-s, -s, -s), Point3D(s, -s, -s), Point3D(s, s, -s), Point3D(-s, s, -s),
            Point3D(-s, -s, s),  Point3D(s, -s, s),  Point3D(s, s, s),  Point3D(-s, s, s)
        )
        val f = listOf(
            listOf(0, 1, 2, 3), // Front
            listOf(5, 4, 7, 6), // Back
            listOf(4, 0, 3, 7), // Left
            listOf(1, 5, 6, 2), // Right
            listOf(4, 5, 1, 0), // Top
            listOf(3, 2, 6, 7)  // Bottom
        )
        v to f
    } else {
        // Icosahedron (20-sided)
        val phi = (1f + sqrt(5f)) / 2f
        val scale = radius / sqrt(1f + phi * phi)
        val v = listOf(
            Point3D(-1f * scale, phi * scale, 0f),
            Point3D(1f * scale, phi * scale, 0f),
            Point3D(-1f * scale, -phi * scale, 0f),
            Point3D(1f * scale, -phi * scale, 0f),
            Point3D(0f, -1f * scale, phi * scale),
            Point3D(0f, 1f * scale, phi * scale),
            Point3D(0f, -1f * scale, -phi * scale),
            Point3D(0f, 1f * scale, -phi * scale),
            Point3D(phi * scale, 0f, -1f * scale),
            Point3D(phi * scale, 0f, 1f * scale),
            Point3D(-phi * scale, 0f, -1f * scale),
            Point3D(-phi * scale, 0f, 1f * scale)
        )
        val f = listOf(
            listOf(0, 11, 5), listOf(0, 5, 1), listOf(0, 1, 7), listOf(0, 7, 10), listOf(0, 10, 11),
            listOf(1, 5, 9), listOf(5, 11, 4), listOf(11, 10, 2), listOf(10, 7, 6), listOf(7, 1, 8),
            listOf(3, 9, 4), listOf(3, 4, 2), listOf(3, 2, 6), listOf(3, 6, 8), listOf(3, 8, 9),
            listOf(4, 9, 5), listOf(2, 4, 11), listOf(6, 2, 10), listOf(8, 6, 7), listOf(9, 8, 1)
        )
        v to f
    }

    // Rotate and project all vertices
    val fov = 400f
    fun rotateVertex(p: Point3D): Triple<Float, Float, Float> {
        // Y rotation
        val x1 = p.x * cos(rotY) + p.z * sin(rotY)
        val z1 = -p.x * sin(rotY) + p.z * cos(rotY)

        // X rotation
        val y2 = p.y * cos(rotX) - z1 * sin(rotX)
        val z2 = p.y * sin(rotX) + z1 * cos(rotX)

        // Z rotation
        val x3 = x1 * cos(rotZ) - y2 * sin(rotZ)
        val y3 = x1 * sin(rotZ) + y2 * cos(rotZ)

        return Triple(x3, y3, z2)
    }

    val transformedVertices = rawVertices.map { rotateVertex(it) }

    // Project to 2D
    val projectedPoints = transformedVertices.map { (x, y, z) ->
        val factor = fov / (fov + z + 180f)
        Offset(centerX + x * factor, centerY + y * factor)
    }

    // Light source vector pointing from top-left front
    val lightX = -0.5f
    val lightY = -0.7f
    val lightZ = 0.9f
    val lightLen = sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ)
    val normLightX = lightX / lightLen
    val normLightY = lightY / lightLen
    val normLightZ = lightZ / lightLen

    // Face representation for sorting
    data class VisibleFace(
        val indices: List<Int>,
        val avgZ: Float,
        val normalZ: Float,
        val shade: Float,
        val centerPoint: Offset
    )

    val visibleFaces = mutableListOf<VisibleFace>()

    for (face in rawFaces) {
        val i0 = face[0]
        val i1 = face[1]
        val i2 = face[2]

        val (x0, y0, z0) = transformedVertices[i0]
        val (x1, y1, z1) = transformedVertices[i1]
        val (x2, y2, z2) = transformedVertices[i2]

        // Cross product for normal
        val v1x = x1 - x0
        val v1y = y1 - y0
        val v1z = z1 - z0
        val v2x = x2 - x0
        val v2y = y2 - y0
        val v2z = z2 - z0

        val nx = v1y * v2z - v1z * v2y
        val ny = v1z * v2x - v1x * v2z
        val nz = v1x * v2y - v1y * v2x

        // Back-face culling: only keep faces whose normal points towards viewer (nz > 0)
        if (nz > 0f) {
            val normalLen = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.001f)
            val nnx = nx / normalLen
            val nny = ny / normalLen
            val nnz = nz / normalLen

            val dot = (nnx * normLightX + nny * normLightY + nnz * normLightZ).coerceIn(0f, 1f)
            val shade = (0.35f + 0.65f * dot).coerceIn(0.2f, 1.0f)
            val avgZ = face.map { transformedVertices[it].third }.average().toFloat()

            var avgPx = 0f
            var avgPy = 0f
            for (idx in face) {
                avgPx += projectedPoints[idx].x
                avgPy += projectedPoints[idx].y
            }
            val center = Offset(avgPx / face.size, avgPy / face.size)

            visibleFaces.add(VisibleFace(face, avgZ, nnz, shade, center))
        }
    }

    // Sort by depth (back to front)
    visibleFaces.sortBy { it.avgZ }

    // Find the most front-facing face (highest normalZ) to show the primary result
    val primaryFace = visibleFaces.maxByOrNull { it.normalZ }

    // Draw faces
    for (f in visibleFaces) {
        val path = Path()
        val firstPt = projectedPoints[f.indices[0]]
        path.moveTo(firstPt.x, firstPt.y)
        for (i in 1 until f.indices.size) {
            val pt = projectedPoints[f.indices[i]]
            path.lineTo(pt.x, pt.y)
        }
        path.close()

        // Gradient lighting across the face
        val faceBaseColor = themeColor
        val shadedColor = Color(
            red = (faceBaseColor.red * f.shade).coerceIn(0f, 1f),
            green = (faceBaseColor.green * f.shade).coerceIn(0f, 1f),
            blue = (faceBaseColor.blue * f.shade).coerceIn(0f, 1f),
            alpha = 0.96f
        )

        drawPath(
            path = path,
            color = shadedColor,
            style = Fill
        )

        // Draw crisp beveled wireframe edges
        drawPath(
            path = path,
            color = Color.White.copy(alpha = (0.3f + 0.5f * f.shade).coerceIn(0.2f, 0.85f)),
            style = Stroke(width = 2.5f)
        )

        // Draw inscribed number or rune on visible face
        val isTargetFace = f == primaryFace
        val faceNumber = if (isTargetFace) {
            if (isRolling) Random.nextInt(1, diceType.sides + 1) else finalValue
        } else {
            ((f.indices[0] * 3 + f.indices[1] * 7) % diceType.sides) + 1
        }

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = if (isTargetFace) android.graphics.Color.WHITE else android.graphics.Color.argb(160, 240, 240, 240)
                textSize = if (isTargetFace) 42f else 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                if (isTargetFace) {
                    setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
                }
            }
            drawText("$faceNumber", f.centerPoint.x, f.centerPoint.y + (paint.textSize / 3f), paint)
        }
    }
}

private fun triggerRollHaptic(context: Context, durationMs: Long) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            v?.vibrate(durationMs)
        }
    } catch (_: Exception) {}
}
