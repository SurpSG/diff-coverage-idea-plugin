package com.github.surpsg.diffcoverage.actions

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

        val settings: GradleProjectSettings = GradleSettings.getInstance(project).linkedProjectsSettings.first()

        val diffCoveragePluginService = project.service<GradleDiffCoveragePluginService>()
        val diffCoverageModule = diffCoveragePluginService.lookupDiffCoveragePluginModule(settings.externalProjectPath)
            ?: return

        BACKGROUND_SCOPE.launch {
            val coverageInfo = diffCoveragePluginService.obtainCacheableDiffCoverageInfo(diffCoverageModule)
            project.service<GradleService>().executeGradleTask(DIFF_COVERAGE_TASK, diffCoverageModule.second) {
                showDiffCoverageReportNotification(coverageInfo, project)
            }
            project.service<CoverageVizualizeService>().showCoverage(coverageInfo)
        }
    }

    private fun showDiffCoverageReportNotification(diffCoverageInfo: DiffCoverageConfiguration, project: Project) {
        val reportUrl = Paths.get(diffCoverageInfo.reportsRoot, HTML_REPORT_RELATIVE_PATH).toUri().toString()
        project.service<BalloonNotificationService>().notify(
            notificationListener = NotificationListener.URL_OPENING_LISTENER,
            message = DiffCoverageBundle.message(REPORT_LINK, reportUrl)
        )
    }

    override fun isDumbAware(): Boolean = false
}
