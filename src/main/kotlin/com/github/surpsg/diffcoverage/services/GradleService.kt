package com.github.surpsg.diffcoverage.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.settings.GradleSettings

@Service
class GradleService(private val project: Project) {

    fun isGradleProject(): Boolean {
        return !GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()
    }

    fun executeGradleTask(
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
}
