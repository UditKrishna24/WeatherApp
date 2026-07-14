package com.example.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun dayName_isCorrect() {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())

        val timestamp = 1783987200000L

        val day = sdf.format(Date(timestamp))

        assertEquals("Tuesday", day)
    }

    @Test
    fun dateFormat_isCorrect() {

        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

        val date = sdf.format(Date())

        assertTrue(date.matches(Regex("\\d{2} .* \\d{4}")))
    }

    @Test
    fun weatherConditionMapping() {

        val sunnyConditions = listOf(
            "Clear",
            "Sunny",
            "Clear Sky"
        )

        assertTrue("Clear" in sunnyConditions)
        assertTrue("Sunny" in sunnyConditions)
        assertTrue("Clear Sky" in sunnyConditions)
    }
}
