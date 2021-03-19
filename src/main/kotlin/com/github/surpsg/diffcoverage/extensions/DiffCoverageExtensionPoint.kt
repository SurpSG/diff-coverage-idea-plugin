package com.github.surpsg.diffcoverage.extensions

import com.form.diff.ClassFile
import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.JavaCoverageEngineExtension
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.components.service
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File

class DiffCoverageExtensionPoint : JavaCoverageEngineExtension() {

    override fun isApplicableTo(conf: RunConfigurationBase<*>?): Boolean = false

    override fun ignoreCoverageForClass(bundle: CoverageSuitesBundle?, classFile: File?): Boolean {
        val filePath = classFile?.absolutePath ?: return false
        val coverageBundle = bundle ?: return false

        val classFileInfo = buildClassFileInfo(filePath)
        return !coverageBundle.project.service<ModifiedFilesService>()
            .isFileModified(classFileInfo)
    }

    private fun buildClassFileInfo(filePath: String): ClassFile {
        return ClassReader(File(filePath).readBytes()).let {
            val customClassVisitor = CustomClassVisitor()
            it.accept(customClassVisitor, ClassReader.SKIP_CODE)
            ClassFile(customClassVisitor.sourceName, it.className)
        }
    }

    private class CustomClassVisitor : ClassVisitor(Opcodes.ASM7) {

        var sourceName: String = ""

        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source, debug)
            sourceName = source ?: ""
        }
    }
}
