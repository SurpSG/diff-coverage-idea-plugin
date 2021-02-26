package com.github.surpsg.diffcoverage.actions

import com.github.surpsg.diffcoverage.services.GradleDiffCoveragePluginService
import com.github.surpsg.diffcoverage.services.ModifiedCodeService
import com.github.surpsg.diffcoverage.services.GradleService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.nio.file.Paths

class RunDiffCoverageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!service<GradleService>().isGradleProject(project)) {
            Messages.showMessageDialog(project, "" +
                    "Not a Gradle project", "DiffCoverage plugin", Messages.getWarningIcon()
            )
            return
        }

        service<ModifiedCodeService>().collectModifiedCode(project).forEach {
            println(it.name)
        }

        service<GradleService>().apply {
            val rootModulePath: String = GradleSettings.getInstance(project).linkedProjectsSettings.first().externalProjectPath
            val diffCoverageModuleAbsolutePath = service<GradleDiffCoveragePluginService>()
                .lookupDiffCoveragePluginModulePath(project, rootModulePath) ?: return
            executeGradleTask(project, "test", diffCoverageModuleAbsolutePath) {
                executeGradleTask(project, "diffCoverage", diffCoverageModuleAbsolutePath) {
                    showDiffCoverageReportNotification(diffCoverageModuleAbsolutePath, project)
                }
            }
        }
    }

    private fun showDiffCoverageReportNotification(diffCoverageModuleAbsolutePath: String, project: Project) {
        val defaultDiffCoverageReportPath = "build/reports/jacoco/diffCoverage/html/index.html"
        val reportUrl = Paths.get(diffCoverageModuleAbsolutePath, defaultDiffCoverageReportPath).toUri().toString()

        val reportLink = """<a href="$reportUrl">Diff coverage report</a>""".trimIndent()
        Notifications.Bus.notify(
            Notification(
                "",
                "Diff Coverage",
                reportLink,
                NotificationType.INFORMATION,
                NotificationListener.URL_OPENING_LISTENER
            ),
            project
        )
    }

    override fun isDumbAware(): Boolean = false
}
