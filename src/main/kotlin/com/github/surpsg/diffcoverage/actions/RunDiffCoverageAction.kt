package com.github.surpsg.diffcoverage.actions

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.properties.NOT_GRADLE_PROJECT
import com.github.surpsg.diffcoverage.properties.REPORT_LINK
import com.github.surpsg.diffcoverage.properties.REPORT_LINK_WITH_ERROR
import com.github.surpsg.diffcoverage.properties.gradle.DIFF_COVERAGE_TASK
import com.github.surpsg.diffcoverage.properties.gradle.HTML_REPORT_RELATIVE_PATH
import com.github.surpsg.diffcoverage.services.CoverageVizualizeService
import com.github.surpsg.diffcoverage.services.gradle.GradleDiffCoveragePluginService
import com.github.surpsg.diffcoverage.services.gradle.GradleService
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.nio.file.Paths

class RunDiffCoverageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!project.service<GradleService>().isGradleProject()) {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                message = DiffCoverageBundle.message(NOT_GRADLE_PROJECT)
            )
            return
        }

        val diffCoveragePluginService = project.service<GradleDiffCoveragePluginService>()
        val diffCoverageModule = diffCoveragePluginService.lookupDiffCoveragePluginModule() ?: return

        BACKGROUND_SCOPE.launch {
            diffCoveragePluginService.obtainCacheableDiffCoverageInfo(diffCoverageModule)?.let { coverageInfo ->
                launch {
                    project.service<CoverageVizualizeService>().showCoverage(coverageInfo)
                }
                buildCoverageReportByGradlePlugin(project, diffCoverageModule.second, coverageInfo)
            }
        }
    }

    private suspend fun buildCoverageReportByGradlePlugin(
        project: Project,
        modulePath: String,
        coverageInfo: DiffCoverageConfiguration
    ) {
        val successExecute = project.service<GradleService>().suspendableExecuteGradleTask(
            DIFF_COVERAGE_TASK,
            modulePath
        )
        val messageKeyToNotificationType = if (successExecute) {
            REPORT_LINK to NotificationType.INFORMATION
        } else {
            REPORT_LINK_WITH_ERROR to NotificationType.ERROR
        }
        showDiffCoverageReportNotification(project, coverageInfo, messageKeyToNotificationType)
    }

    private fun showDiffCoverageReportNotification(
        project: Project,
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

    override fun isDumbAware(): Boolean = false
}
