package com.github.surpsg.diffcoverage.services.gradle

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.properties.DIFF_COVERAGE_COLLECT_INFO
import com.github.surpsg.diffcoverage.properties.DIFF_COVERAGE_COLLECT_INFO_FAILED
import com.github.surpsg.diffcoverage.properties.MULTIPLE_DIFF_COVERAGE_ENTRIES
import com.github.surpsg.diffcoverage.properties.gradle.DIFF_COVERAGE_CONFIGURATION_PROPERTY
import com.github.surpsg.diffcoverage.properties.gradle.DIFF_COVERAGE_CONFIG_FILE_NAME
import com.github.surpsg.diffcoverage.properties.gradle.DIFF_COVERAGE_FAKE_TASK
import com.github.surpsg.diffcoverage.services.CacheService
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.build.SyncViewManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.jetbrains.rd.util.first
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.withContext
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

@Service
class GradleDiffCoveragePluginService(private val project: Project) {

    fun lookupDiffCoveragePluginModule(rootModulePath: String): Pair<String, String>? {
        val diffPluginAppliedTo: Map<String, Set<String>> = lookupDiffCoveragePluginModules()
        if (diffPluginAppliedTo.size > 1 || diffPluginAppliedTo.first().value.size > 1) {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                message = DiffCoverageBundle.message(MULTIPLE_DIFF_COVERAGE_ENTRIES)
            )
            return null
        }

        val diffCoverageModule = diffPluginAppliedTo.first().value.first()
        val diffCoverageModuleKey = normalizeModuleKey(rootModulePath, diffCoverageModule)
        return GradleUtil.findGradleModuleData(project, rootModulePath)
            ?.parent
            ?.let { ExternalSystemApiUtil.findAll(it, ProjectKeys.MODULE) }
            ?.find { it.data.id == diffCoverageModuleKey }
            ?.data
            ?.let { diffCoverageModule to it.linkedExternalProjectPath }
    }

    private fun normalizeModuleKey(rootModulePath: String, moduleKey: String): String {
        return if (moduleKey == ROOT_PROJECT_KEY) {
            Paths.get(rootModulePath).fileName.toString()
        } else {
            moduleKey
        }
    }

    private fun lookupDiffCoveragePluginModules(): Map<String, Set<String>> {
        val projectsWithDiffCoverPlugin = mutableMapOf<String, MutableSet<String>>()
        for (projectEntry in GradleExtensionsSettings.getInstance(project).projects) {
            for (moduleExtension in projectEntry.value.extensions) {
                if (moduleExtension.value.extensions.containsKey(DIFF_COVERAGE_CONFIGURATION_PROPERTY)) {
                    projectsWithDiffCoverPlugin.computeIfAbsent(projectEntry.key) {
                        mutableSetOf()
                    }.add(moduleExtension.key)
                }
            }
        }
        return projectsWithDiffCoverPlugin
    }

    suspend fun obtainCacheableDiffCoverageInfo(projectIdToPath: Pair<String, String>): DiffCoverageConfiguration? {
        return project.service<CacheService>().suspendableGetCached {
            withContext(BACKGROUND_SCOPE.coroutineContext) {
                collectDiffCoverageInfo(projectIdToPath).asDeferred().await()
            }
        }
    }

    private fun collectDiffCoverageInfo(
        projectIdToPath: Pair<String, String>
    ): CompletableFuture<DiffCoverageConfiguration?> {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            executionName = DiffCoverageBundle.message(DIFF_COVERAGE_COLLECT_INFO)
            externalProjectPath = projectIdToPath.second
            taskNames = listOf(DIFF_COVERAGE_FAKE_TASK)
            vmOptions = GradleSettings.getInstance(project).gradleVmOptions
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
        }
        val userData = UserDataHolderBase().apply {
            putUserData(
                GradleTaskManager.INIT_SCRIPT_KEY,
                createCollectDiffCoverageConfigScript(projectIdToPath.first)
            )
            putUserData(ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY, SyncViewManager::class.java)
        }

        return CompletableFuture<DiffCoverageConfiguration?>().apply {
            ExternalSystemUtil.runTask(
                settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
                object : TaskCallback {
                    override fun onSuccess() {
                        val diffContent = getDiffCoverageConfigFile(projectIdToPath.second)
                            .readText().replace("\\", "\\\\")
                        val diffCoverageInfo = jacksonObjectMapper().readValue<DiffCoverageConfiguration>(diffContent)
                        complete(diffCoverageInfo)
                    }

                    override fun onFailure() {
                        DiffCoverageBundle.message(DIFF_COVERAGE_COLLECT_INFO_FAILED).let {
                            project.service<BalloonNotificationService>().notify(
                                notificationType = NotificationType.ERROR,
                                message = it
                            )
                            complete(null)
                        }
                    }
                },
                ProgressExecutionMode.IN_BACKGROUND_ASYNC,
                false,
                userData
            )
        }
    }

    private fun getDiffCoverageConfigFile(moduleAbsolutePath: String): File {
        return Paths.get(moduleAbsolutePath, "build", DIFF_COVERAGE_CONFIG_FILE_NAME).toFile()
    }

    private fun createCollectDiffCoverageConfigScript(gradlePath: String): String {
        val varSign = "$"
        return """
            allprojects {
                afterEvaluate { project ->
                    if(project.path == '$gradlePath') {
                        def execFiles = project.diffCoverageReport.jacocoExecFiles.collect{ '"' + it +'"' }.join(',')
                        def classes = project.diffCoverageReport.classesDirs.collect{ '"' + it +'"' }.join(',')
                        def reportsRoot = project.file(project.diffCoverageReport.reportConfiguration.baseReportDir)
                        new File(project.buildDir, "$DIFF_COVERAGE_CONFIG_FILE_NAME").write ""${'"'}
                            {
                                "project": "$varSign{project.name}",
                                "projectDir": "$varSign{project.projectDir}",
                                "execFiles": [ $varSign{execFiles} ],
                                "classes": [ $varSign{classes} ],
                                "reportsRoot": "$varSign{reportsRoot}"
                            }
                        ""${'"'}
                    }
                }
            }
            """
    }

    companion object {
        const val ROOT_PROJECT_KEY = ":"
    }
}
