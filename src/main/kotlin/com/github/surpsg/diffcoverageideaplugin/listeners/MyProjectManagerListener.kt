package com.github.surpsg.diffcoverageideaplugin.listeners

import com.github.surpsg.diffcoverageideaplugin.services.MyProjectService
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.ui.Messages

internal class MyProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        Messages.showMessageDialog(project, "yo helo", "I'm title", Messages.getInformationIcon());
    }
}
