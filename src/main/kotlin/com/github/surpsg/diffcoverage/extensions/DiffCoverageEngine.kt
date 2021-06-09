package com.github.surpsg.diffcoverage.extensions

import com.form.diff.ClassFile
import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.JavaCoverageAnnotator
import com.intellij.coverage.JavaCoverageEngine
import com.intellij.coverage.view.CoverageViewExtension
import com.intellij.coverage.view.CoverageViewManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.io.File

class DiffCoverageEngine : JavaCoverageEngine() {

    override fun includeUntouchedFileInCoverage(
        qualifiedName: String,
        outputFile: File,
        sourceFile: PsiFile,
        suite: CoverageSuitesBundle
    ): Boolean {
        val classFile = ClassFile(sourceFile.name, qualifiedName.replace(".", "/"))
        return suite.project.service<ModifiedFilesService>().isFileModified(classFile)
    }

    override fun createCoverageViewExtension(
        project: Project,
        suiteBundle: CoverageSuitesBundle,
        stateBean: CoverageViewManager.StateBean
    ): CoverageViewExtension {
        return DiffCoverageViewExtension(
            getCoverageAnnotator(project) as JavaCoverageAnnotator,
            project,
            suiteBundle,
            CoverageViewManager.StateBean()
        )
    }
}
