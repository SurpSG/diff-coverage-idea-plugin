package com.github.surpsg.diffcoverage.services.notifications

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.properties.PLUGIN_NAME
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class BalloonNotificationService(private val project: Project) {

    fun notify(
        group: NotificationGroup = DIFF_COVERAGE_BALLOON,
        notificationType: NotificationType = NotificationType.INFORMATION,
        notificationListener: NotificationListener? = null,
        message: String
    ) {
        Notifications.Bus.notify(
            group.createNotification(
                DiffCoverageBundle.message(PLUGIN_NAME),
                message,
                notificationType,
                notificationListener
            ),
            project
        )
    }

    companion object {
        val DIFF_COVERAGE_BALLOON = NotificationGroup(
            "diff.coverage.notification.balloon",
            NotificationDisplayType.BALLOON,
            true
        )
    }
}
