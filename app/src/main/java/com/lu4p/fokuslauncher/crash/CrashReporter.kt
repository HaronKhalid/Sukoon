package com.lu4p.fokuslauncher.crash

import android.content.Context
import java.io.File

/**
 * Saves the last uncaught exception to a private file so the next start can show it. The report
 * never leaves the device on its own: the user chooses whether to copy it.
 */
object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"
    private const val MAX_CHARS = 20_000

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(appContext, thread, error) }
            previous?.uncaughtException(thread, error)
        }
    }

    internal fun format(versionName: String, thread: String, error: Throwable, nowMillis: Long): String =
            buildString {
                append("Sukoon crash report\n")
                append("version: ").append(versionName).append('\n')
                append("time: ").append(java.util.Date(nowMillis)).append('\n')
                append("thread: ").append(thread).append('\n')
                append("device: ").append(android.os.Build.MANUFACTURER).append(' ')
                        .append(android.os.Build.MODEL).append(", Android ")
                        .append(android.os.Build.VERSION.RELEASE).append('\n')
                append('\n').append(error.stackTraceToString())
            }.take(MAX_CHARS)

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val version = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
        File(context.filesDir, FILE_NAME)
                .writeText(format(version, thread.name, error, System.currentTimeMillis()))
    }

    /** The saved report, or null if there is none. */
    fun pending(context: Context): String? =
            File(context.filesDir, FILE_NAME).takeIf { it.isFile }?.readText()?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }
}
