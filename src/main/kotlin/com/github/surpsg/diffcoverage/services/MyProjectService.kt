package com.github.surpsg.diffcoverage.services

import com.intellij.openapi.project.Project
import com.github.surpsg.diffcoverage.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
