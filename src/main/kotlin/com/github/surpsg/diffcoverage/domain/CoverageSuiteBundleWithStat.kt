package com.github.surpsg.diffcoverage.domain

import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.CoverageSuitesBundle

class CoverageSuiteBundleWithStat(suite: CoverageSuite) : CoverageSuitesBundle(suite) {

    override fun getCoverageData(): ProjectDataWithStat {
        return suites.asSequence()
            .map { it.getCoverageData(null) }
            .map { it as? ProjectDataWithStat }
            .filterNotNull()
            .first()
            .apply {
                super.getCoverageData()?.let {
                    merge(it)
                }
            }
    }
}
