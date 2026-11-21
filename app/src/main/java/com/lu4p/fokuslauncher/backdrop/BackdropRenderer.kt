package com.lu4p.fokuslauncher.backdrop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas as GfxCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Turns a [BackdropScene] into pixels. Cheap: one pass of a few hundred primitives. */
object BackdropRenderer {
    /** Longest rendered edge; larger targets are drawn from this bitmap scaled up (backdrops are soft). */
    const val MAX_RENDER_EDGE = 1280

    fun render(scene: BackdropScene): ImageBitmap {
        val bitmap = ImageBitmap(scene.width, scene.height)
        CanvasDrawScope().draw(
                Density(1f),
                LayoutDirection.Ltr,
                GfxCanvas(bitmap),
                Size(scene.width.toFloat(), scene.height.toFloat()),
        ) {
            scene.ops.forEach { drawOp(it) }
        }
        return bitmap
    }

    private fun col(rgb: Int, alpha: Float = 1f) = Color(0xFF000000.toInt() or rgb).copy(alpha = alpha.coerceIn(0f, 1f))

    private fun smooth(points: List<Float>): Path {
        val p = Path()
        if (points.size < 4) return p
        p.moveTo(points[0], points[1])
        var i = 2
        while (i + 3 < points.size) {
            val mx = (points[i] + points[i + 2]) / 2f
            val my = (points[i + 1] + points[i + 3]) / 2f
            p.quadraticTo(points[i], points[i + 1], mx, my)
            i += 2
        }
        p.lineTo(points[points.size - 2], points[points.size - 1])
        return p
    }

    private fun DrawScope.drawOp(op: BackdropOp) {
        when (op) {
            is BackdropOp.VGradient ->
                    drawRect(Brush.verticalGradient(
                            *op.stops.zip(op.colors).map { (s, c) -> s to col(c) }.toTypedArray()))
            is BackdropOp.Blob ->
                    drawCircle(
                            Brush.radialGradient(
                                    listOf(col(op.color, op.alpha), col(op.color, 0f)),
                                    center = Offset(op.cx, op.cy),
                                    radius = op.r,
                            ),
                            radius = op.r,
                            center = Offset(op.cx, op.cy),
                    )
            is BackdropOp.Silhouette -> {
                val path = smooth(op.points)
                path.lineTo(size.width + 4f, size.height + 4f)
                path.lineTo(-4f, size.height + 4f)
                path.close()
                drawPath(path, col(op.color, op.alpha))
            }
            is BackdropOp.Rect ->
                    drawRect(col(op.color, op.alpha), Offset(op.l, op.t), Size(op.r - op.l, op.b - op.t))
            is BackdropOp.Dot -> drawCircle(col(op.color, op.alpha), max(op.r, 0.5f), Offset(op.x, op.y))
            is BackdropOp.Ribbon ->
                    drawPath(smooth(op.points), col(op.color, op.alpha),
                            style = Stroke(width = max(op.width, 1f), cap = StrokeCap.Round))
            is BackdropOp.Curtain -> {
                val n = op.points.size / 2
                val step = size.width / max(n - 1, 1)
                for (i in 0 until n) {
                    val x = op.points[2 * i]
                    val y = op.points[2 * i + 1]
                    drawLine(
                            Brush.verticalGradient(
                                    listOf(col(op.color, op.alpha), col(op.color, 0f)),
                                    startY = y,
                                    endY = y + op.length,
                            ),
                            Offset(x, y),
                            Offset(x, y + op.length),
                            strokeWidth = step + 1f,
                    )
                }
            }
        }
    }
}

/** Small LRU of rendered backdrops keyed by (id, w, h). Rendering happens off the main thread. */
object BackdropCache {
    private const val MAX_ENTRIES = 8
    private val map = object : LinkedHashMap<String, ImageBitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > MAX_ENTRIES
    }

    private fun key(id: String, w: Int, h: Int) = "$id@${w}x$h"

    @Synchronized
    fun peek(id: String, w: Int, h: Int): ImageBitmap? = map[key(id, w, h)]

    /** Fit (w, h) so the longer edge is at most [BackdropRenderer.MAX_RENDER_EDGE], keeping the ratio. */
    fun renderSize(w: Int, h: Int): IntSize {
        val s = minOf(1f, BackdropRenderer.MAX_RENDER_EDGE / max(w, h).toFloat())
        return IntSize(
                (w * s).toInt().coerceAtLeast(Backdrops.MIN_SIZE),
                (h * s).toInt().coerceAtLeast(Backdrops.MIN_SIZE),
        )
    }

    suspend fun get(id: String, w: Int, h: Int): ImageBitmap? {
        val r = renderSize(w, h)
        peek(id, r.width, r.height)?.let { return it }
        return withContext(Dispatchers.Default) {
            val scene = Backdrops.generate(id, r.width, r.height) ?: return@withContext null
            val bmp = BackdropRenderer.render(scene)
            synchronized(this@BackdropCache) { map[key(id, r.width, r.height)] = bmp }
            bmp
        }
    }
}

/**
 * Paints the backdrop [id] filling its box at [intensity] (0..1, opacity over what is behind). An
 * unknown or empty id paints nothing. The bitmap is produced once per size on a background thread.
 */
@Composable
fun BackdropLayer(id: String, modifier: Modifier = Modifier, intensity: Float = 1f) {
    if (Backdrops.info(id) == null) return
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bitmap by produceState<ImageBitmap?>(null, id, size) {
        value = if (size.width > 0 && size.height > 0) {
            val r = BackdropCache.renderSize(size.width, size.height)
            BackdropCache.peek(id, r.width, r.height) ?: BackdropCache.get(id, size.width, size.height)
        } else null
    }
    Box(modifier.fillMaxSize().onSizeChanged { size = it }) {
        val bmp = bitmap
        if (bmp != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawImage(bmp, dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()),
                        alpha = intensity.coerceIn(0f, 1f))
            }
        }
    }
}
