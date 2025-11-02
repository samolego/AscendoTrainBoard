package io.github.samolego.ascendo_trainboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import io.github.samolego.ascendo_trainboard.api.ProblemHold
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector


@Composable
fun ZoomableSectorProblemImage(
    sectorImageUrl: String,
    sector: Sector,
    holds: Collection<ProblemHold>,
    interactive: Boolean = false,
    onImageLoadError: (AsyncImagePainter.State.Error) -> Unit = {},
    onHoldClicked: (holdIndex: Int) -> Unit = {},
    onHoldLongClicked: (holdIndex: Int) -> Unit = {},
    selectedHold: ProblemHold?,
) {
    var origImgSize by remember { mutableStateOf(IntSize.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var zoom by remember { mutableStateOf(1f) }
    var zoomOffsetX by remember { mutableStateOf(0f) }
    var zoomOffsetY by remember { mutableStateOf(0f) }


    val holdRects = remember(sector.holds) {
        sector.holds.map { rect ->
            Rect(
                left = rect[0].toFloat(),
                top = rect[1].toFloat(),
                right = rect[2].toFloat(),
                bottom = rect[3].toFloat()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .shadow(elevation = 8.dp)
            .pointerInput(interactive, holdRects, origImgSize, canvasSize) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downPosition = down.position
                    var totalPan = Offset.Zero
                    var totalZoom = 1f
                    var wasMultiTouch = false

                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }

                        if (!canceled) {
                            if (event.changes.size > 1) {
                                wasMultiTouch = true
                            }

                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            totalZoom *= zoomChange
                            totalPan += panChange

                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                zoom *= zoomChange

                                val newOffsetX = zoomOffsetX + panChange.x
                                val newOffsetY = zoomOffsetY + panChange.y

                                // Only apply pan constraints when zoomed in
                                if (zoom > 1f) {
                                    // Calculate bounds to prevent white space
                                    // When scaled from center, the image can move in both directions
                                    val scaledWidth = canvasSize.width * zoom
                                    val scaledHeight = canvasSize.height * zoom

                                    // Maximum distance we can pan is half the difference between scaled and canvas size
                                    val maxPanX = (scaledWidth - canvasSize.width) / 2f
                                    val maxPanY = (scaledHeight - canvasSize.height) / 2f

                                    // Clamp offsets to bounds (symmetric around center)
                                    zoomOffsetX = newOffsetX.coerceIn(-maxPanX, maxPanX)
                                    zoomOffsetY = newOffsetY.coerceIn(-maxPanY, maxPanY)
                                } else {
                                    // When zoom <= 1, reset to center/no offset
                                    zoomOffsetX = 0f
                                    zoomOffsetY = 0f
                                    zoom = 1f
                                }

                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })

                    val movement = totalPan.getDistance()

                    if (interactive && !wasMultiTouch && movement < 10f && totalZoom == 1f) {
                        val tapPosition = downPosition

                        if (origImgSize.width > 0 && canvasSize.width > 0) {
                            val imageScale = canvasSize.width.toFloat() / origImgSize.width.toFloat()

                            // Account for center-based zoom transformation
                            val centerX = canvasSize.width / 2f
                            val centerY = canvasSize.height / 2f

                            val adjustedX = ((tapPosition.x - centerX - zoomOffsetX) / zoom) + centerX
                            val adjustedY = ((tapPosition.y - centerY - zoomOffsetY) / zoom) + centerY
                            val adjustedTapPosition = Offset(adjustedX, adjustedY)

                            val clickedIndex = holdRects.indexOfFirst { rect ->
                                val scaledRect = Rect(
                                    left = rect.left * imageScale,
                                    top = rect.top * imageScale,
                                    right = rect.right * imageScale,
                                    bottom = rect.bottom * imageScale
                                )
                                scaledRect.contains(adjustedTapPosition)
                            }

                            if (clickedIndex != -1) {
                                onHoldClicked(clickedIndex)
                                //val hold = sector.holds[clickedIndex]
                                //println("Hold clicked: $hold")
                            }
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = zoomOffsetX,
                    translationY = zoomOffsetY
                ),
            onSuccess = {
                origImgSize = IntSize(it.result.image.width, it.result.image.height)
                println("Success! Size: $origImgSize")
            },
            onError = onImageLoadError,
            model = sectorImageUrl,
            contentDescription = "Image of sector ${sector.name}.",
        )


        Canvas(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(
                    scaleX = zoom,
                    scaleY = zoom,
                    translationX = zoomOffsetX,
                    translationY = zoomOffsetY
                )
                .onSizeChanged {
                    canvasSize = it
                }
        ) {
            val scale = size.width / origImgSize.width

            val markHold = { rect: Rect, color: Color, selected: Boolean ->
                val scaledRect = Rect(
                    left = rect.left * scale,
                    top = rect.top * scale,
                    right = rect.right * scale,
                    bottom = rect.bottom * scale,
                )

                if (selected) {
                    drawRect(
                        color = color.copy(alpha = 0.5f),
                        topLeft = Offset(scaledRect.left, scaledRect.top),
                        size = Size(
                            width = scaledRect.width,
                            height = scaledRect.height,
                        ),
                    )
                }

                drawRect(
                    color = color,
                    topLeft = Offset(scaledRect.left, scaledRect.top),
                    size = Size(
                        width = scaledRect.width,
                        height = scaledRect.height,
                    ),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    ),

                )
            }

            if (interactive) {
                holdRects.forEach { rect ->
                    markHold(rect, Color.LightGray, false)
                }
            }

            holds.forEach { hold ->
                val index = hold.holdIndex

                if (index < sector.holds.size) {
                    val color =  hold.holdType.outlineColor
                    val sectorHold = sector.holds[index]

                    val rect = Rect(
                        left = sectorHold[0].toFloat(),
                        top = sectorHold[1].toFloat(),
                        right = sectorHold[2].toFloat(),
                        bottom = sectorHold[3].toFloat()
                    )

                    markHold(rect, color, index == selectedHold?.holdIndex)
                }
            }
        }
    }
}
