package com.github.surpsg.diffcoverage.services.gradle

import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
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
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.util.concurrent.CompletableFuture

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
        return executeTask(gradleTask.gradleTask, userData)
    }

    private suspend fun executeTask(
        gradleTask: RunnableGradleTask,
        userData: UserDataHolderBase
    ): Boolean = withContext(BACKGROUND_SCOPE.coroutineContext) {
        CompletableFuture<Boolean>().apply {
            executeGradleTask(
                gradleTask,
                userData,
                buildTaskCallbackFromFuture(this)
            )
        }.asDeferred().await()
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

    private fun buildTaskCallbackFromFuture(result: CompletableFuture<Boolean>) = object : TaskCallback {
        override fun onSuccess() {
            result.complete(true)
        }

        override fun onFailure() {
            result.complete(false)
        }
    }
}
