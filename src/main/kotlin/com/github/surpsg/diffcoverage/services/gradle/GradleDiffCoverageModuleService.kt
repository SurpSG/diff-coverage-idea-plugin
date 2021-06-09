package com.github.surpsg.diffcoverage.services.gradle

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.domain.gradle.GradleModule
import com.github.surpsg.diffcoverage.properties.gradle.DIFF_COVERAGE_CONFIGURATION_PROPERTY
import com.github.surpsg.diffcoverage.services.notifications.BalloonNotificationService
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.first
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.util.GradleUtil
import java.io.File
import java.nio.file.Paths

@Service
class GradleDiffCoverageModuleService(private val project: Project) {

    fun lookupDiffCoveragePluginModule(): GradleModule? {
        val diffCoverageModule = getDiffCoverageModule() ?: return null

        val rootModulePath = getRootModulePath()
        val diffCoverageModuleKey = normalizeModuleKey(rootModulePath, diffCoverageModule)
        return GradleUtil.findGradleModuleData(project, rootModulePath)
            ?.parent
            ?.let { ExternalSystemApiUtil.findAll(it, ProjectKeys.MODULE) }
            ?.find { it.data.id == diffCoverageModuleKey }
            ?.data
            ?.let { GradleModule(diffCoverageModule, it.linkedExternalProjectPath) }
    }

    private fun getDiffCoverageModule(): String? {
        val diffPluginAppliedTo: Map<String, Set<String>> = lookupDiffCoveragePluginModules()
        if (diffPluginAppliedTo.isEmpty() || diffPluginAppliedTo.first().value.isEmpty()) {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                notificationListener = NotificationListener.URL_OPENING_LISTENER,
                message = DiffCoverageBundle.message("no.diff.coverage.entries")
            )
            return null
        }
        if (diffPluginAppliedTo.size > 1 || diffPluginAppliedTo.first().value.size > 1) {
            project.service<BalloonNotificationService>().notify(
                notificationType = NotificationType.ERROR,
                message = DiffCoverageBundle.message(
                    "multiple.diff.coverage.entries",
                    diffPluginAppliedTo.asSequence().flatMap { entry ->
                        entry.value.asSequence().map {
                            String.format("%s%s", File(entry.key).name, it)
                        }
                    }.joinToString("\n", limit = 3)
                )
            )
            return null
        }
        return diffPluginAppliedTo.first().value.first()
    }

    private fun getRootModulePath(): String {
        return GradleSettings.getInstance(project).linkedProjectsSettings.first().externalProjectPath
    }

    private fun normalizeModuleKey(rootModulePath: String, moduleKey: String): String {
        return if (moduleKey == ROOT_PROJECT_KEY) {
            Paths.get(rootModulePath).fileName.toString()
        } else {
            moduleKey
        }
    }

    private fun lookupDiffCoveragePluginModules(): Map<String, Set<String>> {
        val projectsWithDiffCoverPlugin = mutableMapOf<String, MutableSet<String>>()
        for (projectEntry in GradleExtensionsSettings.getInstance(project).projects) {
            for (moduleExtension in projectEntry.value.extensions) {
                if (moduleExtension.value.extensions.containsKey(DIFF_COVERAGE_CONFIGURATION_PROPERTY)) {
                    projectsWithDiffCoverPlugin.computeIfAbsent(projectEntry.key) {
                        mutableSetOf()
                    }.add(moduleExtension.key)
                }
            }
        }
        return projectsWithDiffCoverPlugin
    }

    companion object {
        const val ROOT_PROJECT_KEY = ":"
    }
}
