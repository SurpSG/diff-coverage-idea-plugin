package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.DiffCoverageRunner
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.intellij.coverage.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File

@Service
class CoverageVizualizeService(private val project: Project) {

    fun showCoverage(coverageInfo: DiffCoverageConfiguration) = ApplicationManager.getApplication().invokeLater {
        val coverageSuitesBundle = CoverageSuitesBundle(buildCoverageSuite(coverageInfo))
        CoverageDataManager.getInstance(project).chooseSuitesBundle(
            coverageSuitesBundle
        )
    }

    private fun buildCoverageSuite(coverageInfo: DiffCoverageConfiguration): CoverageSuite {
        val classesPath = coverageInfo.classes.asSequence()
            .map(::File).filter(File::exists)
            .map(File::toPath).toSet()

        return coverageEngine().createCoverageSuite(
            DiffCoverageRunner(classesPath),
            "DiffCoverage",
            DefaultCoverageFileProvider(File(coverageInfo.execFiles.first())),
            null,
            System.currentTimeMillis(),
            null, false, true, false, project
        ) ?: throw RuntimeException("Cannot create coverage suite")
    }

    private fun coverageEngine(): CoverageEngine = CoverageEngine.EP_NAME.findFirstSafe {
        it.javaClass.simpleName.endsWith("JavaCoverageEngine")
    } ?: throw RuntimeException(
        """Cannot find java coverage engine in:             
            ${CoverageEngine.EP_NAME.extensionList.map { it::class.java }.joinToString(", ")}
            """.trimIndent()
    )
}
