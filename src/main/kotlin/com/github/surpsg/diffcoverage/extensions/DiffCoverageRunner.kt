package com.github.surpsg.diffcoverage.extensions

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.domain.CoverageStat
import com.github.surpsg.diffcoverage.domain.ProjectDataWithStat
import com.github.surpsg.diffcoverage.properties.PLUGIN_NAME
import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Path

class DiffCoverageRunner(
    private val project: Project,
    private val classesPath: Set<Path>,
    private val execFilesPaths: Set<Path>
) : CoverageRunner() {

    override fun loadCoverageData(sessionDataFile: File, baseCoverageSuite: CoverageSuite?): ProjectDataWithStat {
        return try {
            DiffCoverageLoader().loadDiffExecutionData(
                project.service<ModifiedFilesService>().obtainCodeUpdateInfo(),
                classesPath,
                execFilesPaths
            )
        } catch (e: Exception) {
            LOG.error("Cannot load coverage data", e)
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.WARNING,
                message = DiffCoverageBundle.message("cannot.load.diff.coverage.data", execFilesPaths)
            )
            ProjectDataWithStat(CoverageStat(emptyMap()))
        }
    }

    override fun getPresentableName(): String = DiffCoverageBundle.message(PLUGIN_NAME)

    override fun getId(): String = "diff_coverage"

    override fun getDataFileExtension(): String = "exec"

    override fun acceptsCoverageEngine(engine: CoverageEngine): Boolean {
        val javaCoverageEngineClass = CoverageEngine.EP_NAME.findFirstSafe {
            it.javaClass.simpleName.endsWith("JavaCoverageEngine")
        }?.javaClass ?: return false

        return javaCoverageEngineClass.isInstance(engine)
    }

    companion object {
        private val LOG = Logger.getInstance(DiffCoverageRunner::class.java)
    }
}
