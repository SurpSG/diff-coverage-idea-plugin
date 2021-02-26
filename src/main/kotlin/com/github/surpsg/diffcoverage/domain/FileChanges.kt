package com.github.surpsg.diffcoverage.domain

data class FileChange(
    val path: String,
    val changedRanges: Set<ChangeRange>
)

data class ChangeRange(val from: Int, val to: Int)
