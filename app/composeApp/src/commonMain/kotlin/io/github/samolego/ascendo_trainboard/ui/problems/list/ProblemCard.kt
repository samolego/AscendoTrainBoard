package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemSummary
import io.github.samolego.ascendo_trainboard.ui.components.GradeBadge
import io.github.samolego.ascendo_trainboard.ui.getFrenchGrade
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
fun ProblemCard(
    modifier: Modifier = Modifier,
    problem: ProblemSummary,
    sectorName: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8f))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = problem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Grade badge
                GradeBadge(
                    grade = problem.grade,
                    usePrefixText = false,
                )
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
