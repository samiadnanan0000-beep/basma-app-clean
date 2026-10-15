package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil

class SignageCalculatorTest {

    @Test
    fun testSignageAreaAndSheetsCalculation() {
        val width = 4.0
        val height = 1.5
        val area = width * height
        val sheetArea = 2.97
        val rawSheets = (area * 1.10) / sheetArea
        val claddingSheets = ceil(rawSheets).toInt().coerceAtLeast(1)

        assertEquals(6.0, area, 0.001)
        assertEquals(3, claddingSheets)
    }

    @Test
    fun testLedModulesAndTransformersCalculation() {
        val area = 6.0
        val ledCount = ceil(area * 45).toInt()
        val transformers = ceil(ledCount / 220.0).toInt().coerceAtLeast(1)

        assertEquals(270, ledCount)
        assertEquals(2, transformers)
    }

    @Test
    fun testCurrencyConversions() {
        val costYer = 100000.0
        val sarRate = 142.0
        val usdRate = 535.0

        val sar = costYer / sarRate
        val usd = costYer / usdRate

        assertTrue(sar > 0)
        assertTrue(usd > 0)
        assertTrue(sar > usd)
    }
}
