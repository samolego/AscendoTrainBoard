package io.github.samolego.ascendo_trainboard

import android.content.Context
import android.os.Build
import io.github.samolego.ascendo_trainboard.generated.BuildKonfig

class AndroidPlatform(context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val storage: PlatformStorage = AndroidStorage(context)

    override fun baseUrl(): String {
        return if (BuildKonfig.DEBUG) {
            "http://192.168.0.139:3000"
        } else {
            "http://192.168.1.1"
        }
    }
}

private lateinit var platformInstance: Platform

fun initializePlatform(context: Context) {
    platformInstance = AndroidPlatform(context.applicationContext)
}

actual fun getPlatform(): Platform = platformInstance
