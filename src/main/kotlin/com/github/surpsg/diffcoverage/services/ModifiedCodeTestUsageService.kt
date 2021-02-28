package com.github.surpsg.diffcoverage.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.SuperMethodsSearch

@Service
class ModifiedCodeTestUsageService(private val project: Project) {

    fun collectUsages() {
        val scope = GlobalSearchScopesCore.projectTestScope(project)
        project.service<ModifiedCodeService>()
            .collectModifiedCode().forEach { method ->
                println(method.name)

                println("\tdirect usage")
                findMethodUsage(method, scope)

                println("\tsuper method usage")
                SuperMethodsSearch.search(method, null, true, false).firstOrNull()?.method?.let {
                    findMethodUsage(it, scope)
                }

                println("\tinheritors method usage")
                OverridingMethodsSearch.search(method, scope, true).findAll().forEach { overridingMethod ->
                    println(overridingMethod.name)
                    findMethodUsage(overridingMethod, scope)
                }

                println("======")
            }
    }

    private fun findMethodUsage(method: PsiMethod, scope: GlobalSearchScope) {
        MethodReferencesSearch.search(method, scope, true).asSequence()
            .forEach {
                val containingFile = it.element.containingFile
                println("\t\tfound usage in file: $containingFile")
            }
    }
}
