package io.github.samolego.ascendo_trainboard

import io.github.samolego.ascendo_trainboard.generated.BuildKonfig
import kotlinx.browser.window

class WasmPlatform : Platform {
    override val name = "Web with Kotlin/Wasm"
    override val storage = WasmLocalStorage()

    override fun baseUrl() =
        if (BuildKonfig.DEBUG) {
            "${window.location.protocol}//${window.location.hostname}:3000"
        } else {
            "${window.location.protocol}//${window.location.host}"
        }
}

actual fun getPlatform(): Platform = WasmPlatform()
