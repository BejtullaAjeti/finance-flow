package com.example.financeflow.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReportBarChartLabelTest {
    @Test
    fun `returns the label at a valid index`() {
        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        assertEquals("Thu", resolveBucketLabel(labels, 3.0))
    }

    @Test
    fun `never returns empty when asked for an index beyond a shorter label list`() {
        // Reproduces the crash: axis still querying indices from a larger stale model (e.g.
        // Yearly's 12 buckets) against a period that just switched to a shorter list (Weekly's 7).
        val staleLargerModelSize = 12
        val newShorterLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        for (index in 0 until staleLargerModelSize) {
            val result = resolveBucketLabel(newShorterLabels, index.toDouble())
            assertFalse("index $index produced an empty string", result.isEmpty())
        }
    }

    @Test
    fun `never returns empty for a negative index`() {
        val labels = listOf("Jan", "Feb", "Mar")
        assertFalse(resolveBucketLabel(labels, -1.0).isEmpty())
    }

    @Test
    fun `never returns empty for a null or empty label list`() {
        assertFalse(resolveBucketLabel(null, 0.0).isEmpty())
        assertFalse(resolveBucketLabel(emptyList(), 0.0).isEmpty())
    }
}
