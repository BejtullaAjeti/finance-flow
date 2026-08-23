package com.example.financeflow.ui

import com.example.financeflow.ui.navigation.CATEGORIES_ROUTE
import com.example.financeflow.ui.navigation.FinanceFlowDestination
import com.example.financeflow.ui.navigation.RECURRING_ROUTE
import com.example.financeflow.ui.navigation.matchesCurrentRoute
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceFlowDestinationTest {
    @Test
    fun `each destination matches its own route`() {
        FinanceFlowDestination.entries.forEach { destination ->
            assertTrue(destination.matchesCurrentRoute(destination.route))
        }
    }

    @Test
    fun `Settings matches Categories and Recurring sub-routes`() {
        assertTrue(FinanceFlowDestination.Settings.matchesCurrentRoute(CATEGORIES_ROUTE))
        assertTrue(FinanceFlowDestination.Settings.matchesCurrentRoute(RECURRING_ROUTE))
    }

    @Test
    fun `Home does not match Categories or Recurring sub-routes`() {
        assertFalse(FinanceFlowDestination.Home.matchesCurrentRoute(CATEGORIES_ROUTE))
        assertFalse(FinanceFlowDestination.Home.matchesCurrentRoute(RECURRING_ROUTE))
    }

    @Test
    fun `null route matches nothing`() {
        FinanceFlowDestination.entries.forEach { destination ->
            assertFalse(destination.matchesCurrentRoute(null))
        }
    }
}
