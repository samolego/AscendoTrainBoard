package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagChip(
    tag: Tag,
    sectorNameResolver: (Int) -> String?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = tag.searchableLabel().takeIf { it.isNotBlank() } ?: "Neznana kategorija"
    val displayValue = tag.displayValue(sectorNameResolver).takeIf { it.isNotBlank() } ?: "?"

    InputChip(
        selected = true,
        onClick = {},
        label = { Text("$label: $displayValue") },
        leadingIcon = if (tag.negated == true) {
            {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = "Izključeno",
                    modifier = Modifier.size(InputChipDefaults.IconSize)
                )
            }
        } else null,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove tag",
                modifier = Modifier
                    .size(InputChipDefaults.IconSize)
                    .clickable { onRemove() }
            )
        },
        modifier = modifier
    )
}

/**
 * Expanded filter input that appears below the category/grade row once a tag category is chosen.
 * Boolean tags get Da/Ne buttons; other tags get a value field with a leading negate toggle and
 * a trailing add button.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagSearchBar(
    selectedTag: SearchableTag,
    query: String,
    onQueryChange: (String) -> Unit,
    onAddTag: (Tag) -> Unit,
    onClear: () -> Unit,
    sectors: List<SectorSummary>,
    modifier: Modifier = Modifier
) {
    var isNegated by remember(selectedTag) { mutableStateOf(false) }

    fun add(negated: Boolean) {
        selectedTag.createTag(query, negated)?.let(onAddTag)
    }

    Column(modifier = modifier) {
        if (selectedTag.isBoolean) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { selectedTag.createTag("true", false)?.let(onAddTag) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Da")
                }
                Button(
                    onClick = { selectedTag.createTag("false", false)?.let(onAddTag) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ne")
                }
            }
        } else {
            val canAdd = remember(selectedTag, query) {
                selectedTag.createTag(query, false) != null
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { onQueryChange(it) },
                    placeholder = { Text("Vrednost...") },
                    singleLine = true,
                    leadingIcon = {
                        val activeColor = MaterialTheme.colorScheme.error
                        val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
                        IconButton(
                            onClick = { isNegated = !isNegated }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        color = if (isNegated) activeColor.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isNegated) Icons.Filled.Block else Icons.Outlined.Block,
                                    contentDescription = if (isNegated) "Izključi" else "Ne izključi",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isNegated) activeColor else inactiveColor
                                )
                            }
                        }
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { add(isNegated) },
                            enabled = canAdd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Dodaj filter"
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .onKeyEvent {
                            when {
                                it.key == Key.Enter && canAdd -> {
                                    add(isNegated)
                                    true
                                }

                                it.key == Key.Backspace && query.isEmpty() -> {
                                    onClear()
                                    true
                                }

                                else -> false
                            }
                        }
                )
            }

            val recommendations = remember(selectedTag, query, sectors) {
                selectedTag.recommendations(sectors)
                    .filter {
                        it.label.lowercase().contains(query.lowercase()) ||
                                it.value.lowercase().contains(query.lowercase())
                    }
            }

            if (recommendations.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recommendations.forEach { rec ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                selectedTag.createTag(rec.value, isNegated)?.let(onAddTag)
                            },
                            label = { Text(rec.label) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelector(
    selectedTag: SearchableTag?,
    onCategorySelected: (SearchableTag) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedTag?.label ?: "",
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Filter") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SearchableTags.forEach { tag ->
                DropdownMenuItem(
                    text = { Text(tag.label) },
                    onClick = {
                        onCategorySelected(tag)
                        expanded = false
                    }
                )
            }
        }
    }
}
