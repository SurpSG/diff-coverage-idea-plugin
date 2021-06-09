package com.github.surpsg.diffcoverage.domain

import com.intellij.rt.coverage.data.CoverageData
import com.intellij.rt.coverage.data.ProjectData

class ProjectDataWithStat(
    val coverageStat: CoverageStat
) : ProjectData() {

    override fun merge(data: CoverageData) {
        super.merge(data)
        if (data is ProjectDataWithStat) {
            coverageStat.merge(data.coverageStat)
        }
    }
}
