package io.github.samolego.ascendo_trainboard.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.samolego.ascendo_trainboard.api.generated.models.Problem
import io.github.samolego.ascendo_trainboard.api.generated.models.Sector


@Composable
fun SectorProblemImage(
    sectorImageUrl: String,
    sector: Sector,
    problem: Problem,
    interactive: Boolean = true,
) {
    var origImgSize by remember { mutableStateOf(IntSize.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

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
            .padding(8.dp)
            .clip(RoundedCornerShape(8.dp))
            .shadow(elevation = 8.dp)
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth(),
            onSuccess = {
                origImgSize = IntSize(it.result.image.width, it.result.image.height)
                println("Success! Size: $origImgSize")
            },
            onError = {
                println("Error: ${it.result.image}")
                println("Error: ${it.result.request.data}")
            },
            model = sectorImageUrl,
            contentDescription = "Image of sector ${sector.name}.",
        )


        Canvas(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged {
                    canvasSize = it
                    println("Canvas size: $it")
                }
                .then(
                    if (interactive) {
                        Modifier.pointerInput(holdRects, origImgSize, canvasSize) {
                            detectTapGestures { offset ->
                                println("Tap detected at: $offset")
                                if (origImgSize.width > 0 && canvasSize.width > 0) {
                                    val scale = canvasSize.width.toFloat() / origImgSize.width.toFloat()
                                    val clickedIndex = holdRects.indexOfFirst { rect ->
                                        val scaledRect = Rect(
                                            left = rect.left * scale,
                                            top = rect.top * scale,
                                            right = rect.right * scale,
                                            bottom = rect.bottom * scale
                                        )
                                        scaledRect.contains(offset)
                                    }
                                    if (clickedIndex != -1) {
                                        val hold = sector.holds[clickedIndex]
                                        println("Hold clicked: $hold")
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            val scale = size.width / origImgSize.width

            val markHold = { rect: Rect, color: Color ->
                val scaledRect = Rect(
                    left = rect.left * scale,
                    top = rect.top * scale,
                    right = rect.right * scale,
                    bottom = rect.bottom * scale,
                )

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
                    markHold(rect, Color.Cyan)
                }
            }

            problem.holdSequence.forEach { holds ->
                val index = holds[0]
                val type = holds[1]

                if (index < sector.holds.size) {
                    val hold = sector.holds[index]
                    val color = typeToColor(type)

                    val rect = Rect(
                        left = hold[0].toFloat(),
                        top = hold[1].toFloat(),
                        right = hold[2].toFloat(),
                        bottom = hold[3].toFloat()
                    )

                    markHold(rect, color)
                }
            }
        }
    }
}

private fun typeToColor(type: Int): Color {
    return when (type) {
        0 -> Color.Green
        1 -> Color.Yellow
        2 -> Color.Blue
        3 -> Color.Red
        else -> Color.Cyan
    }
}
