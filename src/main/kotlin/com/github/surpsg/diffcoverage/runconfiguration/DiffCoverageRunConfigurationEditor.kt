package com.github.surpsg.diffcoverage.runconfiguration

import com.intellij.openapi.options.SettingsEditor
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class DiffCoverageRunConfigurationEditor : SettingsEditor<DiffCoverageRunConfiguration>() {
    private lateinit var showDiffCoverageReportCheckBox: JCheckBox
    private lateinit var runDiffCoverageGradleTaskCheckBox: JCheckBox
    private lateinit var panel: JPanel

    override fun resetEditorFrom(diffCoverageRunConfiguration: DiffCoverageRunConfiguration) {
        diffCoverageRunConfiguration.apply {
            showDiffCoverageReportCheckBox.isSelected = buildReportByIde
            runDiffCoverageGradleTaskCheckBox.isSelected = buildReportByGradlePlugin
        }
    }

    override fun applyEditorTo(diffCoverageRunConfiguration: DiffCoverageRunConfiguration) {
        diffCoverageRunConfiguration.apply {
            buildReportByGradlePlugin = runDiffCoverageGradleTaskCheckBox.isSelected
            buildReportByIde = showDiffCoverageReportCheckBox.isSelected
        }
    }

    override fun createEditor(): JComponent {
        return panel
    }
}
