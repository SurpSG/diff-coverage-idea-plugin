package com.github.surpsg.diffcoverage.domain

import org.jacoco.core.analysis.ICounter
import org.jacoco.core.analysis.ICoverageNode
import org.jacoco.core.internal.analysis.CounterImpl

class CoverageStat(
    counters: Map<ICoverageNode.CounterEntity, ICounter>
) {
    private val counters: MutableMap<ICoverageNode.CounterEntity, ICounter> = HashMap(counters)

    val linesCoverage: Double
        get() = getCoverage(ICoverageNode.CounterEntity.LINE).coveredRatio.toPercents()
    val branchesCoverage: Double
        get() = getCoverage(ICoverageNode.CounterEntity.BRANCH).coveredRatio.toPercents()
    val instructionsCoverage: Double
        get() = getCoverage(ICoverageNode.CounterEntity.INSTRUCTION).coveredRatio.toPercents()

    fun getCoverage(coverageType: ICoverageNode.CounterEntity): ICounter {
        return counters[coverageType] ?: createEmptyCounter()
    }

    fun merge(coverageStat: CoverageStat) {
        coverageStat.counters.forEach { (type, counter) ->
            mergeCounter(type, counter)
        }
    }

    private fun mergeCounter(
        type: ICoverageNode.CounterEntity,
        counter: ICounter
    ) {
        val currentCounter = counters.computeIfAbsent(type) {
            createEmptyCounter()
        }
        counters[type] = CounterImpl.getInstance(
            currentCounter.missedCount + counter.missedCount,
            currentCounter.coveredCount + counter.coveredCount
        )
    }

    private fun createEmptyCounter() = CounterImpl.getInstance(0, 0)

    private fun Double.toPercents(): Double = this * PERCENT_MULTIPLIER

    private companion object {
        const val PERCENT_MULTIPLIER = 100
    }
}
