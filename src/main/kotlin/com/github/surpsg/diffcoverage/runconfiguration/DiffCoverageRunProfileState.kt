package com.github.surpsg.diffcoverage.runconfiguration

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.domain.gradle.GradleModule
import com.github.surpsg.diffcoverage.properties.NOT_GRADLE_PROJECT
import com.github.surpsg.diffcoverage.properties.REPORT_LINK
import com.github.surpsg.diffcoverage.properties.REPORT_LINK_WITH_ERROR
import com.github.surpsg.diffcoverage.properties.gradle.HTML_REPORT_RELATIVE_PATH
import com.github.surpsg.diffcoverage.services.CoverageVizualizeService
import com.github.surpsg.diffcoverage.services.gradle.GradleDiffCoveragePluginSettingsService
import com.github.surpsg.diffcoverage.services.gradle.GradleDiffCoverageRunService
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
            launch {
                buildCoverageReportByGradlePlugin(module, coverageInfo)
            }
            buildReportIde(coverageInfo)
        }
    }

    private fun buildReportIde(coverageInfo: DiffCoverageConfiguration) {
        if (diffCoverageRunConfiguration.buildReportByIde) {
            project.service<CoverageVizualizeService>().showCoverage(coverageInfo)
        }
    }

    private suspend fun buildCoverageReportByGradlePlugin(
        gradleModule: GradleModule,
        coverageInfo: DiffCoverageConfiguration
    ) {
        val successExecute = project.service<GradleDiffCoverageRunService>().runDiffCoverageTask(gradleModule)
        val messageKeyToNotificationType = if (successExecute) {
            REPORT_LINK to NotificationType.INFORMATION
        } else {
            REPORT_LINK_WITH_ERROR to NotificationType.ERROR
        }
        showDiffCoverageReportNotification(coverageInfo, messageKeyToNotificationType)
    }

    private fun showDiffCoverageReportNotification(
        diffCoverageInfo: DiffCoverageConfiguration,
        messageKeyToNotificationType: Pair<String, NotificationType>
    ) {
        val reportUrl = Paths.get(diffCoverageInfo.reportsRoot, HTML_REPORT_RELATIVE_PATH).toUri().toString()
        project.service<BalloonNotificationService>().notify(
            messageKeyToNotificationType.second,
            notificationListener = NotificationListener.URL_OPENING_LISTENER,
            message = DiffCoverageBundle.message(messageKeyToNotificationType.first, reportUrl)
        )
    }
}
