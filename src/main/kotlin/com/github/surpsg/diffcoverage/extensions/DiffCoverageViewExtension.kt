package com.github.surpsg.diffcoverage.extensions

import com.form.diff.ClassFile
import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.JavaCoverageAnnotator
import com.intellij.coverage.view.CoverageListNode
import com.intellij.coverage.view.CoverageViewManager
import com.intellij.coverage.view.JavaCoverageViewExtension
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPackage
import com.intellij.psi.search.GlobalSearchScope
import java.io.File
import java.nio.file.Paths

class DiffCoverageViewExtension(
    annotator: JavaCoverageAnnotator,
    project: Project,
    suitesBundle: CoverageSuitesBundle,
    stateBean: CoverageViewManager.StateBean
) : JavaCoverageViewExtension(
    annotator, project, suitesBundle, stateBean
) {

    private val codeUpdateInfo = myProject.service<ModifiedFilesService>().obtainCodeUpdateInfo()
    private val packageCoverageData = PackageCoverageData(myProject)

    override fun getChildrenNodes(node: AbstractTreeNode<*>?): List<AbstractTreeNode<*>> {
        if (node !is CoverageListNode || node.getValue() is PsiClass) {
            return emptyList()
        }
        var children: Sequence<AbstractTreeNode<*>> = emptySequence()
        val nodeValue = node.getValue()
        if (nodeValue is PsiPackage) {
            children += collectClasses(nodeValue)
        }
        return children.onEach { it.parent = node }.toList()
    }

    private fun collectClasses(psiPackage: PsiPackage): Sequence<AbstractTreeNode<*>> {
        var children = emptySequence<AbstractTreeNode<*>>()
        val currentPackages = ArrayDeque<PsiPackage>().apply { this += psiPackage }
        while (!currentPackages.isEmpty()) {
            val currentPackage = currentPackages.removeFirst()
            if (getInReadThread { !currentPackage.isValid }) {
                continue
            }

            if (packageCoverageData.isPackageInScope(currentPackage)) {
                children += currentPackage.directChildrenSequence(PsiPackage::getFiles).flatMap { file ->
                    collectChildrenClasses(file)
                }
            }

            currentPackage.directChildrenSequence(PsiPackage::getSubPackages)
                .filter { packageCoverageData.isParentPackageForDiffPackages(it) }
                .forEach { currentPackages += it }
        }
        return children
    }

    private fun <T> PsiPackage.directChildrenSequence(
        childrenItemsGetter: (PsiPackage, GlobalSearchScope) -> Array<T>
    ): Sequence<T> {
        return getValidValue(emptySequence()) {
            val searchScope = mySuitesBundle.getSearchScope(myProject)
            childrenItemsGetter(this, searchScope).asSequence()
        }
    }

    private fun collectChildrenClasses(file: PsiFile): Sequence<AbstractTreeNode<*>> {
        return if (file is PsiClassOwner) {
            file.getValidValue(PsiClass.EMPTY_ARRAY, PsiClassOwner::getClasses)
                .asSequence()
                .filter { isFileModified(it) }
                .map { CoverageListNode(myProject, it, mySuitesBundle, myStateBean) }
        } else {
            emptySequence()
        }
    }

    private fun isFileModified(psiClass: PsiClass): Boolean {
        val qualifiedClassName = psiClass.getValidValue(null) { qualifiedName } ?: return false
        val classFile = getInReadThread {
            ClassFile(psiClass.containingFile.name, qualifiedClassName)
        }

        return codeUpdateInfo.isInfoExists(classFile)
    }

    class PackageCoverageData(project: Project) {
        private val diffPackages: Set<String> = project.service<ModifiedFilesService>()
            .buildPatchCollection().asSequence()
            .map { Paths.get(it.path).parent.toString() }
            .map { it.replace(File.separator, ".") }
            .toSet()

        fun isPackageInScope(psiPackage: PsiPackage): Boolean {
            return psiPackage.isPackageFiltered { packageCandidate, diffPackage ->
                diffPackage.endsWith(packageCandidate)
            }
        }

        fun isParentPackageForDiffPackages(psiPackage: PsiPackage): Boolean {
            return psiPackage.isPackageFiltered { packageCandidate, diffPackage ->
                diffPackage.contains(packageCandidate)
            }
        }

        private fun PsiPackage.isPackageFiltered(
            filterCondition: (String, String) -> Boolean
        ): Boolean {
            val qualifiedNamePackage = ReadAction.compute<String, RuntimeException>(::getQualifiedName)
            return diffPackages.any { filterCondition(qualifiedNamePackage, it) }
        }
    }

    private fun <T> getInReadThread(func: () -> T): T {
        return ReadAction.compute<T, RuntimeException>(func)
    }

    private fun <T : PsiElement, V> T.getValidValue(defaultValue: V, valueGetter: T.() -> V): V {
        return getInReadThread {
            if (isValid) {
                valueGetter()
            } else {
                defaultValue
            }
        }
    }
}
