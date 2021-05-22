package com.github.surpsg.diffcoverage.extensions

import com.form.coverage.filters.ModifiedLinesFilter
import com.form.diff.CodeUpdateInfo
import com.github.surpsg.diffcoverage.domain.CoverageStat
import com.github.surpsg.diffcoverage.domain.ProjectDataWithStat
import com.intellij.openapi.diagnostic.Logger
import com.intellij.rt.coverage.data.LineCoverage
import com.intellij.rt.coverage.data.LineData
import org.jacoco.core.analysis.*
import org.jacoco.core.data.ExecutionDataStore
import org.jacoco.core.internal.analysis.FilteringAnalyzer
import org.jacoco.core.tools.ExecFileLoader
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

internal class DiffCoverageLoader {

    fun loadDiffExecutionData(
        codeUpdateInfo: CodeUpdateInfo,
        classesPath: Set<Path>,
        execFilesPaths: Set<Path>
    ): ProjectDataWithStat {
        val coverageBuilder: CoverageBuilder = getCoverageBuilder(codeUpdateInfo, classesPath, execFilesPaths)
        val bundle: IBundleCoverage = coverageBuilder.getBundle("")
        val coverageData = ProjectDataWithStat(buildCoverageStat(bundle))

        for (classCoverage in coverageBuilder.classes) {
            var className = classCoverage.name
            className = className.replace('\\', '.').replace('/', '.')
            val classData = coverageData.getOrCreateClassData(className)
            val lines = arrayOfNulls<LineData>(classCoverage.lastLine + 1)
            for (method in classCoverage.methods) {
                val desc = method.name + method.desc
                for (i in method.firstLine..method.lastLine) {
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
                    lineData.hits = computeHits(methodLineStatus)
                    var coveredCount = methodLine.branchCounter.coveredCount
                    for (b in 0 until methodLine.branchCounter.totalCount) {
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
        return coverageData
    }

    private fun computeHits(methodLineStatus: Int): Int {
        return if (methodLineStatus == ICounter.FULLY_COVERED || methodLineStatus == ICounter.PARTLY_COVERED) 1 else 0
    }

    private fun getCoverageBuilder(
        codeUpdateInfo: CodeUpdateInfo,
        classesPath: Set<Path>,
        execFilesPaths: Set<Path>
    ): CoverageBuilder {
        return CoverageBuilder().apply {
            val analyzer = buildAnalyzer(codeUpdateInfo, execFilesPaths, this)
            val fileVisitor = fileVisitor(analyzer)

            classesPath.forEach {
                Files.walkFileTree(it, fileVisitor)
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

    private fun buildAnalyzer(
        codeUpdateInfo: CodeUpdateInfo,
        execFilesPaths: Set<Path>,
        coverageVisitor: ICoverageVisitor
    ): FilteringAnalyzer {
        return FilteringAnalyzer(
            loadCoverage(execFilesPaths),
            coverageVisitor,
            codeUpdateInfo::isInfoExists
        ) {
            ModifiedLinesFilter(codeUpdateInfo)
        }
    }

    private fun loadCoverage(
        execFilesPaths: Set<Path>
    ): ExecutionDataStore = ExecFileLoader().apply {
        execFilesPaths.forEach {
            load(it.toFile())
        }
    }.executionDataStore

    private fun buildCoverageStat(bundle: IBundleCoverage): CoverageStat {
        return ICoverageNode.CounterEntity.values().asSequence().map {
            it to bundle.getCounter(it)
        }.toMap().let {
            CoverageStat(it)
        }
    }

    companion object {
        private val LOG = Logger.getInstance(DiffCoverageLoader::class.java)
    }
}
