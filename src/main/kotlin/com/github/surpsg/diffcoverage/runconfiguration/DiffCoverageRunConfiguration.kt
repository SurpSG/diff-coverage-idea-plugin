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

    var buildReportByIde: Boolean = DEFAULT_BUILD_BY_IDE
    var buildReportByGradlePlugin: Boolean = DEFAULT_BUILD_BY_GRADLE

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
        buildReportByIde = element.readBooleanChildOrDefault(BUILD_BY_IDE_KEY, DEFAULT_BUILD_BY_IDE)
        buildReportByGradlePlugin = element.readBooleanChildOrDefault(BUILD_BY_GRADLE_KEY, DEFAULT_BUILD_BY_GRADLE)
    }

    private fun Element.readBooleanChildOrDefault(key: String, default: Boolean): Boolean = getChild(key)?.let {
        it.text.toBoolean()
    } ?: default

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.apply {
            addContent(
                BUILD_BY_GRADLE_KEY.elementWithValue(buildReportByGradlePlugin)
            )
            addContent(
                BUILD_BY_IDE_KEY.elementWithValue(buildReportByIde)
            )
        }
    }

    private fun String.elementWithValue(value: Boolean) = Element(this).apply {
        text = value.toString()
    }

    companion object {
        const val BUILD_BY_IDE_KEY = "buildByIde"
        const val BUILD_BY_GRADLE_KEY = "buildByGradle"
        const val DEFAULT_BUILD_BY_IDE = true
        const val DEFAULT_BUILD_BY_GRADLE = false
    }
}
