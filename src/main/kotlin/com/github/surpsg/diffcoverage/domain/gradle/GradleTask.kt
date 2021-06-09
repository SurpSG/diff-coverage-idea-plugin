package com.github.surpsg.diffcoverage.domain.gradle

data class RunnableGradleTask(
    val taskName: String,
    val gradleModule: GradleModule,
    val taskDescription: String = taskName
)

data class GradleTaskWithInitScript(
    val gradleTask: RunnableGradleTask,
    val initScript: String
)
