package com.wordlock.app

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

class ScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, showSystemUi = false)

    @Test
    fun lock() {
        paparazzi.snapshot {
            LockScreen(prefs = Prefs(paparazzi.context), preview = false, onUnlocked = {})
        }
    }

    @Test
    fun settings() {
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {}
            }
        }
        paparazzi.snapshot {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                WordLockTheme { SettingsScreen() }
            }
        }
    }
}
