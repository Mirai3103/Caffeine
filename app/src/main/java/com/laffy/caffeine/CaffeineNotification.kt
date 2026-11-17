package com.laffy.caffeine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

const val CAFFEINE_NOTIFICATION_ID = 1
private const val CHANNEL_ID = "caffeine"

/**
 * Ongoing notification for [session]. The clock is a system-drawn chronometer (counting down to the
 * end for timed sessions, up from the start for ∞), so the app does no periodic work to update it.
 */
fun buildCaffeineNotification(context: Context, session: Session): Notification {
    val nm = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= 26 && nm.getNotificationChannel(CHANNEL_ID) == null) {
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
    }
    val open = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )
    val stop = serviceAction(context, 1, CaffeineController.ACTION_STOP)
    val endAt = session.endAt

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_coffee)
        .setContentIntent(open)
        .setShowWhen(true)
        .setUsesChronometer(true)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)

    if (endAt != null) {
        builder.setContentTitle(context.getString(R.string.notif_title))
            .setWhen(endAt)
            .setChronometerCountDown(true)
            .addAction(0, context.getString(R.string.notif_extend), serviceAction(context, 2, CaffeineController.ACTION_EXTEND))
    } else {
        builder.setContentTitle(context.getString(R.string.notif_title_infinite))
            .setWhen(session.startAt)
            .setChronometerCountDown(false)
    }
    return builder.addAction(0, context.getString(R.string.notif_stop), stop).build()
}

private fun serviceAction(context: Context, requestCode: Int, action: String): PendingIntent =
    PendingIntent.getService(
        context, requestCode,
        Intent(context, CaffeineService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE
    )
