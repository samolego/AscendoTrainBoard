package io.github.samolego.ascendo_trainboard.ui.components.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.samolego.ascendo_trainboard.api.generated.models.Attempt
import io.github.samolego.ascendo_trainboard.api.generated.models.SubmitGradeRequest
import io.github.samolego.ascendo_trainboard.ui.components.GradeSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProblemRatingDialog(  // todo - landscape
    defaultGrade: Int,
    defaultRating: Int,
    defaultAttempt: Attempt = Attempt.Redpoint,
    onDismiss: () -> Unit,
    onConfirm: (SubmitGradeRequest) -> Unit
) {
    var grade by remember { mutableStateOf(defaultGrade.coerceAtLeast(0)) }
    var stars by remember {
        mutableStateOf(
            defaultRating.coerceIn(1, 5)
        )
    }
    var attempt by remember { mutableStateOf(defaultAttempt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Oceni težavnost", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GradeSelector(
                    grade = grade,
                    onGradeChanged = { grade = it },
                )

                // Stars chooser
                Column {
                    Text(text = "Zvezdice", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        for (i in 1..5) {
                            IconButton(onClick = { stars = i }) {
                                if (i <= stars) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "$i zvezdic",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Star,
                                        contentDescription = "$i star"
                                    )
                                }
                            }
                        }
                    }
                }

                // Attempt chooser
                Column {
                    Text(text = "Poskus", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Attempt.entries.forEach { a ->
                            val selected = a == attempt
                            OutlinedButton(
                                onClick = { attempt = a },
                                colors = if (selected) ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                ) else ButtonDefaults.outlinedButtonColors(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text(
                                    text = a.name,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Ensure valid range before confirming
                    val safeStars = stars.coerceIn(1, 5)
                    val safeGrade = grade.coerceAtLeast(0)
                    onConfirm(SubmitGradeRequest(grade = safeGrade, stars = safeStars, attempt = attempt))
                }
            ) {
                Text("Potrdi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Prekliči")
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp
    )
}

@Preview
@Composable
private fun PreviewRatingDialog() {
    ProblemRatingDialog(
        defaultGrade = 16,
        defaultRating = 3,
        onDismiss = {},
        onConfirm = {}
    )
}
