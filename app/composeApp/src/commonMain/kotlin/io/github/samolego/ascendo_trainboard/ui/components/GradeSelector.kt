package io.github.samolego.ascendo_trainboard.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.samolego.ascendo_trainboard.ui.getFrenchGrade
import kotlin.math.roundToInt

@Composable
fun GradeRangeSelector(
    minGrade: Int,
    maxGrade: Int,
    onGradeRangeChanged: (Int, Int) -> Unit,
) {
    var selectedMinGrade by remember(minGrade) { mutableStateOf(minGrade) }
    var selectedMaxGrade by remember(maxGrade) { mutableStateOf(maxGrade) }

    var currentSliderRange by remember(selectedMinGrade, selectedMaxGrade) {
        mutableStateOf(selectedMinGrade.toFloat()..selectedMaxGrade.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        GradeBadge(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            grade = selectedMinGrade,
            secondGrade = selectedMaxGrade,
        )

        Spacer(modifier = Modifier.height(8.dp))

        RangeSlider(
            value = currentSliderRange,
            onValueChange = { range ->
                currentSliderRange = range
                selectedMinGrade = range.start.roundToInt()
                selectedMaxGrade = range.endInclusive.roundToInt()
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
}

@Composable
fun GradeSelector(
    grade: Int,
    onGradeChanged: (Int) -> Unit,
) {
    var selectedGrade by remember(grade) { mutableStateOf(grade) }

    var sliderValue by remember(selectedGrade) {
        mutableStateOf(selectedGrade.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        GradeBadge(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            grade = selectedGrade,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = sliderValue,
            onValueChange = { range ->
                sliderValue = range
                selectedGrade = range.roundToInt()
            },
            onValueChangeFinished = {
                onGradeChanged(sliderValue.roundToInt())
            },
            valueRange = 0f..32f,
            steps = 32,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GradeBadge(
    modifier: Modifier = Modifier,
    grade: Int,
    secondGrade: Int? = null,
) {
    val text = if (secondGrade == null || secondGrade == grade) {
        "Ocena = ${getFrenchGrade(grade)}"
    } else {
        "${getFrenchGrade(grade)} ≤ ocena ≤ ${getFrenchGrade(secondGrade)}"
    }

    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(4.dp)
        )
    }
}
