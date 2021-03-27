package com.github.surpsg.diffcoverage.runconfiguration

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.properties.NOT_GRADLE_PROJECT
import com.github.surpsg.diffcoverage.properties.REPORT_LINK
import com.github.surpsg.diffcoverage.properties.gradle.DIFF_COVERAGE_TASK
import com.github.surpsg.diffcoverage.properties.gradle.HTML_REPORT_RELATIVE_PATH
import com.github.surpsg.diffcoverage.services.CoverageVizualizeService
import com.github.surpsg.diffcoverage.services.gradle.GradleDiffCoveragePluginService
import com.github.surpsg.diffcoverage.services.gradle.GradleService
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ProgramRunner
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import java.nio.file.Paths

class DiffCoverageRunProfileState(
    private val project: Project,
    private val diffCoverageRunConfiguration: DiffCoverageRunConfiguration
) : RunProfileState {

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? {
        if (!project.service<GradleService>().isGradleProject()) {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                message = DiffCoverageBundle.message(NOT_GRADLE_PROJECT)
            )
            return null
        }

        val diffCoveragePluginService = project.service<GradleDiffCoveragePluginService>()
        val diffCoverageModule = diffCoveragePluginService.lookupDiffCoveragePluginModule() ?: return null

        BACKGROUND_SCOPE.launch {
            diffCoveragePluginService.obtainCacheableDiffCoverageInfo(diffCoverageModule)?.let { coverageInfo ->
                buildReportByGradleDiffCoveragePlugin(coverageInfo, diffCoverageModule.second)
                buildReportIde(coverageInfo)
            }
        }
        return null
    }

    private fun buildReportIde(coverageInfo: DiffCoverageConfiguration) {
        if (diffCoverageRunConfiguration.buildReportByIde) {
            project.service<CoverageVizualizeService>().showCoverage(coverageInfo)
        }
    }

    private fun buildReportByGradleDiffCoveragePlugin(coverageInfo: DiffCoverageConfiguration, projectPath: String) {
        if (diffCoverageRunConfiguration.buildReportByGradlePlugin) {
            project.service<GradleService>().executeGradleTask(DIFF_COVERAGE_TASK, projectPath) {
                showDiffCoverageReportNotification(coverageInfo, project)
            }
        }
    }

    private fun showDiffCoverageReportNotification(diffCoverageInfo: DiffCoverageConfiguration, project: Project) {
        val reportUrl = Paths.get(diffCoverageInfo.reportsRoot, HTML_REPORT_RELATIVE_PATH).toUri().toString()
        project.service<BalloonNotificationService>().notify(
            notificationListener = NotificationListener.URL_OPENING_LISTENER,
            message = DiffCoverageBundle.message(REPORT_LINK, reportUrl)
        )
    }
}
