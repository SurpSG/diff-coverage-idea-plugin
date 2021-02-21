package com.github.surpsg.diffcoverage.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.TextPatchBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager

@Service
class LocalChangesService {

    fun buildPatchCollection(project: Project): List<TextFilePatch> {
        return ChangeListManager.getInstance(project).changeLists.asSequence()
            .flatMap { it.changes.asSequence() }
            .map(::buildTextFilePatch)
            .toList()
    }

    private fun buildTextFilePatch(change: Change): TextFilePatch {
        return TextFilePatch(
            change.virtualFile?.charset,
            change.virtualFile?.detectedLineSeparator
        ).apply {
            beforeName = change.beforeRevision?.file?.path
            afterName = change.afterRevision?.file?.path

            TextPatchBuilder.buildPatchHunks(
                change.beforeRevision?.content ?: "",
                change.afterRevision?.content ?: ""
            ).forEach(::addHunk)
        }
    }
}
