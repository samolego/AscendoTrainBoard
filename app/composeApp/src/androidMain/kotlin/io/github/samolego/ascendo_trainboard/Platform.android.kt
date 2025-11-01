package io.github.samolego.ascendo_trainboard

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override fun baseUrl(debug: Boolean): String {
        return "http://192.168.0.139:${ if (debug) "3000" else "80"}"
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()
