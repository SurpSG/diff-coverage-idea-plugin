package com.github.surpsg.diffcoverage.persistent

import com.github.surpsg.diffcoverage.domain.DiffCoverageConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.concurrent.atomic.AtomicReference

@State(name = "diffCoverageConfiguration", storages = [Storage("diffCoverageConfiguration.xml")])
class PersistentDiffCoverageConfigurationSettings :
    DiffCoverageConfigurationProvider, PersistentStateComponent<PersistentDiffCoverageConfigurationSettings> {
    var project: String = ""
    var projectDir: String = ""
    var execFiles: List<String> = emptyList()
    var classes: List<String> = emptyList()
    var reportsRoot: String = ""

    private val initialized: AtomicReference<DiffPluginInitializationStatus> = AtomicReference(
        DiffPluginInitializationStatus.NOT_STARTED
    )

    companion object {
        @JvmStatic
        fun getInstance(): DiffCoverageConfigurationProvider {
            return ApplicationManager.getApplication()
                .getService(PersistentDiffCoverageConfigurationSettings::class.java)
        }
    }

    override fun getInitializationStatus(): DiffPluginInitializationStatus = initialized.get()

    override fun setInitializationStatus(status: DiffPluginInitializationStatus) = initialized.set(status)

    override fun getState(): PersistentDiffCoverageConfigurationSettings {
        return copyProperties(this, PersistentDiffCoverageConfigurationSettings())
    }

    override fun loadState(state: PersistentDiffCoverageConfigurationSettings) {
        copyProperties(state, this)
    }

    private fun copyProperties(
        from: PersistentDiffCoverageConfigurationSettings,
        to: PersistentDiffCoverageConfigurationSettings,
    ) = to.apply {
        project = from.project
        projectDir = from.projectDir
        execFiles = from.execFiles
        classes = from.classes
        reportsRoot = from.reportsRoot
    }

    override fun obtainConfiguration() = DiffCoverageConfiguration(
        project,
        projectDir,
        execFiles,
        classes,
        reportsRoot
    )

    override fun applyConfigurationWithCompleteStatus(diffCoverageConfiguration: DiffCoverageConfiguration) {
        project = diffCoverageConfiguration.project
        projectDir = diffCoverageConfiguration.projectDir
        classes = diffCoverageConfiguration.classes
        execFiles = diffCoverageConfiguration.execFiles
        reportsRoot = diffCoverageConfiguration.reportsRoot
        initialized.set(DiffPluginInitializationStatus.COMPLETE)
    }
}

interface DiffCoverageConfigurationProvider {
    fun obtainConfiguration(): DiffCoverageConfiguration
    fun applyConfigurationWithCompleteStatus(diffCoverageConfiguration: DiffCoverageConfiguration)
    fun getInitializationStatus(): DiffPluginInitializationStatus
    fun setInitializationStatus(status: DiffPluginInitializationStatus)
}

enum class DiffPluginInitializationStatus {
    NOT_STARTED, IN_PROGRESS, COMPLETE, FAILED
}
