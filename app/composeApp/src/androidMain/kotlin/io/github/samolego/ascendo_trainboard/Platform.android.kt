package io.github.samolego.ascendo_trainboard

import android.content.Context
import android.os.Build
import io.github.samolego.ascendo_trainboard.generated.BuildKonfig
import java.util.logging.Logger

class AndroidPlatform(context: Context) : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val storage: PlatformStorage = AndroidStorage(context)

    override fun log(msg: String) {
        Logger.getLogger("AscendoTrainboard").info(msg)
    }

    override fun baseUrl(): String {
        Logger.getLogger("AndroidPlatform").info("baseUrl() called: ${BuildConfig.DEBUG} vs ${BuildKonfig.DEBUG}")
        return if (BuildConfig.DEBUG) {
            "http://192.168.0.140:3000"
        } else {
            "http://ascendo.local"
        }
    }
}

private lateinit var platformInstance: Platform

fun initializePlatform(context: Context) {
    platformInstance = AndroidPlatform(context.applicationContext)
}

actual fun getPlatform(): Platform = platformInstance
