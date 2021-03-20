package com.github.surpsg.diffcoverage.services.diff

import com.form.diff.ClassFile
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service
class ModifiedFilesService(private val project: Project) {

    fun isFileModified(classFile: ClassFile): Boolean {
        return project.service<LocalChangesService>()
            .obtainCodeUpdateInfo()
            .isInfoExists(classFile)
    }
}
