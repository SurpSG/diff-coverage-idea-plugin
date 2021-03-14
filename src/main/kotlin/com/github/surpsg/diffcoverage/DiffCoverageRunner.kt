package com.github.surpsg.diffcoverage

import com.intellij.coverage.CoverageEngine
import com.intellij.coverage.CoverageRunner
import com.intellij.coverage.CoverageSuite
import com.intellij.openapi.diagnostic.Logger
import com.intellij.rt.coverage.data.LineCoverage
import com.intellij.rt.coverage.data.LineData
import com.intellij.rt.coverage.data.ProjectData
import org.jacoco.core.analysis.Analyzer
import org.jacoco.core.analysis.CoverageBuilder
import org.jacoco.core.analysis.ICounter
import org.jacoco.core.tools.ExecFileLoader
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class DiffCoverageRunner(private val classesPath: Set<Path>) : CoverageRunner() {

    override fun loadCoverageData(sessionDataFile: File, baseCoverageSuite: CoverageSuite?): ProjectData {
        return ProjectData().apply {
            try {
                loadExecutionData(sessionDataFile, this)
            } catch (e: Exception) {
                LOG.error(e)
            }
        }
    }

    private fun loadExecutionData(
        sessionDataFile: File,
        data: ProjectData
    ) {
        val coverageBuilder = getCoverageBuilder(sessionDataFile)

        for (classCoverage in coverageBuilder.classes) {
            var className = classCoverage.name
            className = className.replace('\\', '.').replace('/', '.')
            val classData = data.getOrCreateClassData(className)
            val methods = classCoverage.methods
            val lines = arrayOfNulls<LineData>(classCoverage.lastLine + 1)
            for (method in methods) {
                val desc = method.name + method.desc
                // Line numbers are 1-based here.
                val firstLine = method.firstLine
                val lastLine = method.lastLine
                for (i in firstLine..lastLine) {
                    val methodLine = method.getLine(i)
                    val methodLineStatus = methodLine.status
                    if (methodLineStatus == ICounter.EMPTY) continue
                    val lineData = LineData(i, desc)
                    when (methodLineStatus) {
                        ICounter.FULLY_COVERED -> {
                            lineData.setStatus(LineCoverage.FULL)
                            lineData.setStatus(LineCoverage.PARTIAL)
                            lineData.setStatus(LineCoverage.NONE)
                        }
                        ICounter.PARTLY_COVERED -> {
                            lineData.setStatus(LineCoverage.PARTIAL)
                            lineData.setStatus(LineCoverage.NONE)
                        }
                        else -> lineData.setStatus(LineCoverage.NONE)
                    }
                    lineData.hits =
                        if (methodLineStatus == ICounter.FULLY_COVERED || methodLineStatus == ICounter.PARTLY_COVERED) 1 else 0
                    val branchCounter = methodLine.branchCounter
                    var coveredCount = branchCounter.coveredCount
                    for (b in 0 until branchCounter.totalCount) {
                        val jump = lineData.addJump(b)
                        if (coveredCount-- > 0) {
                            jump.trueHits = 1
                            jump.falseHits = 1
                        }
                    }
                    classData.registerMethodSignature(lineData)
                    lineData.fillArrays()
                    lines[i] = lineData
                }
            }
            classData.setLines(lines)
        }
    }

    private fun getCoverageBuilder(sessionDataFile: File): CoverageBuilder {
        val executionDataStore = ExecFileLoader().apply { load(sessionDataFile) }.executionDataStore
        return CoverageBuilder().apply {
            val analyzer = Analyzer(executionDataStore, this)
            classesPath.forEach {
                Files.walkFileTree(it, fileVisitor(analyzer))
            }
        }
    }

    private fun fileVisitor(analyzer: Analyzer) = object : SimpleFileVisitor<Path>() {
        override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
            try {
                analyzer.analyzeAll(path.toFile())
            } catch (e: Exception) {
                LOG.info(e)
            }
            return FileVisitResult.CONTINUE
        }
    }

    override fun getPresentableName(): String = "DiffCoverage"

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
