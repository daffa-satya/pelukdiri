package com.makhp.pelukdiri.features.settings

import kotlinx.collections.immutable.persistentSetOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsInterventionUiStateTest {

    @Test
    fun `initial state reports loading while installed apps are discovered`() {
        assertTrue(AppsInterventionUiState().isLoading)
    }

    @Test
    fun `selected count excludes monitored packages that are not installed`() {
        val state = AppsInterventionUiState(
            installedPackageNames = persistentSetOf("installed.one", "installed.two"),
            selectedPackageNames = persistentSetOf(
                "installed.one",
                "installed.two",
                "missing.one",
                "missing.two"
            )
        )

        assertEquals(2, state.selectedCount)
    }
}
