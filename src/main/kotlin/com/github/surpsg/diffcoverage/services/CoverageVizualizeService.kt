package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.extensions.DiffCoverageEngine
import com.github.surpsg.diffcoverage.extensions.DiffCoverageRunner
import com.github.surpsg.diffcoverage.properties.NO_COVERAGE_FILES
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.coverage.CoverageDataManager
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageSuite
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.DefaultCoverageFileProvider
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Path

@Service
class CoverageVizualizeService(private val project: Project) {

    fun showCoverage(coverageInfo: DiffCoverageConfiguration) = ApplicationManager.getApplication().invokeLater {
        val coverageSuite: CoverageSuite? = buildCoverageSuite(coverageInfo)
        if (coverageSuite != null) {
            CoverageDataManager.getInstance(project).chooseSuitesBundle(
                CoverageSuitesBundle(coverageSuite)
            )
        } else {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                message = DiffCoverageBundle.message(NO_COVERAGE_FILES)
            )
        }
    }

    private fun buildCoverageSuite(coverageInfo: DiffCoverageConfiguration): CoverageSuite? {
        val classesPaths = toExistingPaths(coverageInfo.classes)
        val execFiles = toExistingPaths(coverageInfo.execFiles)
        if (execFiles.isEmpty()) {
            return null
        }

        return coverageEngine().createCoverageSuite(
            DiffCoverageRunner(project, classesPaths, execFiles),
            "DiffCoverage",
            DefaultCoverageFileProvider(execFiles.first().toFile()),
            null,
            System.currentTimeMillis(),
            null, false, true, false, project
        ) ?: throw RuntimeException("Cannot create coverage suite")
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
