package com.github.surpsg.diffcoverage.listeners

import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.gradle.settings.GradleExtensionsSettings
import org.jetbrains.plugins.gradle.settings.GradleSettings

internal class GradleStartup : StartupActivity.DumbAware {
    override fun runActivity(project: Project) {
        println("Is gradle project ${GradleSettings.getInstance(project).linkedProjectsSettings.isNotEmpty()}")

        ExternalProjectsManager.getInstance(project).runWhenInitialized {
            val diffCoveragePluginAppliedTo = lookupDiffCoveragePlugin(project)
            val (icon, message) = if (diffCoveragePluginAppliedTo.isNotEmpty()) {
                Messages.getInformationIcon() to diffCoveragePluginAppliedTo.asSequence().joinToString("\n") {
                    "DiffCoverage is applied to: \n ${it.key}\n\t${it.value.joinToString("\n\t")}"
                }
            } else {
                Messages.getWarningIcon() to "DiffCoverage is not applied. Visit https://github.com/form-com/diff-coverage-gradle"
            }

            Messages.showMessageDialog(
                    project,
                    message,
                    "DiffCoverage plugin",
                    icon
            );
        }
    }

    private fun lookupDiffCoveragePlugin(project: Project): Map<String, Set<String>> {
        val projectsWithDiffCoverPlugin = mutableMapOf<String, MutableSet<String>>()
        for (projectEntry in GradleExtensionsSettings.getInstance(project).projects) {
            for (moduleExtension in projectEntry.value.extensions) {
                if (moduleExtension.value.tasksMap.containsKey("diffCoverage")) {
                    projectsWithDiffCoverPlugin.computeIfAbsent(projectEntry.key) {
                        mutableSetOf()
                    }.add(moduleExtension.key)
                }
            }
        }

        println(projectsWithDiffCoverPlugin)

        return projectsWithDiffCoverPlugin
    }
}
