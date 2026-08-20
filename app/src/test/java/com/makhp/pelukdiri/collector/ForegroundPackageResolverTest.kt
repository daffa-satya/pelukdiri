package com.makhp.pelukdiri.collector

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundPackageResolverTest {
    @Test fun `recents root wins over transient target card event`() {
        assertEquals(
            "com.miui.home",
            ForegroundPackageResolver.resolve(
                eventPackage = "com.google.android.youtube",
                activeWindowPackage = "com.miui.home"
            )
        )
    }

    @Test fun `target event remains authoritative when target owns active root`() {
        assertEquals(
            "com.google.android.youtube",
            ForegroundPackageResolver.resolve(
                eventPackage = "com.google.android.youtube",
                activeWindowPackage = "com.google.android.youtube"
            )
        )
    }

    @Test fun `active target wins over stale intervention event`() {
        assertEquals(
            "com.google.android.youtube",
            ForegroundPackageResolver.resolve(
                eventPackage = "com.makhp.pelukdiri",
                activeWindowPackage = "com.google.android.youtube"
            )
        )
    }

    @Test fun `null active root falls back to event package`() {
        assertEquals(
            "com.instagram.android",
            ForegroundPackageResolver.resolve("com.instagram.android", null)
        )
    }
}
