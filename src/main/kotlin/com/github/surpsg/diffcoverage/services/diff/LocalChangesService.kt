package com.github.surpsg.diffcoverage.services.diff

import com.form.diff.CodeUpdateInfo
import com.github.surpsg.diffcoverage.domain.ChangeRange
import com.github.surpsg.diffcoverage.domain.FileChange
import com.github.surpsg.diffcoverage.services.CacheService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diff.impl.patch.PatchHunk
import com.intellij.openapi.diff.impl.patch.PatchLine
import com.intellij.openapi.diff.impl.patch.TextFilePatch
import com.intellij.openapi.diff.impl.patch.TextPatchBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager

@Service
class LocalChangesService(private var project: Project) {

    fun obtainCodeUpdateInfo(): CodeUpdateInfo {
        return project.service<CacheService>().getCached {
            CodeUpdateInfo(
                buildPatchCollection().map {
                    it.path to collectValuesFromRanges(it.changedRanges)
                }.toMap()
            )
        }
    }

    private fun collectValuesFromRanges(ranges: Set<ChangeRange>): Set<Int> {
        return ranges.asSequence().flatMap { range ->
            (range.from..range.to).asSequence()
        }.toSet()
    }

    fun buildPatchCollection(): List<FileChange> {
        return project.service<CacheService>().getCached {
            ChangeListManager.getInstance(project).changeLists.asSequence()
                .flatMap { it.changes.asSequence() }
                .map(::buildTextFilePatch)
                .map(::toFileChange)
                .toList()
        }
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
        val context = PatchHunkScanContext(startLineAfter)

        var currentOffset = 0
        for (patchLine in lines) {
            if (patchLine.type != context.currentHunkMode) {
                context.commitChangeRange(patchLine.type, currentOffset)
            }
            if (patchLine.type == PatchLine.Type.CONTEXT || patchLine.type == PatchLine.Type.ADD) {
                currentOffset++
            }
        }
        context.commitChangeRange(PatchLine.Type.CONTEXT, currentOffset)

        return context.changes
    }

    private class PatchHunkScanContext(
        private val startLineOffset: Int
    ) {
        var currentHunkMode: PatchLine.Type = PatchLine.Type.CONTEXT
        var currentHunkModeStartOffset: Int = 0

        private val changesPrivate: MutableSet<ChangeRange> = mutableSetOf()

        val changes: Set<ChangeRange> = changesPrivate

        fun commitChangeRange(
            nextHunkMode: PatchLine.Type,
            currentHunkModeEndOffset: Int
        ) {
            if (currentHunkMode == PatchLine.Type.ADD) {
                changesPrivate += ChangeRange(
                    startLineOffset + currentHunkModeStartOffset + 1,
                    startLineOffset + currentHunkModeEndOffset
                )
            }
            currentHunkMode = nextHunkMode
            currentHunkModeStartOffset = currentHunkModeEndOffset
        }
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
