package io.github.samolego.ascendo_trainboard

import kotlinx.browser.window

class WasmPlatform : Platform {
    override val name = "Web with Kotlin/Wasm"
    override val storage = WasmLocalStorage()

    override fun baseUrl(debug: Boolean) =
        if (debug) {
            "${window.location.protocol}//${window.location.hostname}:3000"
        } else {
            "${window.location.protocol}//${window.location.host}"
        }
}

actual fun getPlatform(): Platform = WasmPlatform()
