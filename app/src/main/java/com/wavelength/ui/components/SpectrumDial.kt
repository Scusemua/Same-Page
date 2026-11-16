package com.wavelength.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.wavelength.game.GameConfig
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private val DIAL_BG_LIGHT = Color(0xFFEAECEF)
private val DIAL_BG_DARK = Color(0xFF2E2E34)
private val DIAL_BORDER_LIGHT = Color(0xFF5A5A66)
private val DIAL_BORDER_DARK = Color(0xFF8E8E9A)

private val BULLSEYE_COLOR = Color(0xFFFF6B35)
private val INNER_COLOR = Color(0xFFFFA366)
private val OUTER_COLOR = Color(0xFFFFD4B8)
private val TARGET_COLOR = Color(0xFFD84315)
private val GUESS_COLOR = Color(0xFF1E88E5)
private val NEEDLE_WIDTH = 4.5f

@Composable
fun SpectrumDial(
    modifier: Modifier = Modifier,
    targetPosition: Float? = null,
    guessPosition: Float? = null,
    showScoringZones: Boolean = false,
    revealProgress: Float = 1f,
    pulseProgress: Float = 0f,
    isDraggable: Boolean = false,
    onGuessChanged: (Float) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val dialBgColor = if (isDark) DIAL_BG_DARK else DIAL_BG_LIGHT
    val dialBorderColor = if (isDark) DIAL_BORDER_DARK else DIAL_BORDER_LIGHT

    fun calculatePositionFromTouch(touchOffset: Offset, width: Float, height: Float): Float? {
        val center = Offset(width / 2f, height)
        val dx = touchOffset.x - center.x
        val dy = center.y - touchOffset.y

        val distFromCenter = sqrt(dx * dx + dy * dy)
        val radius = width / 2f
        if (distFromCenter < radius * 0.15f) return null

        val angle = atan2(dy, dx)
        if (angle < -0.15f || angle > PI + 0.15f) return null

        val position = 1f - (angle / PI).toFloat()
        return position.coerceIn(0f, 1f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .then(
                if (isDraggable) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                calculatePositionFromTouch(
                                    offset,
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )?.let(onGuessChanged)
                            }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                calculatePositionFromTouch(
                                    change.position,
                                    size.width.toFloat(),
                                    size.height.toFloat()
                                )?.let(onGuessChanged)
                            }
                        }
                } else {
                    Modifier
                }
            )
    ) {
        val centerX = size.width / 2f
        val centerY = size.height - 6f
        val radius = (size.width / 2f - 10f).coerceAtMost(centerY - 6f)
        val clampedReveal = revealProgress.coerceIn(0f, 1f)

        drawDialBackground(centerX, centerY, radius, dialBgColor, dialBorderColor)

        if (showScoringZones && targetPosition != null && clampedReveal > 0f) {
            drawScoringZones(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                targetPosition = targetPosition,
                revealProgress = clampedReveal
            )
        }

        drawDialTicks(centerX, centerY, radius, dialBorderColor)

        // Expanding reveal shockwave arc
        if (pulseProgress > 0f && pulseProgress < 1f) {
            val pulseRadius = radius * (0.25f + 0.75f * pulseProgress)
            val arcTopLeft = Offset(centerX - pulseRadius, centerY - pulseRadius)
            drawArc(
                color = BULLSEYE_COLOR.copy(alpha = (1f - pulseProgress) * 0.55f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = Size(pulseRadius * 2, pulseRadius * 2),
                style = Stroke(width = 6f * (1f - pulseProgress) + 2f)
            )
        }

        // Target needle shoots outward from center pivot as revealProgress animates
        if (targetPosition != null && clampedReveal > 0.05f) {
            drawNeedle(
                centerX = centerX,
                centerY = centerY,
                radius = radius * clampedReveal,
                position = targetPosition,
                color = TARGET_COLOR,
                width = NEEDLE_WIDTH + 1.5f
            )
        }

        // Blue guess needle
        if (guessPosition != null) {
            drawNeedle(centerX, centerY, radius, guessPosition, GUESS_COLOR, NEEDLE_WIDTH)
        }

        drawPivotCap(centerX, centerY, dialBorderColor, dialBgColor)
    }
}

private fun DrawScope.drawDialBackground(
    centerX: Float,
    centerY: Float,
    radius: Float,
    bgColor: Color,
    borderColor: Color
) {
    val arcTopLeft = Offset(centerX - radius, centerY - radius)
    val arcSize = Size(radius * 2, radius * 2)

    drawArc(
        color = bgColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = arcTopLeft,
        size = arcSize
    )

    drawArc(
        color = borderColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = arcTopLeft,
        size = arcSize,
        style = Stroke(width = 3.5f)
    )

    drawLine(
        color = borderColor,
        start = Offset(centerX - radius, centerY),
        end = Offset(centerX + radius, centerY),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawDialTicks(
    centerX: Float,
    centerY: Float,
    radius: Float,
    tickColor: Color
) {
    val tickCount = 10
    for (i in 1 until tickCount) {
        val fraction = i.toFloat() / tickCount
        val angle = PI * (1.0 - fraction)
        val innerR = if (i == tickCount / 2) radius * 0.88f else radius * 0.93f
        val startX = centerX + (innerR * cos(angle)).toFloat()
        val startY = centerY - (innerR * sin(angle)).toFloat()
        val endX = centerX + (radius * cos(angle)).toFloat()
        val endY = centerY - (radius * sin(angle)).toFloat()

        drawLine(
            color = tickColor.copy(alpha = 0.45f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (i == tickCount / 2) 3f else 2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawScoringZones(
    centerX: Float,
    centerY: Float,
    radius: Float,
    targetPosition: Float,
    revealProgress: Float
) {
    val centerAngle = 180f + (targetPosition * 180f)

    val outerHalfSweep = GameConfig.OUTER_RADIUS * 180f * revealProgress
    val innerHalfSweep = GameConfig.INNER_RADIUS * 180f * revealProgress
    val bullseyeHalfSweep = GameConfig.BULLSEYE_RADIUS * 180f * revealProgress

    val arcTopLeft = Offset(centerX - radius, centerY - radius)
    val arcSize = Size(radius * 2, radius * 2)

    fun drawClampedZone(color: Color, halfSweep: Float) {
        val start = (centerAngle - halfSweep).coerceIn(180f, 360f)
        val end = (centerAngle + halfSweep).coerceIn(180f, 360f)
        val sweep = end - start
        if (sweep > 0f) {
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = arcTopLeft,
                size = arcSize
            )
        }
    }

    drawClampedZone(OUTER_COLOR, outerHalfSweep)
    drawClampedZone(INNER_COLOR, innerHalfSweep)
    drawClampedZone(BULLSEYE_COLOR, bullseyeHalfSweep)
}

private fun DrawScope.drawNeedle(
    centerX: Float,
    centerY: Float,
    radius: Float,
    position: Float,
    color: Color,
    width: Float
) {
    val angle = PI * (1.0 - position)
    val endX = centerX + (radius * cos(angle)).toFloat()
    val endY = centerY - (radius * sin(angle)).toFloat()

    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = width,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = color,
        radius = width * 1.9f,
        center = Offset(endX, endY)
    )
}

private fun DrawScope.drawPivotCap(
    centerX: Float,
    centerY: Float,
    borderColor: Color,
    centerColor: Color
) {
    drawCircle(
        color = borderColor,
        radius = 11f,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = centerColor,
        radius = 5f,
        center = Offset(centerX, centerY)
    )
}
