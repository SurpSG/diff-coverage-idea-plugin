package com.github.surpsg.diffcoverage.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.jetbrains.plugins.gradle.settings.GradleSettings


internal class MyProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        println("Is gradle project ${GradleSettings.getInstance(project).linkedProjectsSettings.isNotEmpty()}")
    }
}
