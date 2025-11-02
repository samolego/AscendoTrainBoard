package io.github.samolego.ascendo_trainboard.api

import androidx.compose.ui.graphics.Color

data class ProblemHold(
    val holdIndex: Int,
    val holdType: HoldType,
) {
    fun toList(): List<Int> {
        val list = mutableListOf<Int>()

        list.add(holdIndex)
        list.add(holdType.ordinal)

        return list
    }

    companion object {
        fun fromList(list: List<Int>): ProblemHold? {
            if (list.size != 2 || list[0] < 0 || list[1] >= HoldType.entries.size) {
                return null
            }
            return ProblemHold(
                holdIndex = list[0],
                holdType = HoldType.entries[list[1]]
            )
        }
    }
}


enum class HoldType(val outlineColor: Color) {
    START(Color.Green),
    FOOT(Color.Yellow),
    NORMAL(Color.Cyan),
    END(Color.Red);

    fun getTypeName(): String {
        return when (this) {
            START -> "Začeten"
            FOOT -> "Noga"
            NORMAL -> "Običajen"
            END -> "Konec"
        }
    }
}
