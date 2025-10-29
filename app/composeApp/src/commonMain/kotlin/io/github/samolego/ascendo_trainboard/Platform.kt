package io.github.samolego.ascendo_trainboard

interface Platform {
    val name: String
    fun getHostname(debug: Boolean): String
}

expect fun getPlatform(): Platform
