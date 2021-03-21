package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import kotlinx.coroutines.runBlocking

@Service
class CacheService(private val project: Project) {

    fun <T> getCached(heavyFunction: () -> T): T {
        return CachedValuesManager.getManager(project).let { manager ->
            val cachedValue: Key<CachedValue<T>> = manager.getKeyForClass(heavyFunction.javaClass)
            manager.getCachedValue(
                project,
                cachedValue,
                {
                    CachedValueProvider.Result.create(
                        heavyFunction(),
                        PsiModificationTracker.MODIFICATION_COUNT,
                        ProjectRootManager.getInstance(project)
                    )
                },
                false
            )
        }
    }

    suspend fun <T> suspendableGetCached(heavyFunction: suspend () -> T): T {
        return CachedValuesManager.getManager(project).let { manager ->
            manager.getCachedValue(
                project,
                manager.getKeyForClass<T>(heavyFunction.javaClass),
                {
                    runBlocking(BACKGROUND_SCOPE.coroutineContext) {
                        CachedValueProvider.Result.create(
                            heavyFunction(),
                            PsiModificationTracker.MODIFICATION_COUNT,
                            ProjectRootManager.getInstance(project)
                        )
                    }
                },
                false
            )
        }
    }
}
