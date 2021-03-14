package com.github.surpsg.diffcoverage.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.intellij.build.SyncViewManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.components.Service
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
        val diffCoveragePluginAppliedTo: Map<String, Set<String>> = lookupDiffCoveragePluginModules()
        if (diffCoveragePluginAppliedTo.size > 1 || diffCoveragePluginAppliedTo.first().value.size > 1) {
            return null
        }

        val diffCoverageModuleKey = diffCoveragePluginAppliedTo.first().value.first()
        return GradleUtil.findGradleModuleData(project, rootModulePath)
            ?.parent
            ?.let { ExternalSystemApiUtil.findAll(it, ProjectKeys.MODULE) }
            ?.find { it.data.id == diffCoverageModuleKey }
            ?.data
            ?.let{ it.id to it.linkedExternalProjectPath }
    }

    private fun lookupDiffCoveragePluginModules(): Map<String, Set<String>> {
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

    fun collectDiffCoverageInfo(projectIdToPath: Pair<String, String>): CompletableFuture<DiffCoverageConfiguration> {
        val settings = ExternalSystemTaskExecutionSettings().apply {
            executionName = DIFF_COVERAGE_INFO_COLLECT_PROCESS
            externalProjectPath = projectIdToPath.second
            taskNames = listOf("projects")
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

        return CompletableFuture<DiffCoverageConfiguration>().apply {
            ExternalSystemUtil.runTask(
                settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
                object : TaskCallback {
                    override fun onSuccess() {
                        val diffFile = getDiffCoverageConfigFile(projectIdToPath.second)
                        val diffCoverageInfo = jacksonObjectMapper().readValue<DiffCoverageConfiguration>(diffFile)
                        println(diffCoverageInfo)
                        complete(diffCoverageInfo)
                    }

                    override fun onFailure() {
                        completeExceptionally(RuntimeException("'$DIFF_COVERAGE_INFO_COLLECT_PROCESS' was failed"))
                    }
                },
                ProgressExecutionMode.IN_BACKGROUND_ASYNC,
                false,
                userData
            )
        }
    }

    fun getDiffCoverageConfigFile(moduleAbsolutePath: String): File {
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
                                "project": "${varSign}{project.name}",
                                "projectDir": "${varSign}{project.projectDir}",
                                "execFiles": [ ${varSign}execFiles ],
                                "classes": [ ${varSign}classes ],
                                "reportsRoot": "${varSign}reportsRoot"
                                
                            }
                        ""${'"'}
                    }
                }
            }
            """
    }

    companion object {
        const val DIFF_COVERAGE_CONFIG_FILE_NAME = "diffCoverage.json"
        const val DIFF_COVERAGE_INFO_COLLECT_PROCESS = "Collect diff coverage plugin info"
    }
}
