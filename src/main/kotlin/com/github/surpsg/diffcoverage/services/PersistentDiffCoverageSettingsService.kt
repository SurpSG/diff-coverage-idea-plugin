package com.github.surpsg.diffcoverage.services

import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.github.surpsg.diffcoverage.persistent.DiffPluginInitializationStatus
import com.github.surpsg.diffcoverage.persistent.PersistentDiffCoverageConfigurationSettings
import com.github.surpsg.diffcoverage.services.gradle.DiffCoverageSettingsService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import kotlinx.coroutines.launch
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Service
class PersistentDiffCoverageSettingsService(private val project: Project) {

    suspend fun loadSettings(): Pair<DiffPluginInitializationStatus, DiffCoverageConfiguration> {
        val configurationProvider = PersistentDiffCoverageConfigurationSettings.getInstance()
        return when (configurationProvider.getInitializationStatus()) {
            DiffPluginInitializationStatus.COMPLETE -> {
                DiffPluginInitializationStatus.COMPLETE to configurationProvider.obtainConfiguration()
            }
            DiffPluginInitializationStatus.IN_PROGRESS -> DiffPluginInitializationStatus.IN_PROGRESS to NULL_SETTINGS
            else -> reloadSettings()
        }
    }

    suspend fun reloadSettings(): Pair<DiffPluginInitializationStatus, DiffCoverageConfiguration> {
        return suspendCoroutine {
            reloadSettingsWhenProjectInitialized(it)
        }
    }

    private fun reloadSettingsWhenProjectInitialized(
        continuation: Continuation<Pair<DiffPluginInitializationStatus, DiffCoverageConfiguration>>
    ) {
        DumbService.getInstance(project).runWhenSmart {
            ExternalProjectsManager.getInstance(project).runWhenInitialized {
                loadSettingsAsync(continuation)
            }
        }
    }

    private fun loadSettingsAsync(
        continuation: Continuation<Pair<DiffPluginInitializationStatus, DiffCoverageConfiguration>>
    ) = BACKGROUND_SCOPE.launch {
        val configurationProvider = PersistentDiffCoverageConfigurationSettings.getInstance()
        val diffCoverageSettings = project.service<DiffCoverageSettingsService>().obtainDiffCoverageSettings()
        val result = if (diffCoverageSettings == null) {
            configurationProvider.setInitializationStatus(DiffPluginInitializationStatus.FAILED)
            DiffPluginInitializationStatus.FAILED to DiffCoverageConfiguration()
        } else {
            configurationProvider.applyConfigurationWithCompleteStatus(diffCoverageSettings)
            DiffPluginInitializationStatus.COMPLETE to diffCoverageSettings
        }
        continuation.resume(result)
    }

    private companion object {
        private val NULL_SETTINGS = DiffCoverageConfiguration()
    }
}
