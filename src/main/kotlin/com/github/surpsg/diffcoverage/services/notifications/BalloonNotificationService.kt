package com.github.surpsg.diffcoverage.services.notifications

import com.github.surpsg.diffcoverage.DiffCoverageBundle
import com.github.surpsg.diffcoverage.properties.PLUGIN_NAME
import com.intellij.notification.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class BalloonNotificationService(private val project: Project) {

    fun notify(
        notificationType: NotificationType = NotificationType.INFORMATION,
        notificationListener: NotificationListener? = null,
        message: String
    ) {
        Notifications.Bus.notify(
            getNotificationGroup().createNotification(
                DiffCoverageBundle.message(PLUGIN_NAME),
                message,
                notificationType,
                notificationListener
            ),
            project
        )
    }

    private fun getNotificationGroup(): NotificationGroup {
        return NotificationGroupManager.getInstance().getNotificationGroup("diff.coverage.notification")
    }
}
