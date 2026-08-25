package com.makhp.pelukdiri.core.domain.engine

import com.makhp.pelukdiri.core.domain.model.DeviationConfig
import org.junit.Test

class DeviationSensitivityTest {

    private fun generateProfiles(): List<Profile> {
        val profiles = mutableListOf<Profile>()
        
        // 1. Highly Consistent (M=0)
        profiles.add(Profile("Consistent 2h", List(14) { 120.0 }, listOf(120.0, 121.0, 150.0, 180.0, 240.0)))
        
        // 2. Typical Moderate Fluctuation (M=15)
        profiles.add(Profile("Moderate 2h (M=15)", doubled(100.0, 105.0, 120.0, 120.0, 120.0, 135.0, 140.0), listOf(120.0, 135.0, 180.0, 240.0, 360.0)))
        
        // 3. High Fluctuation (M=60)
        profiles.add(Profile("High 3h (M=60)", doubled(60.0, 120.0, 180.0, 180.0, 240.0, 300.0, 180.0), listOf(180.0, 240.0, 300.0, 360.0, 480.0, 600.0)))
        
        // 4. Low Baseline (B=30, M=5)
        profiles.add(Profile("Low 30m (M=5)", doubled(25.0, 30.0, 35.0, 30.0, 25.0, 35.0, 30.0), listOf(30.0, 45.0, 60.0, 90.0, 120.0)))

        // 5. High Baseline (B=480, M=30)
        profiles.add(Profile("High 8h (M=30)", doubled(450.0, 480.0, 510.0, 480.0, 450.0, 510.0, 480.0), listOf(480.0, 540.0, 600.0, 720.0, 960.0)))

        return profiles
    }

    data class Profile(val name: String, val history: List<Double>, val testUsages: List<Double>)

    private fun doubled(vararg values: Double) = values.flatMap { listOf(it, it) }

    @Test
    fun runAllSensitivityAnalysis() {
        val out = StringBuilder()
        val profiles = generateProfiles()

        // 1. Alpha Sensitivity
        out.append("=== ALPHA SENSITIVITY (k=1.0, s0=1.0) ===\n")
        listOf(0.0, 0.05, 0.1, 0.2, 0.5, 1.0).forEach { alpha ->
            val engine = DeviationEngine(DeviationConfig(alpha = alpha, k = 1.0, s0 = 1.0))
            out.append("\nAlpha: $alpha\n")
            profiles.forEach { profile ->
                val results = profile.testUsages.map { usage -> engine.calculate(usage, profile.history) }
                val dAvg = results.mapNotNull { it.deviation }.average()
                val sMax = results.mapNotNull { it.signal }.maxOrNull() ?: 0.0
                out.append(String.format("  %-20s | Avg D: %.3f | Max S: %7.2f\n", profile.name, dAvg, sMax))
            }
        }

        // 2. K Sensitivity
        out.append("\n=== K SENSITIVITY (alpha=0.1, s0=1.0) ===\n")
        listOf(0.25, 0.5, 1.0, 2.0, 4.0).forEach { k ->
            val engine = DeviationEngine(DeviationConfig(alpha = 0.1, k = k, s0 = 1.0))
            out.append("\nk: $k\n")
            profiles.forEach { profile ->
                val results = profile.testUsages.map { usage -> engine.calculate(usage, profile.history) }
                val dList = results.mapNotNull { it.deviation }
                val dAvg = dList.average()
                val countSaturated = dList.count { it >= 0.9 }
                out.append(String.format("  %-20s | Avg D: %.3f | Saturated: %d/%d\n", profile.name, dAvg, countSaturated, dList.size))
            }
        }

        // 3. S0 Sensitivity
        out.append("\n=== S0 SENSITIVITY (alpha=0.1, k=1.0) ===\n")
        listOf(0.25, 0.5, 1.0, 1.5, 2.0, 3.0).forEach { s0 ->
            val engine = DeviationEngine(DeviationConfig(alpha = 0.1, k = 1.0, s0 = s0))
            out.append("\ns0: $s0\n")
            profiles.forEach { profile ->
                val results = profile.testUsages.map { usage -> engine.calculate(usage, profile.history) }
                val dList = results.mapNotNull { it.deviation }
                val dAvg = dList.average()
                val countZero = dList.count { it < 0.1 }
                out.append(String.format("  %-20s | Avg D: %.3f | Low Dev (<0.1): %d/%d\n", profile.name, dAvg, countZero, dList.size))
            }
        }

        // 4. Joint Analysis
        out.append("\n=== JOINT ANALYSIS ===\n")
        listOf(
            Config(0.1, 1.0, 1.0, "Balanced Default"),
            Config(0.05, 0.5, 2.0, "Conservative"),
            Config(0.2, 1.5, 0.5, "Aggressive"),
            Config(0.1, 1.0, 2.0, "Moderate Delay")
        ).forEach { config ->
            val engine = DeviationEngine(DeviationConfig(alpha = config.alpha, k = config.k, s0 = config.s0))
            out.append("\nConfig: ${config.label} (α=${config.alpha}, k=${config.k}, s0=${config.s0})\n")
            
            val allD = mutableListOf<Double>()
            profiles.forEach { profile ->
                val dList = profile.testUsages.map { usage -> engine.calculate(usage, profile.history).deviation!! }
                allD.addAll(dList)
            }
            
            val p10 = allD.count { it < 0.1 } * 100 / allD.size
            val p50 = allD.count { it in 0.25..0.75 } * 100 / allD.size
            val p90 = allD.count { it > 0.9 } * 100 / allD.size
            out.append("  Distribution: <10%: $p10% | 25-75%: $p50% | >90%: $p90%\n")
        }

        // org.junit.Assert.fail(out.toString()) // Removed intentional failure for log output
        println(out.toString())
    }

    data class Config(val alpha: Double, val k: Double, val s0: Double, val label: String)
}
