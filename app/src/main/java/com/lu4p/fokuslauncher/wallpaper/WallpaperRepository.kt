package com.lu4p.fokuslauncher.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.core.graphics.createBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Original, code-generated AMOLED-friendly backgrounds (no third-party imagery). */
enum class BuiltInWallpaper(val id: String, val top: Int, val bottom: Int) {
    PURE_BLACK("black", 0xFF000000.toInt(), 0xFF000000.toInt()),
    MIDNIGHT("midnight", 0xFF000000.toInt(), 0xFF0B1426.toInt()),
    EMBER("ember", 0xFF000000.toInt(), 0xFF2A0E08.toInt()),
    FOREST("forest", 0xFF000000.toInt(), 0xFF0A2016.toInt()),
    DUSK("dusk", 0xFF0A0A14.toInt(), 0xFF2A1638.toInt()),
}

/** Result of applying a wallpaper; [lockApplied] is false when the lock screen was not changed. */
data class ApplyResult(val homeApplied: Boolean, val lockApplied: Boolean)

@Singleton
class WallpaperRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val dir get() = File(context.filesDir, "wallpapers").apply { mkdirs() }

    /** Recent picks stored as app-private files, newest first. */
    suspend fun recents(): List<File> =
        withContext(Dispatchers.IO) {
            dir.listFiles { f -> f.extension == "jpg" }?.sortedByDescending { it.lastModified() }.orEmpty()
        }

    /** Copies the picked image (downscaled) into private storage; returns the new file or null. */
    suspend fun saveReference(uri: Uri): File? =
        withContext(Dispatchers.IO) {
            runCatching {
                    val bmp = decode(uri, 2048) ?: return@runCatching null
                    val file = File(dir, "wp_${System.currentTimeMillis()}.jpg")
                    file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    bmp.recycle()
                    val existing = recents().map { it.name }
                    val (_, evicted) = WallpaperMath.pushRecent(existing, file.name, MAX_RECENTS)
                    evicted.forEach { File(dir, it).delete() }
                    file
                }
                .getOrNull()
        }

    suspend fun delete(file: File) {
        withContext(Dispatchers.IO) { file.delete() }
    }

    suspend fun decodeFile(file: File, maxEdge: Int): Bitmap? =
        withContext(Dispatchers.IO) { runCatching { decode(Uri.fromFile(file), maxEdge) }.getOrNull() }

    fun builtIn(w: BuiltInWallpaper): Bitmap {
        val m = context.resources.displayMetrics
        val bmp = createBitmap(m.widthPixels, m.heightPixels, Bitmap.Config.ARGB_8888)
        val paint =
            Paint().apply {
                shader = LinearGradient(0f, 0f, 0f, bmp.height.toFloat(), w.top, w.bottom, Shader.TileMode.CLAMP)
            }
        Canvas(bmp).drawRect(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat(), paint)
        return bmp
    }

    /** Centre-crops [source] to the screen aspect and sets it. Lock-screen failures are reported. */
    suspend fun apply(source: Bitmap, home: Boolean, lock: Boolean): ApplyResult =
        withContext(Dispatchers.IO) {
            val m = context.resources.displayMetrics
            val c = WallpaperMath.centerCrop(source.width, source.height, m.widthPixels, m.heightPixels)
            val cropped = Bitmap.createBitmap(source, c.left, c.top, c.width, c.height)
            val wm = WallpaperManager.getInstance(context)
            fun set(flag: Int) = runCatching { wm.setBitmap(cropped, null, true, flag) }.isSuccess
            ApplyResult(
                homeApplied = home && set(WallpaperManager.FLAG_SYSTEM),
                lockApplied = lock && set(WallpaperManager.FLAG_LOCK),
            )
        }

    /** Average colour of a 32px downscale. */
    fun averageOf(bitmap: Bitmap): Int? {
        val small = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        val px = IntArray(32 * 32)
        small.getPixels(px, 0, 32, 0, 0, 32, 32)
        if (small !== bitmap) small.recycle()
        return WallpaperMath.averageColor(px)
    }

    private fun decode(uri: Uri, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0) return null
        val opts =
            BitmapFactory.Options().apply {
                inSampleSize = WallpaperMath.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
            }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    companion object {
        const val MAX_RECENTS = 8
    }
}
