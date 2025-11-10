package io.github.samolego.ascendo_trainboard.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorChooserDropdown(
    modifier: Modifier = Modifier,
    showAllOption: Boolean,
    sectors: List<SectorSummary>,
    selectedSector: SectorSummary? = null,
    onSectorSelected: (SectorSummary?) -> Unit,
) {
    var expandedSector by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expandedSector,
        onExpandedChange = { expandedSector = it },
    ) {
        OutlinedTextField(
            value = selectedSector?.name ?: "Vsi sektorji",
            onValueChange = {},
            readOnly = true,
            label = { Text("Sektor") },
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
            if (showAllOption) {
                DropdownMenuItem(
                    text = { Text("Vsi sektorji") },
                    onClick = {
                        onSectorSelected(null)
                        expandedSector = false
                    }
                )
            }
            sectors.forEach { sector ->
                DropdownMenuItem(
                    text = { Text(sector.name) },
                    onClick = {
                        onSectorSelected(sector)
                        expandedSector = false
                    }
                )
            }
        }
    }

}
