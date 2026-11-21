package com.lu4p.fokuslauncher.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lu4p.fokuslauncher.MainActivity
import com.lu4p.fokuslauncher.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * One inexact alarm for the next due reminder. Deliberately no exact-alarm permission: delivery may
 * be a little late under Doze, which is fine for reminders.
 */
@Singleton
class ReminderAlarmScheduler @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun schedule(atMs: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, pending(create = true) ?: return)
    }

    fun cancel() {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pending(create = false) ?: return
        am.cancel(pi)
        pi.cancel()
    }

    private fun pending(create: Boolean): PendingIntent? =
            PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE,
                    Intent(context, ReminderAlarmReceiver::class.java).setAction(ReminderAlarmReceiver.ACTION_FIRE),
                    PendingIntent.FLAG_IMMUTABLE or
                            if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE,
            )

    private companion object {
        const val REQUEST_CODE = 4301
    }
}

@Singleton
class ReminderNotifier @Inject constructor(@param:ApplicationContext private val context: Context) {
    fun canPost(): Boolean =
            (Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun post(r: Reminder, showFullText: Boolean) {
        if (!canPost()) return
        ensureChannel()
        val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val title = if (showFullText) r.title else context.getString(R.string.reminders_notification_private_title)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_mono)
                .setContentTitle(r.title)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(open)
                .setAutoCancel(true)
                .addAction(0, context.getString(R.string.reminders_action_done), action(r.id, ReminderActionReceiver.ACTION_DONE))
                .addAction(0, context.getString(R.string.reminders_action_snooze), action(r.id, ReminderActionReceiver.ACTION_SNOOZE))
        if (r.note.isNotBlank()) builder.setContentText(r.note)
        if (showFullText) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        } else {
            // PRIVATE: after unlock the full text shows; on the lock screen only the public version.
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            builder.setPublicVersion(
                    NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher_mono)
                            .setContentTitle(title)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .build(),
            )
        }
        NotificationManagerCompat.from(context).notify(notificationId(r.id), builder.build())
    }

    fun cancel(id: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(id))
    }

    private fun action(id: String, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                    context,
                    notificationId(id) xor action.hashCode(),
                    Intent(context, ReminderActionReceiver::class.java).setAction(action)
                            .putExtra(ReminderActionReceiver.EXTRA_ID, id),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.reminders_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
                        .apply {
                            description = context.getString(R.string.reminders_channel_description)
                            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                        },
        )
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        fun notificationId(id: String): Int = 0x5200_0000 or (id.hashCode() and 0x00FF_FFFF)
    }
}

/** Single place that edits reminders and keeps the alarm pointing at the next due one. */
@Singleton
class ReminderManager @Inject constructor(
        private val repository: ReminderRepository,
        private val alarms: ReminderAlarmScheduler,
        private val notifier: ReminderNotifier,
) {
    suspend fun save(r: Reminder) {
        repository.update { list -> if (list.any { it.id == r.id }) list.map { if (it.id == r.id) r else it } else list + r }
        reschedule()
    }

    suspend fun delete(id: String) {
        repository.update { list -> list.filterNot { it.id == id } }
        notifier.cancel(id)
        reschedule()
    }

    /** Tick/untick from the list. Ticking a repeating reminder moves it to its next occurrence. */
    suspend fun toggleDone(id: String, nowMs: Long = System.currentTimeMillis()) {
        repository.update { list ->
            list.map {
                when {
                    it.id != id -> it
                    it.done -> it.copy(done = false)
                    else -> ReminderMath.complete(it, nowMs)
                }
            }
        }
        notifier.cancel(id)
        reschedule()
    }

    /** "Done" in the notification: a repeating reminder has already rolled forward when it fired. */
    suspend fun doneFromNotification(id: String) {
        repository.update { list -> list.map { if (it.id == id && it.repeat == RepeatRule.NONE) it.copy(done = true) else it } }
        notifier.cancel(id)
        reschedule()
    }

    suspend fun snooze(id: String, minutes: Int, nowMs: Long = System.currentTimeMillis()) {
        repository.update { list ->
            list.map { if (it.id == id) it.copy(snoozeUntilMs = nowMs + minutes * 60_000L, done = false) else it }
        }
        notifier.cancel(id)
        reschedule()
    }

    /** Called by the alarm: announce what is due, roll repeats forward, then arm the next alarm. */
    suspend fun onAlarm(nowMs: Long = System.currentTimeMillis()) {
        val settings = repository.settingsSnapshot()
        val due = ReminderMath.dueNow(repository.snapshot(), nowMs, repository.notifiedUpTo())
        due.forEach { notifier.post(it, settings.showFullTextOnLock) }
        if (due.isNotEmpty()) {
            val ids = due.map { it.id }.toSet()
            repository.update { list -> list.map { if (it.id in ids) ReminderMath.afterFired(it, nowMs) else it } }
        }
        repository.setNotifiedUpTo(nowMs)
        reschedule(nowMs)
    }

    suspend fun reschedule(nowMs: Long = System.currentTimeMillis()) {
        val next = ReminderMath.nextToNotify(repository.snapshot(), nowMs, repository.notifiedUpTo())
        if (next == null) alarms.cancel() else alarms.schedule(ReminderMath.alarmTime(next, nowMs))
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun reminderManager(): ReminderManager
}

private fun runAsync(context: Context, block: suspend (ReminderManager) -> Unit, receiver: BroadcastReceiver) {
    val pending = receiver.goAsync()
    val manager = EntryPointAccessors.fromApplication(context.applicationContext, ReminderEntryPoint::class.java).reminderManager()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            block(manager)
        } finally {
            pending.finish()
        }
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        runAsync(context, { it.onAlarm() }, this)
    }

    companion object {
        const val ACTION_FIRE = "com.lu4p.fokuslauncher.reminders.FIRE"
    }
}

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        when (intent.action) {
            ACTION_DONE -> runAsync(context, { it.doneFromNotification(id) }, this)
            ACTION_SNOOZE -> runAsync(context, { it.snooze(id, SNOOZE_MINUTES) }, this)
        }
    }

    companion object {
        const val ACTION_DONE = "com.lu4p.fokuslauncher.reminders.DONE"
        const val ACTION_SNOOZE = "com.lu4p.fokuslauncher.reminders.SNOOZE"
        const val EXTRA_ID = "reminder_id"
        const val SNOOZE_MINUTES = 10
    }
}

/** Re-arms the alarm after a reboot. Does nothing else. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runAsync(context, { it.onAlarm() }, this)
    }
}
