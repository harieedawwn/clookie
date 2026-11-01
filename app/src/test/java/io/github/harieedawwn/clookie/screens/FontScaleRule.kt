package io.github.harieedawwn.clookie.screens

import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.RuntimeEnvironment

/** Applies the system font scale from `-PfontScale=` (default 1.0) to every screenshot. */
class FontScaleRule : TestWatcher() {
    override fun starting(description: Description) {
        val scale = System.getProperty("clookie.fontScale")?.toFloatOrNull() ?: 1f
        if (scale != 1f) RuntimeEnvironment.setFontScale(scale)
    }
}
