package com.github.surpsg.diffcoverage.services.gradle

import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.action.ExternalSystemActionUtil
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.task.TaskData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.settings.GradleSettings
import java.util.concurrent.CompletableFuture

@Service
class GradleService(private val project: Project) {

    fun isGradleProject(): Boolean {
        return !GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()
    }

    suspend fun suspendableExecuteGradleTask(
        taskName: String,
        modulePath: String
    ): Boolean = withContext(BACKGROUND_SCOPE.coroutineContext) {
        val result = CompletableFuture<Boolean>()
        executeGradleTask(taskName, modulePath, object : TaskCallback {
            override fun onSuccess() {
                result.complete(true)
            }

            override fun onFailure() {
                result.complete(false)
            }
        })
        result.asDeferred().await()
    }

    private fun executeGradleTask(
        taskName: String,
        modulePath: String,
        taskCallback: TaskCallback
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
            taskCallback,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            false
        )
    }
}
