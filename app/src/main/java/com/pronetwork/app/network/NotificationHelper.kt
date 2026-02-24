package com.pronetwork.app.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pronetwork.app.R

object NotificationHelper {

    private const val CHANNEL_ADMIN = "approval_requests"
    private const val CHANNEL_USER = "my_requests_updates"

    /**
     * Call once at app startup to create notification channels.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            // Channel for Admin — new approval requests
            val adminChannel = NotificationChannel(
                CHANNEL_ADMIN,
                context.getString(R.string.notif_channel_admin_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_admin_desc)
            }

            // Channel for User — request status updates
            val userChannel = NotificationChannel(
                CHANNEL_USER,
                context.getString(R.string.notif_channel_user_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_user_desc)
            }

            manager.createNotificationChannels(listOf(adminChannel, userChannel))
        }
    }

    /**
     * Notify Admin: new pending request(s) arrived.
     */
    fun notifyAdminNewRequests(context: Context, count: Int, latestTargetName: String?) {
        val title = context.getString(R.string.notif_admin_title)
        val body = if (count == 1) {
            context.getString(R.string.notif_admin_single, latestTargetName ?: "—")
        } else {
            context.getString(R.string.notif_admin_multiple, count)
        }

        showNotification(context, CHANNEL_ADMIN, 1001, title, body)
    }

    /**
     * Notify User: one of their requests changed status.
     */
    fun notifyUserStatusChanged(
        context: Context,
        targetName: String?,
        newStatus: String
    ) {
        val title = context.getString(R.string.notif_user_title)
        val statusText = when (newStatus) {
            "APPROVED" -> context.getString(R.string.notif_status_approved)
            "REJECTED" -> context.getString(R.string.notif_status_rejected)
            else -> newStatus
        }
        val body = context.getString(
            R.string.notif_user_body,
            targetName ?: "—",
            statusText
        )

        showNotification(
            context, CHANNEL_USER,
            (targetName.hashCode() + newStatus.hashCode()),
            title, body
        )
    }

    private fun showNotification(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        body: String
    ) {
        // Deep link: open MainActivity (which will show the requests screen)
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_screen", "admin")
            }

        val pendingIntent = PendingIntent.getActivity(
            context, notifId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notifId, notification)
    }
}
