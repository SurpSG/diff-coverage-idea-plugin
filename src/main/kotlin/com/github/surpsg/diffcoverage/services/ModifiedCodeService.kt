package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.domain.ChangeRange
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.*
import java.nio.file.Paths
import com.intellij.psi.util.PsiTreeUtil


@Service
class ModifiedCodeService {

    fun collectModifiedCode(project: Project): Set<PsiMethod> {
        val modifiedMethods: MutableSet<PsiMethod> = mutableSetOf()

        val patches = service<LocalChangesService>().buildPatchCollection(project)
        for (patch in patches) {
            val psiFile: PsiJavaFile = obtainPsiFile(project, patch.path) ?: continue
            val document = obtainDocumentByPsiFile(project, psiFile) ?: continue

            val methods: MutableSet<PsiMethod> = collectPsiFileMethods(psiFile)
            for (changeRange in patch.changedRanges) {
                val modifiedRange = toTextRange(document, changeRange)
                val modifiedMethod: PsiMethod? = methods.find { method ->
                    method.textRange.intersects(modifiedRange.lineStartOffset, modifiedRange.lineEndOffset)
                }
                if (modifiedMethod != null) {
                    methods.remove(modifiedMethod)
                    modifiedMethods += modifiedMethod
                }
            }
        }
        return modifiedMethods
    }

    private fun collectPsiFileMethods(psiFile: PsiJavaFile): MutableSet<PsiMethod> =
        PsiTreeUtil.collectElementsOfType(psiFile, PsiClass::class.java)
            .asSequence()
            .flatMap { it.methods.asSequence() }
            .toMutableSet()


    private fun obtainPsiFile(project: Project, path: String): PsiJavaFile? {
        return VirtualFileManager.getInstance()
            .findFileByNioPath(Paths.get(path))
            ?.let { virtualFile ->
                PsiManager.getInstance(project).findFile(virtualFile) as? PsiJavaFile
            }
    }

    private fun obtainDocumentByPsiFile(project: Project, psiFile: PsiFile): Document? {
        return PsiDocumentManager.getInstance(project).getDocument(psiFile)
    }

    private fun toTextRange(document: Document, changeRange: ChangeRange): TextRangeData {
        val lineStartOffset = document.getLineStartOffset(changeRange.from - 1)
        val lineEndOffset = document.getLineEndOffset(changeRange.to - 1)
        return TextRangeData(
            lineStartOffset,
            lineEndOffset,
            changeRange.from,
            changeRange.to
        )
    }

    private data class TextRangeData(
        val lineStartOffset: Int,
        val lineEndOffset: Int,
        val fromLine: Int,
        val toLine: Int
    )
}
