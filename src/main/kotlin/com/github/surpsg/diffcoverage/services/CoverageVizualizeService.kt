package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.extensions.DiffCoverageEngine
import com.github.surpsg.diffcoverage.extensions.DiffCoverageRunner
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.intellij.coverage.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Path

@Service
class CoverageVizualizeService(private val project: Project) {

    fun showCoverage(coverageInfo: DiffCoverageConfiguration) = ApplicationManager.getApplication().invokeLater {
        val coverageSuitesBundle = CoverageSuitesBundle(buildCoverageSuite(coverageInfo))
        CoverageDataManager.getInstance(project).chooseSuitesBundle(
            coverageSuitesBundle
        )
    }

    private fun buildCoverageSuite(coverageInfo: DiffCoverageConfiguration): CoverageSuite {
        val classesPaths = toPaths(coverageInfo.classes)
        val execFiles = toPaths(coverageInfo.execFiles)

        return coverageEngine().createCoverageSuite(
            DiffCoverageRunner(project, classesPaths, execFiles),
            "DiffCoverage",
            DefaultCoverageFileProvider(execFiles.first().toFile()),
            null,
            System.currentTimeMillis(),
            null, false, true, false, project
        ) ?: throw RuntimeException("Cannot create coverage suite")
    }

    private fun toPaths(values: Collection<String>): Set<Path> {
        return values.asSequence()
            .map(::File).filter(File::exists)
            .map(File::toPath).toSet()
    }

    private fun coverageEngine(): CoverageEngine = CoverageEngine.EP_NAME.findExtension(DiffCoverageEngine::class.java)
        ?: throw RuntimeException(
            """Cannot find java coverage engine in:             
            ${CoverageEngine.EP_NAME.extensionList.map { it::class.java }.joinToString(", ")}
            """.trimIndent()
        )
}
