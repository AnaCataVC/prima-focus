package com.primafocus.domain

import kotlin.math.ln
import kotlin.math.max

object PriorityLogic {

    fun calculatePriorityScore(
        categoryWeight: Double,
        hasDate: Boolean,
        timeUrgency: Double,
        subtasksCount: Int,
        estimatedMinutes: Int,
        ageDays: Double,
        manualBoost: Double = 5.0
    ): Double {
        val hasDateInt = if (hasDate) 1 else 0
        
        var score = (10 * categoryWeight) +
                (6 * hasDateInt) +
                (8 * timeUrgency) -
                (2 * ln(1.0 + subtasksCount)) -
                (0.02 * estimatedMinutes) -
                (0.5 * ageDays) +
                manualBoost

        if (categoryWeight >= 4.0) {
            score = max(score, 70.0)
        }

        return score
    }

    fun evaluateIsProject(estimatedMinutes: Int, subtasksCount: Int): Boolean {
        return estimatedMinutes > 180 || subtasksCount > 10
    }
}
