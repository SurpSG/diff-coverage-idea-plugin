package com.github.surpsg.diffcoverage.util

import com.form.diff.ClassFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import java.io.File

fun obtainClassInfo(filePath: String): ClassInfo {
    return ClassReader(File(filePath).readBytes()).let {
        val customClassVisitor = CustomClassVisitor()
        it.accept(customClassVisitor, ClassReader.SKIP_CODE)
        ClassInfo(
            ClassFile(customClassVisitor.sourceName, it.className),
            customClassVisitor.synthetic
        )
    }
}

private class CustomClassVisitor : ClassVisitor(Opcodes.ASM7) {

    var sourceName: String = ""
    var synthetic: Boolean = false

    override fun visitSource(source: String?, debug: String?) {
        super.visitSource(source, debug)
        sourceName = source ?: ""
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        synthetic = access and Opcodes.ACC_SYNTHETIC == Opcodes.ACC_SYNTHETIC
    }
}

data class ClassInfo(
    val classFile: ClassFile,
    val synthetic: Boolean
)
