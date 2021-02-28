package com.github.surpsg.diffcoverage.actions

import com.github.surpsg.diffcoverage.services.GradleDiffCoveragePluginService
import com.github.surpsg.diffcoverage.services.ModifiedCodeService
import com.github.surpsg.diffcoverage.services.GradleService
import com.github.surpsg.diffcoverage.services.ModifiedCodeTestUsageService
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.SuperMethodsSearch
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.nio.file.Paths

class RunDiffCoverageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        if (!project.service<GradleService>().isGradleProject()) {
            Messages.showMessageDialog(
                project, "" +
                        "Not a Gradle project", "DiffCoverage plugin", Messages.getWarningIcon()
            )
            return
        }

        project.service<ModifiedCodeTestUsageService>().collectUsages()

//        project.service<GradleService>().apply {
//            val rootModulePath: String = GradleSettings.getInstance(project).linkedProjectsSettings.first().externalProjectPath
//            val diffCoverageModuleAbsolutePath = project.service<GradleDiffCoveragePluginService>()
//                .lookupDiffCoveragePluginModulePath(rootModulePath) ?: return
//            executeGradleTask("test", diffCoverageModuleAbsolutePath) {
//                executeGradleTask("diffCoverage", diffCoverageModuleAbsolutePath) {
//                    showDiffCoverageReportNotification(diffCoverageModuleAbsolutePath, project)
//                }
//            }
//        }
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
