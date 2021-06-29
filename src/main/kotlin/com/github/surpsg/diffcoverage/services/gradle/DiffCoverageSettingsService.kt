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
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import kotlinx.coroutines.withContext
import java.io.File

@Service
class DiffCoverageSettingsService(private val project: Project) {

    suspend fun obtainDiffCoverageSettings(silent: Boolean = false): DiffCoverageConfiguration? {
        val gradleModule: GradleModule? = project.service<GradleDiffCoverageModuleService>()
            .lookupDiffCoveragePluginModule(silent)
        return if (gradleModule == null) {
            null
        } else {
            withContext(BACKGROUND_SCOPE.coroutineContext) {
                collectDiffCoverageInfo(gradleModule)
            }
        }
    }

    private suspend fun collectDiffCoverageInfo(gradleModule: GradleModule): DiffCoverageConfiguration? {
        val tempDiffSettingsFile = createTemporaryDiffCoverageSettingsFile()
        val successfulExecute: Boolean = project.service<GradleService>().executeTaskWithInitScript(
            GradleTaskWithInitScript(
                RunnableGradleTask(
                    taskName = DIFF_COVERAGE_FAKE_TASK,
                    taskDescription = DiffCoverageBundle.message(DIFF_COVERAGE_COLLECT_INFO),
                    gradleModule = gradleModule
                ),
                initScript = createCollectDiffCoverageConfigScript(gradleModule.name, tempDiffSettingsFile)
            )
        )
        return if (successfulExecute) {
            readDiffCoverageSettingsFromFile(tempDiffSettingsFile)
        } else {
            null
        }
    }

    private fun readDiffCoverageSettingsFromFile(temporaryDiffCoverageSettingsFile: File): DiffCoverageConfiguration {
        return jacksonObjectMapper().readValue(
            temporaryDiffCoverageSettingsFile.readText().escapeBackSlash()
        )
    }

    private fun createTemporaryDiffCoverageSettingsFile(): File = FileUtil.createTempFile(
        DIFF_COVERAGE_CONFIG_FILE_NAME, null, true
    )

    private fun createCollectDiffCoverageConfigScript(
        gradlePath: String,
        temporaryDiffCoverageSettingsFile: File
    ): String {
        val tempDiffSettingsAbsolutePath = temporaryDiffCoverageSettingsFile.absolutePath.escapeBackSlash()
        val varSign = "$"
        return """
            allprojects {
                afterEvaluate { project ->
                    if(project.path == '$gradlePath') {
                        def execFiles = project.diffCoverageReport.jacocoExecFiles.collect{ '"' + it +'"' }.join(',')
                        def classes = project.diffCoverageReport.classesDirs.collect{ '"' + it +'"' }.join(',')
                        def reportsRoot = project.file(project.diffCoverageReport.reportConfiguration.baseReportDir)
                        new File('$tempDiffSettingsAbsolutePath').write ""${'"'}
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

    private fun String.escapeBackSlash(): String = replace("\\", "\\\\")

    private companion object {
        const val DIFF_COVERAGE_CONFIG_FILE_NAME = "diffCoverage.json"
    }
}
