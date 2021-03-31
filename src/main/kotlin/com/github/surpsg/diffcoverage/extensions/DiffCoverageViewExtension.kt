package com.github.surpsg.diffcoverage.extensions

import com.github.surpsg.diffcoverage.services.diff.ModifiedFilesService
import com.intellij.coverage.CoverageSuitesBundle
import com.intellij.coverage.JavaCoverageAnnotator
import com.intellij.coverage.JavaCoverageSuite
import com.intellij.coverage.view.CoverageListNode
import com.intellij.coverage.view.CoverageListRootNode
import com.intellij.coverage.view.CoverageViewManager
import com.intellij.coverage.view.JavaCoverageViewExtension
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
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

    override fun getChildrenNodes(node: AbstractTreeNode<*>?): List<AbstractTreeNode<*>> {
        val children: MutableList<AbstractTreeNode<*>> = ArrayList()

        if (node !is CoverageListNode || node.getValue() is PsiClass) {
            return children
        }
        val nodeValue = node.getValue()
        if (nodeValue is PsiPackage) {
            val packageCoverageData = PackageCoverageData(myProject)
            if (isInCoverageScope(packageCoverageData, nodeValue)) {
                directChildrenSequence(nodeValue, PsiPackage::getSubPackages).forEach { subPackage ->
                    processSubPackage(packageCoverageData, subPackage, children)
                }
                directChildrenSequence(nodeValue, PsiPackage::getFiles).forEach { file ->
                    collectFileChildren(file, node, children)
                }
            } else if (!myStateBean.myFlattenPackages) {
                collectSubPackages(packageCoverageData, children, nodeValue)
            }
        }
        if (node is CoverageListRootNode) {
            mySuitesBundle.suites.asSequence()
                .flatMap { mySuitesBundle.suites.asSequence() }
                .map { suite -> suite as JavaCoverageSuite }
                .flatMap { it.getCurrentSuiteClasses(myProject) }
                .forEach {
                    children += CoverageListNode(myProject, it, mySuitesBundle, myStateBean)
                }
        }

        return children.onEach {
            it.parent = node
        }
    }

    private fun <T> directChildrenSequence(
        nodePackage: PsiPackage,
        childrenItemsGetter: (PsiPackage, GlobalSearchScope) -> Array<T>
    ): Sequence<T> {
        return ReadAction.compute<Sequence<T>, RuntimeException> {
            if (nodePackage.isValid) {
                childrenItemsGetter(nodePackage, getSearchScope()).asSequence()
            } else {
                emptySequence()
            }
        }
    }

    private fun getSearchScope(): GlobalSearchScope {
        return mySuitesBundle.getSearchScope(myProject)
    }

    private fun collectSubPackages(
        packageCoverageData: PackageCoverageData,
        children: MutableList<AbstractTreeNode<*>>,
        rootPackage: PsiPackage
    ) {
        ReadAction.compute<Array<PsiPackage>, RuntimeException> {
            rootPackage.getSubPackages(getSearchScope())
        }.forEach {
            processSubPackage(packageCoverageData, it, children)
        }
    }

    private fun processSubPackage(
        packageCoverageData: PackageCoverageData,
        aPackage: PsiPackage,
        children: MutableList<AbstractTreeNode<*>>
    ) {
        if (isInCoverageScope(packageCoverageData, aPackage)) {
            children += CoverageListNode(myProject, aPackage, mySuitesBundle, myStateBean)
        } else if (!myStateBean.myFlattenPackages) {
            collectSubPackages(packageCoverageData, children, aPackage)
        }
        if (myStateBean.myFlattenPackages) {
            collectSubPackages(packageCoverageData, children, aPackage)
        }
    }

    private fun isInCoverageScope(packageCoverageData: PackageCoverageData, element: PsiElement): Boolean {
        return ReadAction.compute<Boolean, RuntimeException> {
            element is PsiPackage && packageCoverageData.isPackageInScope(element)
        }
    }

    class PackageCoverageData(project: Project) {
        private val diffPackages: Set<String> = project.service<ModifiedFilesService>()
            .buildPatchCollection().asSequence()
            .map { Paths.get(it.path).parent.toString() }
            .map { it.replace(File.separator, ".") }
            .toSet()

        fun isPackageInScope(psiPackage: PsiPackage): Boolean = diffPackages.any {
            it.endsWith(psiPackage.qualifiedName)
        }
    }
}
