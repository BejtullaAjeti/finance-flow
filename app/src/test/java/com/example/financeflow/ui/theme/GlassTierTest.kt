package com.example.financeflow.ui.theme

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassTierTest {
    @Test
    fun `fill opacity increases with tier from Row to Overlay`() {
        val row = GlassTier.Row.spec().fill.alpha
        val card = GlassTier.Card.spec().fill.alpha
        val interactive = GlassTier.Interactive.spec().fill.alpha
        val overlay = GlassTier.Overlay.spec().fill.alpha

        assertTrue("Row ($row) should be more transparent than Card ($card)", row < card)
        assertTrue("Card ($card) should be no more opaque than Interactive ($interactive)", card <= interactive)
        assertTrue("Interactive ($interactive) should be more transparent than Overlay ($overlay)", interactive < overlay)
    }

    @Test
    fun `Row tier has no glow or shadow`() {
        val row = GlassTier.Row.spec()
        assertTrue(row.glowBlur == null)
        assertTrue(row.shadowElevation.value == 0f)
    }

    @Test
    fun `Warning is distinct from Expense and Accent`() {
        assertNotEquals(Warning, Expense)
        assertNotEquals(Warning, Accent)
    }
}
