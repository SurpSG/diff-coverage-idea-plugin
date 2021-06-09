package com.github.surpsg.diffcoverage.runconfiguration

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.ui.IconManager
import javax.swing.Icon

class DiffCoverageRunConfigurationType : ConfigurationType {

    override fun getDisplayName(): String {
        return DiffCoverageBundle.message("diff.coverage.run.config.name")
    }

    override fun getConfigurationTypeDescription(): String {
        return DiffCoverageBundle.message("diff.coverage.run.config.desc")
    }

    override fun getIcon(): Icon = DIFF_COVERAGE_ICON

    override fun getId(): String {
        return CONFIGURATION_TYPE_ID
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(DiffCoverageRunConfigurationFactory(this))
    }

    companion object {
        const val CONFIGURATION_TYPE_ID = "DiffCoverageRunConfiguration"
        val DIFF_COVERAGE_ICON: Icon by lazy {
            IconManager.getInstance().getIcon(
                "/icons/runConfiguration.svg",
                DiffCoverageRunConfigurationType::class.java
            )
        }
    }
}
