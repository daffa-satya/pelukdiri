package com.makhp.pelukdiri.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class NavGraphTest {
    @Test
    fun `fresh install starts onboarding and completed install starts home`() {
        assertEquals(Screen.Onboarding.route, initialRoute(isOnboardingCompleted = false))
        assertEquals(Screen.Home.route, initialRoute(isOnboardingCompleted = true))
    }
}
