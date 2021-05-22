package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.domain.CoverageStat
import com.github.surpsg.diffcoverage.domain.CoverageSuiteBundleWithStat
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.properties.NO_COVERAGE_FILES
import com.github.surpsg.diffcoverage.services.coverage.CoverageBundleService
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.coverage.CoverageDataManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service
class CoverageVizualizeService(private val project: Project) {

    fun showCoverage(coverageInfo: DiffCoverageConfiguration): CoverageStat? {
        val coverageBundleService = project.service<CoverageBundleService>()
        val coverage: CoverageSuiteBundleWithStat? = coverageBundleService.buildCoverageSuiteBundle(coverageInfo)
        return if (coverage == null) {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                message = DiffCoverageBundle.message(NO_COVERAGE_FILES)
            )
            null
        } else {
            ApplicationManager.getApplication().invokeLater {
                CoverageDataManager.getInstance(project).chooseSuitesBundle(coverage)
            }
            return coverage.coverageData.coverageStat
        }
    }
}
