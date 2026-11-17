package com.laffy.caffeine

import android.app.Activity
import android.os.Bundle

/**
 * UI-less trampoline for the launcher shortcuts: runs the requested action and finishes.
 * Extra [EXTRA_DURATION] is an encoded [Duration] (see [Duration.encode]); [STOP] turns Caffeine off.
 */
class ShortcutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Static-shortcut extras may arrive as Int or String depending on how the XML was compiled.
        @Suppress("DEPRECATION") // Bundle.get is the only type-agnostic read.
        val raw = intent.extras?.get(EXTRA_DURATION)?.toString()?.toIntOrNull() ?: STOP
        if (raw == STOP) {
            CaffeineController.stop(this)
        } else {
            CaffeineController.start(this, decodeDuration(raw))
        }
        finish()
    }

    companion object {
        const val EXTRA_DURATION = "duration"
        const val STOP = 0
    }
}
