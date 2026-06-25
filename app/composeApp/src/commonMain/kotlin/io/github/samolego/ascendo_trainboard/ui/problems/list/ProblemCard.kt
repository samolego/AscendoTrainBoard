package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemSummary
import io.github.samolego.ascendo_trainboard.ui.components.GradeBadge
import io.github.samolego.ascendo_trainboard.ui.getFrenchGrade
import io.github.samolego.ascendo_trainboard.ui.theme.darkGold
import io.github.samolego.ascendo_trainboard.ui.theme.gold
import io.github.samolego.ascendo_trainboard.ui.theme.lightGold
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
fun ProblemCard(
    modifier: Modifier = Modifier,
    problem: ProblemSummary,
    sectorName: String,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
        val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    val winner = problem.winner == true

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (winner) {
                    Modifier.drawWithContent {
                        drawContent()
                        val shimmerBrush = Brush.linearGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.4f to Color.Transparent,
                                0.5f to lightGold,
                                0.6f to Color.Transparent,
                                1.0f to Color.Transparent,
                            ),
                        start = Offset(shimmerOffset * size.width, 0f),
                        end = Offset((shimmerOffset + 1f) * size.width, size.height)
                        )
                        drawRect(brush = shimmerBrush, size = size)

                    }
                } else Modifier
            )
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        border = if (winner) {
            BorderStroke(1.5.dp, gold)
        } else null,
        colors = if (winner) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            )
        } else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = problem.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (winner) {
                        WinnerBadge()
                    }
                }
                GradeBadge(grade = problem.grade, usePrefixText = false)
            }


            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Author
                Column {
                    Text(
                        text = "Avtor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = problem.author,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Sector
                Column {
                    Text(
                        text = "Sektor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sectorName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Rating (if available)
                problem.averageStars?.let { stars ->
                    Column {
                        Text(
                            text = "Mnenje",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            modifier = Modifier.align(Alignment.End),
                            text = "⭐ ${stars.toString().take(3)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Average grade (if available)
                problem.averageGrade?.let { grade ->
                    Column {
                        Text(
                            text = "Povp. ocena",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            modifier = Modifier.align(Alignment.End),
                            text = getFrenchGrade(grade.roundToInt()),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            // Description (if available)
            problem.description?.let { description ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun WinnerBadge() {
    Box(
        modifier = Modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(gold, Color(0xFFF0D070), gold)
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "🏆 Zmagovalni",
            style = MaterialTheme.typography.labelSmall,
            color = darkGold,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun ProblemCardPreview() {
    ProblemCard(
        onClick = {},
        sectorName = "Sector B",
        problem = ProblemSummary(
            id = 2,
            author = "samolego",
            description = "Be sure to climb with helmet on!",
            grade = 20,
            name = "abc",
            sectorId = 1,
            averageStars = 3.24f,
            averageGrade = 18.23f,
        )
    )
}

@Preview
@Composable
private fun ProblemCardPreviewMini() {
    ProblemCard(
        onClick = {},
        sectorName = "Nad podestom",
        problem = ProblemSummary(
            id = 2,
            author = "samolego",
            grade = 20,
            name = "abc",
            sectorId = 2,
        )
    )
}
