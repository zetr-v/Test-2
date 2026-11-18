package com.example

import com.example.data.formatFileSize
import com.example.data.formatSavingsPercent
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun formatFileSize_formatsBytesKbAndMb() {
        assertEquals("0 B", formatFileSize(0))
        assertEquals("5.0 KB", formatFileSize(5120))
        assertEquals("250 KB", formatFileSize(256_000))
        assertEquals("1.50 MB", formatFileSize(1_572_864))
    }

    @Test
    fun formatSavingsPercent_calculatesReductionAndIncrease() {
        assertEquals("-50%", formatSavingsPercent(1000, 500))
        assertEquals("+10%", formatSavingsPercent(1000, 1100))
        assertEquals("0%", formatSavingsPercent(1000, 1000))
    }
}
