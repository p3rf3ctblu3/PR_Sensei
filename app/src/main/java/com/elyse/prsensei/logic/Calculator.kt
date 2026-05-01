package com.elyse.prsensei.logic

import com.elyse.prsensei.PlateType
import kotlin.math.abs

data class SetResult(
    val reps: Int,
    val weight: Double,
    val error: Double
)

object Calculator {

    // Removed the private val coefficients map to allow for per-exercise injection

    // Now accepts the coefficient map for the specific exercise
    fun getNew1RM(weight: Double, reps: Int, percentage: Double, coeffs: Map<Int, Double>): Double {
        val currentCoeff = coeffs[reps] ?: 1.0
        val current1RM = weight / currentCoeff
        return current1RM * (1 + (percentage / 100))
    }

    // Now accepts the coefficient map for the specific exercise
    fun getNextSet(
        new1RM: Double,
        repRange: List<Int>,
        plates: List<PlateType>,
        coeffs: Map<Int, Double>
    ): List<SetResult> {
        return repRange.map { reps ->
            val targetWeight = new1RM * (coeffs[reps] ?: 1.0)
            val bestGymWeight = findBestWeight(targetWeight, plates)

            SetResult(reps, bestGymWeight, bestGymWeight - targetWeight)
        }.sortedBy { abs(it.error) }.take(3)
    }

    private fun findBestWeight(target: Double, plates: List<PlateType>): Double {
        val reachableWeights = mutableSetOf(0.0)

        for (plate in plates) {
            val qty = plate.quantity.toIntOrNull() ?: 0
            if (qty <= 0) continue

            val maxAllowed = if (plate.isExclusive) 1 else qty
            val currentSums = reachableWeights.toSet()

            for (sum in currentSums) {
                for (count in 1..maxAllowed) {
                    if (plate.isEvenOnly && count % 2 != 0) continue

                    val newSum = sum + (plate.weight * count)
                    val rounded = kotlin.math.round(newSum * 100.0) / 100.0

                    if (rounded <= target * 1.5) {
                        reachableWeights.add(rounded)
                    }
                }
            }
        }

        return reachableWeights
            .filter { it > 0.0 }
            .minByOrNull { abs(it - target) } ?: 0.0
    }
}