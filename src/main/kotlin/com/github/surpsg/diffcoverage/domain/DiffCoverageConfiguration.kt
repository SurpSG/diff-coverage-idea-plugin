package com.github.surpsg.diffcoverage.domain

data class DiffCoverageConfiguration(
    val project: String,
    val projectDir: String,
    val execFiles: List<String>,
    val classes: List<String>,
    val reportsRoot: String
)
