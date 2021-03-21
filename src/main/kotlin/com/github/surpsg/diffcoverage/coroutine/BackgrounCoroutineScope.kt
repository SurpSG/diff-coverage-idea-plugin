package com.github.surpsg.diffcoverage.coroutine

import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher

val BACKGROUND_SCOPE: CoroutineScope = CoroutineScope(AppExecutorUtil.getAppExecutorService().asCoroutineDispatcher())
