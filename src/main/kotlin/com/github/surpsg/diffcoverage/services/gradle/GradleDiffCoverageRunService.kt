package com.github.surpsg.diffcoverage.services.gradle

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.domain.gradle.GradleModule
import com.github.surpsg.diffcoverage.domain.gradle.GradleTaskWithInitScript
import com.github.surpsg.diffcoverage.domain.gradle.RunnableGradleTask
import com.github.surpsg.diffcoverage.properties.DIFF_COVERAGE_COLLECT_INFO
import com.github.surpsg.diffcoverage.properties.gradle.DIFF_COVERAGE_FAKE_TASK
import com.github.surpsg.diffcoverage.services.CacheService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Paths

@Service
class GradleDiffCoverageRunService(private val project: Project) {

    suspend fun obtainCacheableDiffCoverageInfo(gradleModule: GradleModule): DiffCoverageConfiguration? {
        return project.service<CacheService>().suspendableGetCached {
            withContext(BACKGROUND_SCOPE.coroutineContext) {
                collectDiffCoverageInfo(gradleModule)
            }
        }
    }

    private suspend fun collectDiffCoverageInfo(gradleModule: GradleModule): DiffCoverageConfiguration? {
        val successfulExecute = project.service<GradleService>().executeTaskWithInitScript(
            GradleTaskWithInitScript(
                RunnableGradleTask(
                    taskName = DIFF_COVERAGE_FAKE_TASK,
                    taskDescription = DiffCoverageBundle.message(DIFF_COVERAGE_COLLECT_INFO),
                    gradleModule = gradleModule
                ),
                initScript = createCollectDiffCoverageConfigScript(gradleModule.name)
            )
        )
        return if (successfulExecute) {
            val diffInfoJson = getDiffCoverageConfigFile(gradleModule.absolutePath)
                .readText()
                .replace("\\", "\\\\")
            jacksonObjectMapper().readValue<DiffCoverageConfiguration>(diffInfoJson)
        } else {
            null
        }
    }

    private fun getDiffCoverageConfigFile(moduleAbsolutePath: String): File {
        // TODO use some another path outside the project. `.idea`?
        return Paths.get(moduleAbsolutePath, "build") // TODO extract const property
            .apply { toFile().mkdir() }
            .resolve(DIFF_COVERAGE_CONFIG_FILE_NAME)
            .toFile()
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

    private companion object {
        const val DIFF_COVERAGE_CONFIG_FILE_NAME = "diffCoverage.json"
    }
}
