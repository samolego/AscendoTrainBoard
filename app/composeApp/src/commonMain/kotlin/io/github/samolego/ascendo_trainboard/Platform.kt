package io.github.samolego.ascendo_trainboard

interface Platform {
    val name: String
    fun baseUrl(debug: Boolean): String
}

expect fun getPlatform(): Platform
