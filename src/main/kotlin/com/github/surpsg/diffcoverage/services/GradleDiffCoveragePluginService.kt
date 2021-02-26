package com.github.surpsg.diffcoverage.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.first
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.util.GradleUtil

@Service
class GradleDiffCoveragePluginService(private val project: Project) {

    fun lookupDiffCoveragePluginModulePath(rootModulePath: String): String? {
        val diffCoveragePluginAppliedTo: Map<String, Set<String>> = lookupDiffCoveragePluginModules()
        if (diffCoveragePluginAppliedTo.size > 1 || diffCoveragePluginAppliedTo.first().value.size > 1) {
            return null
        }

        val diffCoverageModuleKey = diffCoveragePluginAppliedTo.first().value.first()
        return GradleUtil.findGradleModuleData(project, rootModulePath)
            ?.parent
            ?.let { ExternalSystemApiUtil.findAll(it, ProjectKeys.MODULE) }
            ?.find { it.data.id == diffCoverageModuleKey }
            ?.data
            ?.linkedExternalProjectPath
    }

    private fun lookupDiffCoveragePluginModules(): Map<String, Set<String>> {
        val projectsWithDiffCoverPlugin = mutableMapOf<String, MutableSet<String>>()
        for (projectEntry in GradleExtensionsSettings.getInstance(project).projects) {
            for (moduleExtension in projectEntry.value.extensions) {
                if (moduleExtension.value.extensions.containsKey("diffCoverageReport")) {
                    projectsWithDiffCoverPlugin.computeIfAbsent(projectEntry.key) {
                        mutableSetOf()
                    }.add(moduleExtension.key)
                }
            }
        }
        return projectsWithDiffCoverPlugin
    }
}
