package io.github.samolego.ascendo_trainboard.ui.problems

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.ProblemSummary
import io.github.samolego.ascendo_trainboard.ui.getFrenchGrade
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
fun ProblemCard(
    problem: ProblemSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
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
                    text = problem.name ?: "Problem #${problem.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Grade badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = getFrenchGrade(problem.grade),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Author
                Column {
                    Text(
                        text = "By",
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
                        text = "Sector",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = problem.sectorName,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Rating (if available)
                problem.averageStars?.let { stars ->
                    Column {
                        Text(
                            text = "Rating",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "⭐ ${stars.toString().take(3)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Average grade (if available)
                problem.averageGrade?.let { grade ->
                    Column {
                        Text(
                            text = "Avg Grade",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
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
        problem = ProblemSummary(
            id = 2,
            author = "samolego",
            description = "Be sure to climb with helmet on!",
            grade = 20,
            sectorName = "Podest",
            averageStars = 3.24f,
            averageGrade = 18.23f,
        )
    )
}
