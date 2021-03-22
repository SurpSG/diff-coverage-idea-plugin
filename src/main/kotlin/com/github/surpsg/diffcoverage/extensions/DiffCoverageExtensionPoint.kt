package com.github.surpsg.diffcoverage.extensions

import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.github.surpsg.diffcoverage.util.obtainClassInfo
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.JavaCoverageEngineExtension
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.components.service
import java.io.File

class DiffCoverageExtensionPoint : JavaCoverageEngineExtension() {

    override fun isApplicableTo(conf: RunConfigurationBase<*>?): Boolean = false

    override fun ignoreCoverageForClass(bundle: CoverageSuitesBundle?, classFile: File?): Boolean {
        val filePath = classFile?.absolutePath ?: return false
        val coverageBundle = bundle ?: return false

        val classInfo = obtainClassInfo(filePath)
        return if (classInfo.synthetic) {
            return true
        } else {
            !coverageBundle.project.service<ModifiedFilesService>()
                .isFileModified(classInfo.classFile)
        }
    }
}
