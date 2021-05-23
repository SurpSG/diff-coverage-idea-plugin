package com.github.surpsg.diffcoverage.runconfiguration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class DiffCoverageRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String
) : RunConfigurationBase<Any?>(project, factory, name) {

    var minCoveragePercents: Int = DEFAULT_MIN_COVERAGE

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return DiffCoverageRunConfigurationEditor()
    }

    override fun getState(
        executor: Executor,
        environment: ExecutionEnvironment
    ): RunProfileState {
        return DiffCoverageRunProfileState(project, this)
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        minCoveragePercents = element.readChildOrDefault(MIN_COVERAGE_KEY, DEFAULT_MIN_COVERAGE) {
            it.toInt()
        }
    }

    private fun <T> Element.readChildOrDefault(
        key: String,
        default: T,
        buildValue: (String) -> T
    ): T {
        return getChild(key)
            ?.let { buildValue(it.text) }
            ?: default
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.apply {
            addContent(
                MIN_COVERAGE_KEY.elementWithValue(minCoveragePercents)
            )
        }
    }

    private fun String.elementWithValue(value: Any) = Element(this).apply {
        text = value.toString()
    }

    companion object {
        const val DEFAULT_MIN_COVERAGE = 90
        const val MIN_COVERAGE_KEY = "minCoverage"
    }
}
