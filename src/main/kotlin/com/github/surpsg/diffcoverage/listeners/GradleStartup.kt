package com.github.surpsg.diffcoverage.listeners

import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleUtil

internal class GradleStartup : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
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
    }


}
