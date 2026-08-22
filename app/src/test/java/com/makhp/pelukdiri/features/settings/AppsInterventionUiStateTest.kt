package com.makhp.pelukdiri.features.settings

import org.junit.Assert.assertTrue
import org.junit.Test

class AppsInterventionUiStateTest {

    @Test
    fun `initial state reports loading while installed apps are discovered`() {
        assertTrue(AppsInterventionUiState().isLoading)
    }
}
