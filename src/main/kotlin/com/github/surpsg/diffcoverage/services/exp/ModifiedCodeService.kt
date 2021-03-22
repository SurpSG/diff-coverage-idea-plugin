package com.github.surpsg.diffcoverage.services.exp

import com.github.surpsg.diffcoverage.domain.ChangeRange
import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import java.nio.file.Paths

@Service
class ModifiedCodeService(private val project: Project) {

    fun collectModifiedCode(): Set<PsiMethod> {
        val modifiedMethods: MutableSet<PsiMethod> = mutableSetOf()

        val patches = project.service<ModifiedFilesService>().buildPatchCollection()
        for (patch in patches) {
            val virtualFile: VirtualFile = obtainVirtualFile(patch.path)
                ?.takeIf { !TestSourcesFilter.isTestSources(it, project) } ?: continue
            val psiFile: PsiJavaFile = obtainPsiFile(virtualFile) ?: continue
            val document = obtainDocumentByPsiFile(psiFile) ?: continue

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

    private fun obtainVirtualFile(path: String): VirtualFile? {
        return VirtualFileManager.getInstance().findFileByNioPath(Paths.get(path))
    }

    private fun obtainPsiFile(virtualFile: VirtualFile): PsiJavaFile? {
        return PsiManager.getInstance(project).findFile(virtualFile) as? PsiJavaFile
    }

    private fun obtainDocumentByPsiFile(psiFile: PsiFile): Document? {
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
