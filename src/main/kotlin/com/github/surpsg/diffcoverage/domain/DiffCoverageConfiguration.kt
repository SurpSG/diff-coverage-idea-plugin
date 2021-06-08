package com.github.surpsg.diffcoverage.domain

data class DiffCoverageConfiguration(
    val project: String = "",
    val projectDir: String = "",
    val execFiles: List<String> = listOf(),
    val classes: List<String> = listOf(),
    val reportsRoot: String = ""
)
