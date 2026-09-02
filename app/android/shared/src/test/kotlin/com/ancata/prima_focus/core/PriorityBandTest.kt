package com.ancata.prima_focus.core

import com.ancata.prima_focus.core.model.PriorityBand
import org.junit.Assert.assertEquals
import org.junit.Test

class PriorityBandTest {

    @Test
    fun fromScore_mapsThresholdsCorrectly() {
        assertEquals(PriorityBand.URGENT, PriorityBand.fromScore(70.0))
        assertEquals(PriorityBand.URGENT, PriorityBand.fromScore(95.5))
        assertEquals(PriorityBand.HIGH, PriorityBand.fromScore(69.99))
        assertEquals(PriorityBand.HIGH, PriorityBand.fromScore(40.0))
        assertEquals(PriorityBand.NORMAL, PriorityBand.fromScore(39.99))
        assertEquals(PriorityBand.NORMAL, PriorityBand.fromScore(20.0))
        assertEquals(PriorityBand.LOW, PriorityBand.fromScore(19.99))
        assertEquals(PriorityBand.LOW, PriorityBand.fromScore(0.0))
    }

    @Test
    fun priorityBand_labelsMatchDesignSystem() {
        assertEquals("Urgente", PriorityBand.URGENT.label)
        assertEquals("Alta", PriorityBand.HIGH.label)
        assertEquals("Normal", PriorityBand.NORMAL.label)
        assertEquals("Baja", PriorityBand.LOW.label)
    }

    @Test
    fun priorityBand_boundaryEdges() {
        assertEquals(PriorityBand.URGENT, PriorityBand.fromScore(150.0))
        assertEquals(PriorityBand.LOW, PriorityBand.fromScore(-25.0))
        assertEquals(PriorityBand.LOW, PriorityBand.fromScore(-100.0))
    }
}
