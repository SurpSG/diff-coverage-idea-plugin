package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.domain.ChangeRange
import com.github.surpsg.diffcoverage.domain.FileChange
import com.intellij.openapi.components.Service
import com.intellij.openapi.diff.impl.patch.PatchHunk
import com.intellij.openapi.diff.impl.patch.PatchLine
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.TextPatchBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager

@Service
class LocalChangesService {

    fun buildPatchCollection(project: Project): List<FileChange> {
        return ChangeListManager.getInstance(project).changeLists.asSequence()
            .flatMap { it.changes.asSequence() }
            .map(::buildTextFilePatch)
            .map(::toFileChange)
            .toList()
    }

    private fun toFileChange(patch: TextFilePatch): FileChange {
        return FileChange(
            patch.afterName,
            toChangeRages(patch.hunks)
        )
    }

    private fun toChangeRages(hunks: List<PatchHunk>): Set<ChangeRange> {
        return hunks.asSequence()
            .flatMap {
                it.toChangeRange().asSequence()
            }.toSet()
    }

    private fun PatchHunk.toChangeRange(): Set<ChangeRange> {
        val changes: MutableSet<ChangeRange> = mutableSetOf()

        var currentLineMode = PatchLine.Type.CONTEXT
        var modeStartOffset = 0
        var currentOffset = 0
        for (patchLine in lines) {
            if (patchLine.type != currentLineMode) {
                if (currentLineMode == PatchLine.Type.ADD) {
                    changes += ChangeRange(
                        startLineAfter + modeStartOffset + 1,
                        startLineAfter + currentOffset
                    )
                }
                modeStartOffset = currentOffset
                currentLineMode = patchLine.type
            }
            if (patchLine.type == PatchLine.Type.CONTEXT || patchLine.type == PatchLine.Type.ADD) {
                currentOffset++
            }
        }
        return changes
    }

    private fun List<PatchLine>.findFirstByTypeOffset(offset: Int, type: PatchLine.Type): Int  {
        for (index in offset..size) {
            if(this[index].type == type) {
                return index
            }
        }
        return 0
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
