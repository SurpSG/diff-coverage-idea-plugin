package com.github.surpsg.diffcoverage.services.coverage

import com.github.surpsg.diffcoverage.domain.CoverageSuiteBundleWithStat
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.extensions.DiffCoverageEngine
import com.github.surpsg.diffcoverage.extensions.DiffCoverageRunner
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.DefaultCoverageFileProvider
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Path

@Service
class CoverageBundleService(private val project: Project) {

    fun buildCoverageSuiteBundle(coverageInfo: DiffCoverageConfiguration): CoverageSuiteBundleWithStat? {
        val classesPaths = toExistingPaths(coverageInfo.classes)
        val execFiles = toExistingPaths(coverageInfo.execFiles)
        if (execFiles.isEmpty()) {
            return null
        }

        val coverageSuiteBundle: CoverageSuite = coverageEngine().createCoverageSuite(
            DiffCoverageRunner(project, classesPaths, execFiles),
            "DiffCoverage",
            DefaultCoverageFileProvider(execFiles.first().toFile()),
            null,
            System.currentTimeMillis(),
            null, false, true, false, project
        ) ?: throw RuntimeException("Cannot create coverage suite")

        return CoverageSuiteBundleWithStat(coverageSuiteBundle)
    }

    private fun toExistingPaths(values: Collection<String>): Set<Path> {
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
