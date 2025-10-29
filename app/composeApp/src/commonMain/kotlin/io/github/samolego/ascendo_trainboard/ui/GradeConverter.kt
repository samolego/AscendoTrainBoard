package io.github.samolego.ascendo_trainboard.ui

private fun _test() {
    val table = mapOf(
        0 to "3",
        1 to "4a",
        2 to "4b",
        3 to "4c",
        4 to "5a",
        5 to "5a+",
        6 to "5b",
        7 to "5b+",
        8 to "5c",
        9 to "5c+",
        10 to "6a",
        11 to "6a+",
        12 to "6b",
        13 to "6b+",
        14 to "6c",
        15 to "6c+",
        16 to "7a",
        17 to "7a+",
        18 to "7b",
        19 to "7b+",
        20 to "7c",
        21 to "7c+",
        22 to "8a",
        23 to "8a+",
        24 to "8b",
        25 to "8b+",
        26 to "8c",
        27 to "8c+",
        28 to "9a",
        29 to "9a+",
        30 to "9b",
        31 to "9b+",
        32 to "9c",
    )
    table.entries.forEach {
        if (getFrenchGrade(it.key) != it.value) {
            println("${it.key} is wrong (should be ${it.value}), but got ${getFrenchGrade(it.key)}")
        }
    }
}

fun getFrenchGrade(grade: Int): String =
    when(grade) {
        0 -> "3"
        in 1 .. 3 -> "4${getLetter((grade - 1) % 3)}"
        else -> {
            val start = grade - 4
            val gradeNumber = 5 + start / 6
            val remainder = (start % 6)
            val letter = getLetter(remainder / 2 )
            val plus = if (remainder % 2 == 1) "+" else ""
            "$gradeNumber$letter$plus"
        }
    }

private fun getLetter(remainder: Int) = 'a' + remainder
