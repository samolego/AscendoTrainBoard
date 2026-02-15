package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.Attempt
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.Tag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagChip(
    tag: Tag,
    sectorNameResolver: (Int) -> String?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jsonObject = Json.encodeToJsonElement(Tag.serializer(), tag).jsonObject
    val entry = jsonObject.entries.firstOrNull()

    val label = if (entry != null) {
        val key = entry.key
        // Retrieve raw string content (unquoted)
        val rawValue = entry.value.jsonPrimitive.content

        val displayValue = when (key) {
            "SectorId" -> tag.sectorId?.let { sectorNameResolver(it) } ?: rawValue
            "Tekmovalni" -> if (rawValue == "true") "Da" else "Ne"
            else -> rawValue
        }
        "$key: $displayValue"
    } else {
        "Unknown Tag"
    }

    InputChip(
        selected = true,
        onClick = {}, // Tag editing could be implemented here
        label = { Text(label) },
        trailingIcon = {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove tag",
                modifier = Modifier
                    .size(InputChipDefaults.IconSize)
                    .clickable { onRemove() }
            )
        },
        modifier = modifier
    )
}

@Composable
fun TagSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onAddTag: (Tag) -> Unit,
    sectors: List<SectorSummary>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Open dropdown when clicked even if already focused
    LaunchedEffect(isPressed) {
        if (isPressed) {
            expanded = true
        }
    }

    // Get all possible keys dynamically from the Tag class
    val allTagKeys = remember {
        try {
            val json = Json { encodeDefaults = true }
            json.encodeToJsonElement(Tag.serializer(), Tag()).jsonObject.keys
        } catch (e: Exception) {
            emptySet<String>()
        }
    }

    val excludedKeys = remember {
        val json = Json { encodeDefaults = false }
        fun key(t: Tag) = json.encodeToJsonElement(Tag.serializer(), t).jsonObject.keys.firstOrNull() ?: ""
        setOf(
            key(Tag(minGrade = 0)),
            key(Tag(maxGrade = 0)),
            key(Tag(spremenjeniZaDatumom = 0L)),
            key(Tag(tekmovalni = true)),
            key(Tag(sectorId = 0)),
        )
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
            },
            interactionSource = interactionSource,
            placeholder = {
                if (selectedCategory == null)
                    Text("Filtriraj (npr. Sektor, Avtor...)")
                else
                    Text("Vrednost za $selectedCategory...")
            },
            leadingIcon = {
                if (selectedCategory != null) {
                    InputChip(
                        selected = true,
                        onClick = {
                            selectedCategory = null
                            onQueryChange("")
                        },
                        label = { Text(selectedCategory!!) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear category",
                                modifier = Modifier
                                    .size(InputChipDefaults.IconSize)
                                    .clickable {
                                        selectedCategory = null
                                        onQueryChange("")
                                    }
                            )
                        },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                // If in category mode and we have typed something valid that works as generic assignment
                if (selectedCategory != null && query.isNotEmpty()) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        modifier = Modifier.clickable {
                            // Try to create generic tag
                            try {
                                val tag = Json.decodeFromJsonElement(
                                    Tag.serializer(),
                                    kotlinx.serialization.json.JsonObject(
                                        mapOf(
                                            selectedCategory!! to JsonPrimitive(
                                                query
                                            )
                                        )
                                    )
                                )
                                onAddTag(tag)
                                onQueryChange("")
                                selectedCategory = null
                                expanded = false
                            } catch (e: Exception) {
                                // Invalid value for type
                            }
                        }
                    )
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    if (it.isFocused && query.isBlank()) {
                        expanded = true
                    }
                }
                .onKeyEvent {
                    if (it.key == Key.Backspace && query.isEmpty() && selectedCategory != null) {
                        selectedCategory = null
                        true
                    } else {
                        false
                    }
                }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .heightIn(max = 240.dp)
        ) {
            val lowerQuery = query.lowercase()

            if (selectedCategory == null) {
                // --- STEP 1: CATEGORY SELECTION MODE ---

                // Always show explicit "Tekmovalni: Da" shortcut if empty or matching
                if (query.isBlank() || "tekmovalni".contains(lowerQuery)) {
                    DropdownMenuItem(
                        text = { Text("Tekmovalni: Da") },
                        onClick = {
                            onAddTag(Tag(tekmovalni = true))
                            onQueryChange("")
                            expanded = false
                        }
                    )
                }

                // Suggest Categories based on available keys
                allTagKeys.forEach { key ->
                    if (key in excludedKeys) return@forEach

                    // Friendly Name Mapping / Filtering
                    val isMatch = when {
                        key == "SectorId" -> "sektor".startsWith(lowerQuery) || "sector".startsWith(lowerQuery)
                        key == "Tekmovalni" -> "tekmovalni".startsWith(lowerQuery)
                        key == "Splezani" -> "splezani".startsWith(lowerQuery) || "poskus".startsWith(lowerQuery) || "attempt".startsWith(
                            lowerQuery
                        )

                        else -> key.lowercase().startsWith(lowerQuery)
                    }

                    if (isMatch) {
                        val friendlyName = when (key) {
                            "SectorId" -> "Sektor"
                            "Tekmovalni" -> "Tekmovalni" // For manual selection
                            "Splezani" -> "Poskus"
                            else -> key
                        }

                        DropdownMenuItem(
                            text = { Text(friendlyName) },
                            onClick = {
                                selectedCategory = key
                                onQueryChange("")
                                // Keep expanded to show values immediately
                                expanded = true
                            }
                        )
                    }
                }

                // If query is empty, show all sectors as shortcut (per requirement)
                if (query.isBlank()) {
                    sectors.forEach { sector ->
                        DropdownMenuItem(
                            text = { Text("Sektor: ${sector.name}") },
                            onClick = {
                                onAddTag(Tag(sectorId = sector.id))
                                expanded = false
                            }
                        )
                    }
                }

            } else {
                // --- STEP 2: VALUE SELECTION MODE ---
                val category = selectedCategory!!

                when (category) {
                    "SectorId" -> {
                        val filteredSectors = if (query.isBlank()) sectors else sectors.filter {
                            it.name.lowercase().contains(lowerQuery)
                        }
                        if (filteredSectors.isEmpty()) {
                            DropdownMenuItem(text = { Text("Ni rezultatov") }, onClick = {})
                        }
                        filteredSectors.forEach { sector ->
                            DropdownMenuItem(
                                text = { Text(sector.name) },
                                onClick = {
                                    onAddTag(Tag(sectorId = sector.id))
                                    onQueryChange("")
                                    selectedCategory = null
                                    expanded = false
                                }
                            )
                        }
                    }

                    "Tekmovalni" -> {
                        if (query.isBlank() || "da".startsWith(lowerQuery)) {
                            DropdownMenuItem(text = { Text("Da") }, onClick = {
                                onAddTag(Tag(tekmovalni = true))
                                onQueryChange("")
                                selectedCategory = null
                                expanded = false
                            })
                        }
                        if (query.isBlank() || "ne".startsWith(lowerQuery)) {
                            DropdownMenuItem(text = { Text("Ne") }, onClick = {
                                onAddTag(Tag(tekmovalni = false))
                                onQueryChange("")
                                selectedCategory = null
                                expanded = false
                            })
                        }
                    }

                    "Splezani" -> {
                        // Iterate Enum values
                        Attempt.values().forEach { attempt ->
                            if (query.isBlank() || attempt.name.lowercase().contains(lowerQuery)) {
                                DropdownMenuItem(text = { Text(attempt.name) }, onClick = {
                                    onAddTag(Tag(splezani = attempt))
                                    onQueryChange("")
                                    selectedCategory = null
                                    expanded = false
                                })
                            }
                        }
                    }

                    else -> {
                        // Generic Value Input
                        if (query.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Uporabi: $query") },
                                onClick = {
                                    try {
                                        val tag = Json.decodeFromJsonElement(
                                            Tag.serializer(),
                                            kotlinx.serialization.json.JsonObject(mapOf(category to JsonPrimitive(query)))
                                        )
                                        onAddTag(tag)
                                        onQueryChange("")
                                        selectedCategory = null
                                        expanded = false
                                    } catch (e: Exception) {
                                        // Ignore invalid values
                                    }
                                }
                            )
                        } else {
                            DropdownMenuItem(text = { Text("Vpiši vrednost...") }, onClick = {})
                        }
                    }
                }
            }
        }
    }
}
