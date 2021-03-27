package com.github.surpsg.diffcoverage.runconfiguration

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class DiffCoverageRunConfigurationFactory(
    type: ConfigurationType
) : ConfigurationFactory(type) {

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return DiffCoverageRunConfiguration(
            project,
            this,
            DiffCoverageBundle.message("diff.coverage.run.config.name")
        )
    }

    override fun getName(): String {
        return NAME
    }

    companion object {
        const val NAME = "Diff Coverage configuration factory"
    }
}
