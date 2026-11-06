package com.wenbo.bookkeeping.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartScaleTest {

    @Test
    fun `rounds up to nice number`() {
        assertEquals(10_000L, ChartScale.maxOf(8_700))
        assertEquals(200L, ChartScale.maxOf(123))
        assertEquals(5L, ChartScale.maxOf(4))
        assertEquals(1L, ChartScale.maxOf(1))
    }

    @Test
    fun `exact values stay`() {
        assertEquals(10_000L, ChartScale.maxOf(10_000))
        assertEquals(2L, ChartScale.maxOf(2))
    }

    @Test
    fun `zero and negative get default`() {
        assertEquals(100L, ChartScale.maxOf(0))
        assertEquals(100L, ChartScale.maxOf(-5))
    }
}
