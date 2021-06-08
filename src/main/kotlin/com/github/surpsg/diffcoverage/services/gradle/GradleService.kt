package com.github.surpsg.diffcoverage.services.gradle

import com.github.surpsg.diffcoverage.domain.gradle.GradleTaskWithInitScript
import com.github.surpsg.diffcoverage.domain.gradle.RunnableGradleTask
import com.intellij.build.SyncViewManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

@Service
class GradleService(private val project: Project) {

    fun isGradleProject(): Boolean {
        return !GradleSettings.getInstance(project).linkedProjectsSettings.isEmpty()
    }

    suspend fun executeTaskWithInitScript(gradleTask: GradleTaskWithInitScript): Boolean {
        val userData: UserDataHolderBase = UserDataHolderBase().apply {
            putUserData(
                GradleTaskManager.INIT_SCRIPT_KEY,
                gradleTask.initScript
            )
            putUserData(ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY, SyncViewManager::class.java)
        }
        return suspendCoroutine {
            executeGradleTask(gradleTask.gradleTask, userData, continuationToTaskCallback(it))
        }
    }

    private fun executeGradleTask(
        gradleTask: RunnableGradleTask,
        userData: UserDataHolderBase?,
        taskCallback: TaskCallback
    ) {
        val settings: ExternalSystemTaskExecutionSettings = ExternalSystemTaskExecutionSettings().apply {
            executionName = gradleTask.taskDescription
            externalProjectPath = gradleTask.gradleModule.absolutePath
            taskNames = listOf(gradleTask.taskName)
            vmOptions = GradleSettings.getInstance(project).gradleVmOptions
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
        }
        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            taskCallback,
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            false,
            userData
        )
    }

    private fun continuationToTaskCallback(continuation: Continuation<Boolean>) = object : TaskCallback {
        override fun onSuccess() {
            continuation.resumeWith(Result.success(true))
        }

        override fun onFailure() {
            continuation.resumeWith(Result.success(false))
        }
    }
}
