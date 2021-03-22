package com.github.surpsg.diffcoverage.extensions

import com.form.diff.ClassFile
import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.intellij.coverage.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import java.io.File
import java.nio.file.Paths

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

    override fun createSuite(
        acceptedCovRunner: CoverageRunner?,
        name: String?,
        coverageDataFileProvider: CoverageFileProvider?,
        filters: Array<String?>?,
        excludePatterns: Array<String?>?,
        lastCoverageTimeStamp: Long,
        coverageByTestEnabled: Boolean,
        tracingEnabled: Boolean,
        trackTestFolders: Boolean,
        project: Project
    ): JavaCoverageSuite {
        return object : JavaCoverageSuite(
            name, coverageDataFileProvider, filters, excludePatterns,
            lastCoverageTimeStamp, coverageByTestEnabled, tracingEnabled, trackTestFolders,
            acceptedCovRunner, this, project
        ) {
            val diffPackages: Set<String> = project.service<ModifiedFilesService>().buildPatchCollection().asSequence()
                .map { Paths.get(it.path).parent.toString() }
                .map { it.replace(File.separator, ".") }
                .toSet()

            override fun isPackageFiltered(packageFQName: String?): Boolean {
                return if (packageFQName.isNullOrEmpty()) {
                    true
                } else {
                    diffPackages.any { it.endsWith(packageFQName) }
                }
            }
        }
    }
}
