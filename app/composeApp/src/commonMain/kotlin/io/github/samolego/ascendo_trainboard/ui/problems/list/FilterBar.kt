package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.ui.components.GradeRangeSelector
import io.github.samolego.ascendo_trainboard.ui.components.SectorChooserDropdown
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    sectors: List<SectorSummary>,
    selectedSector: SectorSummary?,
    minGrade: Int,
    maxGrade: Int,
    searchAuthor: String,
    onSectorSelected: (SectorSummary?) -> Unit,
    onGradeRangeChanged: (Int, Int) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
)
{
    var showAuthorSearch by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Sector Filter Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectorChooserDropdown(
                modifier = Modifier.weight(1f),
                showAllOption = true,
                onSectorSelected = onSectorSelected,
                sectors = sectors,
                selectedSector = selectedSector,
            )
        }

        // Grade Range Filter
        GradeRangeSelector(
            minGrade = minGrade,
            maxGrade = maxGrade,
            onGradeRangeChanged = onGradeRangeChanged,
        )

        // Author Search & Clear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showAuthorSearch) {
                OutlinedTextField(
                    value = searchAuthor,
                    onValueChange = onAuthorChanged,
                    placeholder = { Text("Ime smeri") },
                    trailingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Išči")
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            } else {
                TextButton(
                    onClick = { showAuthorSearch = true }
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Išči po imenu")
                }
            }

            if (selectedSector != null || searchAuthor.isNotBlank()) {
                TextButton(onClick = {
                    onClearFilters()
                    showAuthorSearch = false
                }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Počisti")
                }
            }
        }
    }
}


@Preview
@Composable
private fun PreviewFilterBar() {
    FilterBar(
        sectors = listOf(),
        selectedSector = null,
        minGrade = 1,
        maxGrade = 16,
        searchAuthor = "asd",
        onSectorSelected = {},
        onGradeRangeChanged = { _, _ -> },
        onAuthorChanged = {},
        onClearFilters = {}
    )
}
