package com.github.surpsg.diffcoverage.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.ui.Messages

internal class MyProjectManagerListener : ProjectManagerListener {

    override fun projectOpened(project: Project) {
        Messages.showMessageDialog(project, "yo helo", "I'm title", Messages.getInformationIcon());
    }
}
