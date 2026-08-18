package com.makhp.pelukdiri.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ScreenInteractiveReconstructorTest {

    private lateinit var reconstructor: ScreenInteractiveReconstructor
    private val dayStart = 10000L
    private val queryEnd = 20000L

    @Before
    fun setup() {
        reconstructor = ScreenInteractiveReconstructor()
    }

    @Test
    fun `test 1 Normal session`() {
        val events = listOf(
            UsageEvent("pkg", 11000L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE),
            UsageEvent("pkg", 12000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        assertEquals(1000L, result)
    }

    @Test
    fun `test 2 Open interval`() {
        val events = listOf(
            UsageEvent("pkg", 15000L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE)
        )
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        assertEquals(5000L, result)
    }

    @Test
    fun `test 3 Midnight crossing - Day 1`() {
        // Event at 9950 (before 10000), ended at 10050
        val events = listOf(
            UsageEvent("pkg", 9950L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE),
            UsageEvent("pkg", 10050L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        // Without initial state, it should start from the first event inside or clip the first event if it's before dayStart.
        // My logic clips at max(start, dayStart).
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        assertEquals(50L, result)
    }

    @Test
    fun `test 4 Critical midnight initial state`() {
        // 23:00 INTERACTIVE (9000L)
        // 00:00 dayStart (10000L)
        // 07:00 NON_INTERACTIVE (17000L)
        val events = listOf(
            UsageEvent("pkg", 17000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd, initialStartTime = 9000L)
        assertEquals(7000L, result) // 17000 - 10000
    }

    @Test
    fun `test 5 Previous screen state was OFF`() {
        // 23:00 INTERACTIVE (9000L)
        // 23:30 NON_INTERACTIVE (9500L)
        // 00:00 dayStart (10000L)
        // 07:00 NON_INTERACTIVE (17000L)
        val events = listOf(
            UsageEvent("pkg", 17000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        // initialStartTime should be null because state at midnight is OFF
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd, initialStartTime = null)
        assertEquals(0L, result)
    }

    @Test
    fun `test 6 Duplicate INTERACTIVE`() {
        val events = listOf(
            UsageEvent("pkg", 11000L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE),
            UsageEvent("pkg", 12000L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE),
            UsageEvent("pkg", 13000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        assertEquals(2000L, result)
    }

    @Test
    fun `test 7 Duplicate NON_INTERACTIVE`() {
        val events = listOf(
            UsageEvent("pkg", 11000L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE),
            UsageEvent("pkg", 12000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE),
            UsageEvent("pkg", 13000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        assertEquals(1000L, result)
    }

    @Test
    fun `test 8 No events`() {
        val result = reconstructor.calculateTotalScreenOn(emptyList(), dayStart, queryEnd)
        assertEquals(0L, result)
    }

    @Test
    fun `test 9 Invariant`() {
        val events = listOf(
            UsageEvent("pkg", 5000L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE),
            UsageEvent("pkg", 25000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        val result = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        assertTrue(result >= 0)
        assertTrue(result <= (queryEnd - dayStart))
        assertEquals(10000L, result)
    }

    @Test
    fun `test 10 Idempotency`() {
        val events = listOf(
            UsageEvent("pkg", 11000L, ScreenInteractiveReconstructor.SCREEN_INTERACTIVE),
            UsageEvent("pkg", 15000L, ScreenInteractiveReconstructor.SCREEN_NON_INTERACTIVE)
        )
        val result1 = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        val result2 = reconstructor.calculateTotalScreenOn(events, dayStart, queryEnd)
        assertEquals(result1, result2)
    }
}
