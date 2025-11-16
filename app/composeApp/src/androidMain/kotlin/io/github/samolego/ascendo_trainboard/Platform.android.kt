package io.github.samolego.ascendo_trainboard

import android.content.Context
import android.os.Build

class AndroidPlatform(context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val storage: PlatformStorage = AndroidStorage(context)

    override fun baseUrl(debug: Boolean): String {
        return "http://192.168.1.1:${ if (debug) "3000" else "80"}"
    }
}

private lateinit var platformInstance: Platform

fun initializePlatform(context: Context) {
    platformInstance = AndroidPlatform(context.applicationContext)
}

actual fun getPlatform(): Platform = platformInstance
