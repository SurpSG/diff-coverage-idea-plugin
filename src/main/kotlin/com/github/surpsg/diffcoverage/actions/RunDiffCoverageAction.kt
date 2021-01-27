package com.github.surpsg.diffcoverage.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.rd.util.first
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.nio.file.Paths

class RunDiffCoverageAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val linkedProjectsSettings = GradleSettings.getInstance(project).linkedProjectsSettings
        if (linkedProjectsSettings.isEmpty()) {
            Messages.showMessageDialog(
                    project,
                    "Not a Gradle project",
                    "DiffCoverage plugin",
                    Messages.getWarningIcon()
            )
            return
        }

        val rootModulePath: String = linkedProjectsSettings.first().externalProjectPath
        val diffCoverageModuleAbsolutePath = lookupDiffCoveragePluginModulePath(project, rootModulePath) ?: return

        executeGradleTask(project, "test", diffCoverageModuleAbsolutePath) {
            executeGradleTask(project, "diffCoverage", diffCoverageModuleAbsolutePath) {
                showDiffCoverageReportNotification(diffCoverageModuleAbsolutePath, project)
            }
        }

        ExternalProjectsManager.getInstance(project).runWhenInitialized {
//            if (diffCoveragePluginAppliedTo.isNotEmpty()) {
//                Notifications.Bus.notify(
//                        Notification(
//                                "Repository",
//                                "Diff Coverage",
//                                diffCoveragePluginAppliedTo.asSequence().joinToString("\n") {
//                                    "DiffCoverage is applied to: \n ${it.key}\n\t${it.value.joinToString("\n\t")}"
//                                },
//                                NotificationType.INFORMATION
//                        ),
//                        project
//                )
//            } else {
//                val statusBar = WindowManager.getInstance().getStatusBar(project)
//                JBPopupFactory.getInstance()
//                        .createHtmlTextBalloonBuilder(
//                                "DiffCoverage is not applied. Visit https://github.com/form-com/diff-coverage-gradle",
//                                MessageType.ERROR
//                        ) {}
//                        .setHideOnKeyOutside(true)
//                        .createBalloon()
//                        .show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.atRight)
//            }
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

    private fun executeGradleTask(
            project: Project,
            taskName: String,
            modulePath: String,
            successCallback: () -> Unit
    ) {
        val projectSystemId = ProjectSystemId("GRADLE")
        val taskExecutionInfo = ExternalSystemActionUtil.buildTaskInfo(
                TaskData(
                        projectSystemId,
                        taskName,
                        modulePath,
                        ""
                )
        )
        ExternalSystemUtil.runTask(
                taskExecutionInfo.settings,
                taskExecutionInfo.executorId,
                project,
                projectSystemId,
                object : TaskCallback {
                    override fun onSuccess() {
                        successCallback()
                    }

                    override fun onFailure() {
                        println("task $taskName failed")
                    }
                },
                ProgressExecutionMode.IN_BACKGROUND_ASYNC
        )
    }


    private fun lookupDiffCoveragePluginModulePath(project: Project, rootModulePath: String): String? {
        val diffCoveragePluginAppliedTo: Map<String, Set<String>> = lookupDiffCoveragePluginModules(project)
        if (diffCoveragePluginAppliedTo.size > 1 || diffCoveragePluginAppliedTo.first().value.size > 1) {
            return null
        }

        val diffCoverageModuleKey = diffCoveragePluginAppliedTo.first().value.first()
        return GradleUtil.findGradleModuleData(project, rootModulePath)
                ?.parent
                ?.let { ExternalSystemApiUtil.findAll(it, ProjectKeys.MODULE) }
                ?.find { it.data.id == diffCoverageModuleKey }
                ?.data
                ?.linkedExternalProjectPath
    }

    private fun lookupDiffCoveragePluginModules(project: Project): Map<String, Set<String>> {
        val projectsWithDiffCoverPlugin = mutableMapOf<String, MutableSet<String>>()
        for (projectEntry in GradleExtensionsSettings.getInstance(project).projects) {
            for (moduleExtension in projectEntry.value.extensions) {
                if (moduleExtension.value.extensions.containsKey("diffCoverageReport")) {
                    projectsWithDiffCoverPlugin.computeIfAbsent(projectEntry.key) {
                        mutableSetOf()
                    }.add(moduleExtension.key)
                }
            }
        }
        return projectsWithDiffCoverPlugin
    }

    override fun isDumbAware(): Boolean {
        return false
    }
}
