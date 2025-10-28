package io.github.samolego.ascendo_trainboard

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform