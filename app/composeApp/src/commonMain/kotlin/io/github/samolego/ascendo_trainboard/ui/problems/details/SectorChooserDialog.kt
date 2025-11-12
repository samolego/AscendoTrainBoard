package io.github.samolego.ascendo_trainboard.ui.problems.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.ui.components.SectorChooserDropdown
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorChooserDialog(
    sectors: List<SectorSummary>,
    onChoose: (SectorSummary?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedSector by remember { mutableStateOf<SectorSummary?>(null) }

    BasicAlertDialog(
        onDismissRequest = {},
    ) {
        Card {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = "Izberi sektor",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                SectorChooserDropdown(
                    showAllOption = false,
                    onSectorSelected = {
                        selectedSector = it
                    },
                    sectors = sectors,
                    selectedSector = selectedSector,
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                    ) {
                        Text("Prekliči")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(
                        enabled = selectedSector != null,
                        onClick = {
                            onChoose(selectedSector)
                        }
                    ) {
                        Text("Izberi")
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun PreviewSectorChooseDialog() {
    SectorChooserDialog(
        sectors = listOf(
            SectorSummary(
                id = 1,
                name = "Test Sector",
            )
        ),
        onDismiss = {},
        onChoose = {},
    )
}
