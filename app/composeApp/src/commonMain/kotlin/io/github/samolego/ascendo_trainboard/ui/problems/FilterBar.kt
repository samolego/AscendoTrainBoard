package io.github.samolego.ascendo_trainboard.ui.problems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
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
import io.github.samolego.ascendo_trainboard.ui.getFrenchGrade
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    sectors: List<SectorSummary>,
    selectedSector: String?,
    minGrade: Int,
    maxGrade: Int,
    searchAuthor: String,
    onSectorSelected: (String?) -> Unit,
    onGradeRangeChanged: (Int, Int) -> Unit,
    onAuthorChanged: (String) -> Unit,
    onAuthorSearch: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAuthorSearch by remember { mutableStateOf(false) }
    var expandedSector by remember { mutableStateOf(false) }

    var minGrade by remember(minGrade) { mutableStateOf(minGrade) }
    var maxGrade by remember(maxGrade) { mutableStateOf(maxGrade) }

    var currentSliderRange by remember(minGrade, maxGrade) {
        mutableStateOf(minGrade.toFloat()..maxGrade.toFloat())
    }

    Column(modifier = modifier) {
        // Sector Filter Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expandedSector,
                onExpandedChange = { expandedSector = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedSector ?: "All Sectors",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sector") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSector)
                    },
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expandedSector,
                    onDismissRequest = { expandedSector = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Sectors") },
                        onClick = {
                            onSectorSelected(null)
                            expandedSector = false
                        }
                    )
                    sectors.forEach { sector ->
                        DropdownMenuItem(
                            text = { Text("Sector ${sector.name}") },
                            onClick = {
                                onSectorSelected(sector.name)
                                expandedSector = false
                            }
                        )
                    }
                }
            }
        }

        // Grade Range Filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Badge(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    text = "${getFrenchGrade(minGrade)} ≤ grade ≤ ${getFrenchGrade(maxGrade)}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            RangeSlider(
                value = currentSliderRange,
                onValueChange = { range ->
                    currentSliderRange = range
                    minGrade = range.start.roundToInt()
                    maxGrade = range.endInclusive.roundToInt()
                },
                onValueChangeFinished = {
                    onGradeRangeChanged(
                        currentSliderRange.start.roundToInt(),
                        currentSliderRange.endInclusive.roundToInt()
                    )
                },
                valueRange = 0f..32f,
                steps = 32,
                modifier = Modifier.fillMaxWidth()
            )
        }

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
                    placeholder = { Text("Author name") },
                    trailingIcon = {
                        IconButton(onClick = onAuthorSearch) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
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
                    Text("Search by author")
                }
            }

            if (selectedSector != null || minGrade != 1 || maxGrade != 10 || searchAuthor.isNotBlank()) {
                TextButton(onClick = {
                    onClearFilters()
                    showAuthorSearch = false
                }) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
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
        onAuthorSearch = {},
        onClearFilters = {}
    )
}
