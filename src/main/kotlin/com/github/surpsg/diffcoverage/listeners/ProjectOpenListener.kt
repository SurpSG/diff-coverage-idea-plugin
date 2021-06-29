package com.github.surpsg.diffcoverage.listeners

import com.github.surpsg.diffcoverage.coroutine.BACKGROUND_SCOPE
import com.github.surpsg.diffcoverage.persistent.DiffPluginInitializationStatus
import com.github.surpsg.diffcoverage.persistent.PersistentDiffCoverageConfigurationSettings
import com.github.surpsg.diffcoverage.services.PersistentDiffCoverageSettingsService
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import kotlinx.coroutines.launch

class ProjectOpenListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        ExternalSystemProgressNotificationManager.getInstance().addNotificationListener(
            createExternalSystemListener(project),
            project
        )
    }

    private fun createExternalSystemListener(project: Project): ExternalSystemTaskNotificationListener {
        return object : ExternalSystemTaskNotificationListenerAdapter() {
            override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
                super.onStart(id, workingDir)
                if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
                    PersistentDiffCoverageConfigurationSettings.getInstance().setInitializationStatus(
                        DiffPluginInitializationStatus.IN_PROGRESS
                    )
                }
            }

            override fun onSuccess(id: ExternalSystemTaskId) {
                super.onSuccess(id)
                if (id.type == ExternalSystemTaskType.RESOLVE_PROJECT) {
                    BACKGROUND_SCOPE.launch {
                        project.service<PersistentDiffCoverageSettingsService>().reloadSettings(true)
                    }
                }
            }
        }
    }
}
