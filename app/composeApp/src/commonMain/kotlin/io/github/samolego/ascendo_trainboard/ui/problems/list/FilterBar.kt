package io.github.samolego.ascendo_trainboard.ui.problems.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.api.generated.models.SectorSummary
import io.github.samolego.ascendo_trainboard.api.generated.models.Tag
import io.github.samolego.ascendo_trainboard.ui.components.GradeBadge
import io.github.samolego.ascendo_trainboard.ui.components.GradeRangeSelector
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBar(
    sectors: List<SectorSummary>,
    tags: List<Tag>,
    minGrade: Int,
    maxGrade: Int,
    onAddTag: (Tag) -> Unit,
    onRemoveTag: (Tag) -> Unit,
    onGradeRangeChanged: (Int, Int) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
    isCollapsed: Boolean = false,
    onExpand: () -> Unit = {},
) {
    var searchQuery by remember { mutableStateOf("") }

    // Use AnimatedContent to switch between the collapsed and expanded content
    AnimatedContent(
        targetState = isCollapsed,
        modifier = modifier,
        transitionSpec = {
            val contentTransform = if (targetState) { // Collapsing (Expanded -> Collapsed)
                val exitTransition = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(200))
                val enterTransition = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(200))

                enterTransition togetherWith exitTransition

            } else {
                val exitTransition = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(200))
                val enterTransition = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(200))
                enterTransition togetherWith exitTransition
            }

            // Apply the SizeTransform to the combined transition
            contentTransform.using(
                SizeTransform(clip = false, sizeAnimationSpec = { _, _ -> tween(300) })
            )
        },
        label = "FilterBarCollapseAnimation"
    ) { collapsed ->
        if (collapsed) {
            // --- COLLAPSED FILTER BAR ---
            // Small, condensed bar with interactive elements to expand.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp) // Fixed height for the collapsed bar
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onExpand)
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (tags.isNotEmpty()) "${tags.size} filtrov" else "Vsi balvani",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Filtri")
                }

                GradeBadge(
                    grade = minGrade,
                    secondGrade = maxGrade,
                    onClick = onExpand,
                )

                Spacer(Modifier.width(8.dp))

                // 3. Search Icon
                IconButton(onClick = onExpand) {
                    Icon(Icons.Default.Search, contentDescription = "Išči po avtorju")
                }
            }

        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Tag Search Input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    TagSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onAddTag = onAddTag,
                        sectors = sectors,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Active Tags Chips
                if (tags.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags.filter { it.minGrade == null && it.maxGrade == null }) { tag ->
                            TagChip(
                                tag = tag,
                                sectorNameResolver = { id -> sectors.find { it.id == id }?.name },
                                onRemove = { onRemoveTag(tag) }
                            )
                        }
                    }
                }

                GradeRangeSelector(
                    minGrade = minGrade,
                    maxGrade = maxGrade,
                    onGradeRangeChanged = onGradeRangeChanged,
                )

                // Clear Filters Button
                if (tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            onClearFilters()
                            searchQuery = ""
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Počisti vse")
                        }
                    }
                }
            }
        }
    }
}


@Preview
@Composable
private fun PreviewExtendedFilterBar() {
    FilterBar(
        sectors = listOf(),
        tags = emptyList(),
        minGrade = 1,
        maxGrade = 16,
        onAddTag = {},
        onRemoveTag = {},
        onGradeRangeChanged = { _, _ -> },
        onClearFilters = {},
        isCollapsed = false,
        onExpand = {},
    )
}

@Preview
@Composable
private fun PreviewCollapsedFilterBar() {
    FilterBar(
        sectors = listOf(),
        tags = emptyList(),
        minGrade = 1,
        maxGrade = 16,
        onAddTag = {},
        onRemoveTag = {},
        onGradeRangeChanged = { _, _ -> },
        onClearFilters = {},
        isCollapsed = true,
        onExpand = {},
    )
}
