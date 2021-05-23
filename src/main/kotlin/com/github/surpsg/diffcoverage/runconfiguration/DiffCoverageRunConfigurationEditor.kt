package com.github.surpsg.diffcoverage.runconfiguration

import com.intellij.openapi.options.SettingsEditor
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class DiffCoverageRunConfigurationEditor : SettingsEditor<DiffCoverageRunConfiguration>() {

    private lateinit var panel: JPanel

    private lateinit var minCoverageSpinner: JSpinner

    override fun resetEditorFrom(diffCoverageRunConfiguration: DiffCoverageRunConfiguration) {
        diffCoverageRunConfiguration.apply {
            minCoverageSpinner.value = diffCoverageRunConfiguration.minCoveragePercents
        }
    }

    override fun applyEditorTo(diffCoverageRunConfiguration: DiffCoverageRunConfiguration) {
        diffCoverageRunConfiguration.apply {
            minCoveragePercents = minCoverageSpinner.value as Int
        }
    }

    private fun createUIComponents() {
        minCoverageSpinner = JSpinner(SpinnerNumberModel(
            DiffCoverageRunConfiguration.DEFAULT_MIN_COVERAGE,
            MIN_COVERAGE_PERCENTS_VALUE,
            MAX_COVERAGE_PERCENTS_VALUE,
            COVERAGE_CHANGE_STEP
        ))
    }

    override fun createEditor(): JComponent {
        return panel
    }

    private companion object {
        const val COVERAGE_CHANGE_STEP = 1
        const val MAX_COVERAGE_PERCENTS_VALUE = 100
        const val MIN_COVERAGE_PERCENTS_VALUE = 0
    }
}
