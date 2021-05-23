package com.github.surpsg.diffcoverage.runconfiguration

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.domain.CoverageStat
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.properties.NOT_GRADLE_PROJECT
import com.github.surpsg.diffcoverage.services.CoverageVizualizeService
import com.github.surpsg.diffcoverage.services.gradle.GradleDiffCoveragePluginSettingsService
import com.github.surpsg.diffcoverage.services.gradle.GradleDiffCoverageRunService
import com.github.surpsg.diffcoverage.services.gradle.GradleService
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jacoco.core.analysis.ICoverageNode

class DiffCoverageRunProfileState(
    private val project: Project,
    private val diffCoverageRunConfiguration: DiffCoverageRunConfiguration
) : RunProfileState {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? {
        if (project.service<GradleService>().isGradleProject()) {
            buildDiffCoverage()
        } else {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                message = DiffCoverageBundle.message(NOT_GRADLE_PROJECT)
            )
        }
        return null
    }

    private fun buildDiffCoverage() = BACKGROUND_SCOPE.launch {
        val module = project.service<GradleDiffCoveragePluginSettingsService>().lookupDiffCoveragePluginModule()
            ?: return@launch
        project.service<GradleDiffCoverageRunService>().obtainCacheableDiffCoverageInfo(module)?.let { coverageInfo ->
            showCoverageReportNotification(coverageInfo)
        }
    }

    private fun showCoverageReportNotification(coverageInfo: DiffCoverageConfiguration) {
        val coverageStat: CoverageStat = project.service<CoverageVizualizeService>().showCoverage(coverageInfo)
            ?: return

        val coverageHasViolations = getCoverageViolations(coverageStat).values.contains(true)
        val (notificationType, title) = getIconToMessageKey(coverageHasViolations)
        project.service<BalloonNotificationService>().notify(
            title = title,
            notificationType = notificationType,
            message = DiffCoverageBundle.message(
                "coverage.stat.success.message",
                coverageStat.linesCoverage.asPercentsText(),
                coverageStat.branchesCoverage.asPercentsText(),
                coverageStat.instructionsCoverage.asPercentsText()
            )
        )
    }

    private fun getIconToMessageKey(coverageHasViolations: Boolean) = if (coverageHasViolations) {
        NotificationType.ERROR to DiffCoverageBundle.message(
            "coverage.stat.fail.title",
            diffCoverageRunConfiguration.minCoveragePercents
        )
    } else {
        NotificationType.INFORMATION to DiffCoverageBundle.message("coverage.stat.success.title")
    }

    private fun getCoverageViolations(coverageStat: CoverageStat): Map<ICoverageNode.CounterEntity, Boolean> {
        return sequenceOf(
            ICoverageNode.CounterEntity.LINE,
            ICoverageNode.CounterEntity.BRANCH,
            ICoverageNode.CounterEntity.INSTRUCTION,
        ).map {
            it to coverageStat.getCoverage(it).coveredRatio
        }.filter { (_, coverageRation) ->
            !coverageRation.isNaN()
        }.map { (coverageType, coverageRatio) ->
            coverageType to (coverageRatio.toPercents() < diffCoverageRunConfiguration.minCoveragePercents)
        }.toMap()
    }

    private fun Double.toPercents(): Int = this.toInt() * TO_PERCENT_MULTIPLIER

    private fun Double.asPercentsText(): String = if (isNaN()) {
        NOT_AVAILABLE_COVERAGE
    } else {
        "$this%"
    }

    private companion object {
        const val TO_PERCENT_MULTIPLIER = 100
        const val NOT_AVAILABLE_COVERAGE = "N/A"
    }
}
