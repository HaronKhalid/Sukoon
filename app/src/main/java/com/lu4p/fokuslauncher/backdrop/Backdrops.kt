package com.lu4p.fokuslauncher.backdrop

import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Procedural wallpapers drawn from nothing but maths: gradient meshes, dunes, ridges, skylines,
 * stars, aurora, washes and grain. The result depends only on (id, width, height), never on the
 * clock or the device, so Style previews and the real home background always match.
 */
object Backdrops {
    const val NONE = ""
    const val MIN_SIZE = 16
    const val MAX_SIZE = 4096

    val all: List<BackdropInfo> =
            listOf(
                    BackdropInfo("blobs-dusk", true, listOf(0x140B2E, 0x7A3FD1, 0xE0508F)),
                    BackdropInfo("blobs-peach", false, listOf(0xFFF1E8, 0xFFB59A, 0xFF8FA6)),
                    BackdropInfo("dunes-sunset", true, listOf(0x1F1030, 0xE08A4A, 0x5A2340)),
                    BackdropInfo("dunes-sand", false, listOf(0xF8E8CC, 0xE0B97B, 0xD4A566)),
                    BackdropInfo("ridges-dusk", true, listOf(0x1B1840, 0xF0A070, 0x3B2B5C)),
                    BackdropInfo("ridges-mist", true, listOf(0x0E2A2A, 0x2C5B55, 0x16332F)),
                    BackdropInfo("city-glow", true, listOf(0x0B0B2A, 0xC0407A, 0xFFD27A)),
                    BackdropInfo("city-dawn", true, listOf(0x121C3A, 0xF5A67A, 0x0A0F20)),
                    BackdropInfo("starfield", true, listOf(0x02030A, 0x0B1030, 0xFFFFFF)),
                    BackdropInfo("aurora-ribbons", true, listOf(0x040B1A, 0x3CE0A0, 0x8A5CF0)),
                    BackdropInfo("wash-rose", false, listOf(0xFBF1F0, 0xF5B5C4, 0xF7D4B5)),
                    BackdropInfo("wash-ocean", false, listOf(0xEEF6F8, 0x9FD0E0, 0xBCC8F0)),
                    BackdropInfo("paper-grain", false, listOf(0xF6F0E4, 0xE9DFCB, 0xB8A98C)),
                    BackdropInfo("grain-slate", true, listOf(0x1C2028, 0x2A303C, 0x5A6578)),
            )

    fun info(id: String?): BackdropInfo? = all.firstOrNull { it.id == id }

    /** Null for [NONE] or an unknown id (callers then paint nothing). */
    fun generate(id: String?, width: Int, height: Int): BackdropScene? {
        if (id == null || info(id) == null) return null
        val w = width.coerceIn(MIN_SIZE, MAX_SIZE)
        val h = height.coerceIn(MIN_SIZE, MAX_SIZE)
        val g = Gen(id, w, h)
        when (id) {
            "blobs-dusk" -> g.blobs(listOf(0x140B2E, 0x2B1250), listOf(0x7A3FD1, 0xE0508F, 0x3F7BE0), 0.55f)
            "blobs-peach" -> g.blobs(listOf(0xFFF1E8, 0xFFDCCF), listOf(0xFFB59A, 0xFF8FA6, 0xFFD9A0), 0.5f)
            "dunes-sunset" -> g.dunes(listOf(0x1F1030, 0x8E3A3A, 0xE08A4A), listOf(0x5A2340, 0x3C1833, 0x24102A, 0x1A0B1E), 0xFFC27A)
            "dunes-sand" -> g.dunes(listOf(0xF8E8CC, 0xF3DDB5), listOf(0xEBCF9C, 0xE0B97B, 0xD4A566), 0xFFF6DA)
            "ridges-dusk" -> g.ridges(listOf(0x1B1840, 0x7A4A7E, 0xF0A070), listOf(0x3B2B5C, 0x2A1F48, 0x171230), true)
            "ridges-mist" -> g.ridges(listOf(0x0E2A2A, 0x1E4540, 0x2C5B55), listOf(0x1F4440, 0x16332F, 0x0C2220), false)
            "city-glow" -> g.city(listOf(0x0B0B2A, 0x3A1C5E, 0xC0407A), 0x0A0A18, 0xFFD27A)
            "city-dawn" -> g.city(listOf(0x121C3A, 0x40507F, 0xF5A67A), 0x0A0F20, 0xFFE2A8)
            "starfield" -> g.stars()
            "aurora-ribbons" -> g.aurora()
            "wash-rose" -> g.washes(0xFBF1F0, listOf(0xF5B5C4, 0xF7D4B5, 0xE9B8D8))
            "wash-ocean" -> g.washes(0xEEF6F8, listOf(0x9FD0E0, 0xBCC8F0, 0xC6E8DA))
            "paper-grain" -> g.paper()
            "grain-slate" -> g.slate()
        }
        return BackdropScene(w, h, g.ops)
    }

    private class Gen(id: String, val w: Int, val h: Int) {
        val ops = ArrayList<BackdropOp>()
        private val rnd = Random(id.hashCode().toLong() * 7919L + 104729L)
        private val unit = min(w, h).toFloat()

        private fun f(lo: Float, hi: Float) = lo + rnd.nextFloat() * (hi - lo)

        private fun sky(colors: List<Int>) {
            val n = colors.size
            ops += BackdropOp.VGradient(colors, List(n) { it / (n - 1f) })
        }

        /** Layered sines normalised to roughly -1..1; peaky = ridged (mountain-like). */
        private fun terrain(baseY: Float, amp: Float, peaky: Boolean): List<Float> {
            val ph = FloatArray(4) { f(0f, (2 * PI).toFloat()) }
            val fr = floatArrayOf(f(0.8f, 1.4f), f(1.8f, 2.6f), f(3.5f, 5f), f(7f, 11f))
            val steps = 40
            val pts = ArrayList<Float>(2 * (steps + 3))
            for (i in -1..steps + 1) {
                val x = i * w / steps.toFloat()
                var v = 0f
                var norm = 0f
                for (k in 0 until 4) {
                    val wgt = 1f / (1 shl k)
                    val s = sin((x / w * fr[k] * 2 * PI + ph[k])).toFloat()
                    v += wgt * (if (peaky) 1f - 2f * abs(s) else s)
                    norm += wgt
                }
                pts += x
                pts += (baseY + amp * v / norm).coerceIn(-unit * 0.1f, h + unit * 0.1f)
            }
            return pts
        }

        fun blobs(bg: List<Int>, colors: List<Int>, strength: Float) {
            sky(bg)
            repeat(7) { i ->
                ops += BackdropOp.Blob(f(-0.1f, 1.1f) * w, f(0f, 1f) * h, f(0.35f, 0.8f) * max(w, h) * 0.6f,
                        colors[i % colors.size], strength * f(0.5f, 1f))
            }
        }

        fun dunes(skyColors: List<Int>, layers: List<Int>, sun: Int) {
            sky(skyColors)
            ops += BackdropOp.Blob(f(0.25f, 0.75f) * w, h * 0.55f, unit * 0.55f, sun, 0.45f)
            layers.forEachIndexed { i, c ->
                val base = h * (0.6f + 0.1f * i)
                ops += BackdropOp.Silhouette(terrain(base, h * 0.05f, false), c, 1f)
            }
        }

        fun ridges(skyColors: List<Int>, layers: List<Int>, glow: Boolean) {
            sky(skyColors)
            if (glow) ops += BackdropOp.Blob(f(0.3f, 0.7f) * w, h * 0.5f, unit * 0.7f, 0xF0A070, 0.35f)
            layers.forEachIndexed { i, c ->
                val base = h * (0.52f + 0.12f * i)
                ops += BackdropOp.Silhouette(terrain(base, h * (0.1f - 0.015f * i), true), c, 1f)
                if (i < layers.size - 1) // atmospheric mist between layers
                    ops += BackdropOp.Blob(w / 2f, base + h * 0.06f, max(w, h) * 0.55f, skyColors.last(), 0.12f)
            }
        }

        fun city(skyColors: List<Int>, building: Int, window: Int) {
            sky(skyColors)
            ops += BackdropOp.Blob(w / 2f, h * 0.72f, max(w, h) * 0.6f, skyColors.last(), 0.5f)
            for (layer in 0 until 2) {
                val c = if (layer == 0) blendHex(building, skyColors[1], 0.35f) else building
                var x = -unit * 0.02f
                val horizon = h * (0.8f - 0.06f * (1 - layer))
                while (x < w) {
                    val bw = f(0.05f, 0.13f) * w
                    val bh = f(0.06f, if (layer == 0) 0.2f else 0.15f) * h
                    ops += BackdropOp.Rect(x, horizon - bh, x + bw, h.toFloat(), c, 1f)
                    if (layer == 1) {
                        val cols = max(1, (bw / (unit * 0.03f)).toInt())
                        val rows = max(1, (bh / (unit * 0.04f)).toInt())
                        for (cx in 0 until cols) for (ry in 0 until rows) {
                            if (rnd.nextFloat() < 0.28f) {
                                val wx = x + bw * (cx + 0.5f) / cols
                                val wy = horizon - bh + bh * (ry + 0.5f) / rows
                                ops += BackdropOp.Rect(wx - unit * 0.004f, wy - unit * 0.006f, wx + unit * 0.004f,
                                        wy + unit * 0.006f, window, f(0.4f, 0.9f))
                            }
                        }
                    }
                    x += bw + f(0f, 0.012f) * w
                }
            }
        }

        fun stars() {
            sky(listOf(0x02030A, 0x070B22, 0x0B1030))
            ops += BackdropOp.Blob(f(0.2f, 0.8f) * w, f(0.2f, 0.5f) * h, unit * 0.9f, 0x4B3A9A, 0.25f)
            ops += BackdropOp.Blob(f(0.2f, 0.8f) * w, f(0.5f, 0.8f) * h, unit * 0.7f, 0x1F5F9A, 0.2f)
            repeat(260) {
                val big = rnd.nextFloat() < 0.06f
                ops += BackdropOp.Dot(f(0f, 1f) * w, f(0f, 1f) * h,
                        unit * (if (big) f(0.0025f, 0.004f) else f(0.0006f, 0.0018f)),
                        if (rnd.nextFloat() < 0.2f) 0xBFD6FF else 0xFFFFFF, f(0.35f, 1f))
            }
        }

        fun aurora() {
            sky(listOf(0x040B1A, 0x07182A, 0x0A2030))
            val colors = listOf(0x3CE0A0, 0x40C8F0, 0x8A5CF0)
            for (i in 0 until 3) {
                val base = h * (0.22f + 0.14f * i)
                val pts = terrain(base, h * 0.07f, false)
                ops += BackdropOp.Curtain(pts, h * 0.3f, colors[i], 0.4f)
                ops += BackdropOp.Ribbon(pts, unit * 0.02f, colors[i], 0.55f)
            }
            repeat(70) {
                ops += BackdropOp.Dot(f(0f, 1f) * w, f(0f, 0.7f) * h, unit * f(0.0006f, 0.0014f), 0xFFFFFF, f(0.3f, 0.9f))
            }
            ops += BackdropOp.Silhouette(terrain(h * 0.93f, h * 0.025f, true), 0x030710, 1f)
        }

        fun washes(paper: Int, colors: List<Int>) {
            sky(listOf(paper, paper))
            repeat(9) { i ->
                ops += BackdropOp.Blob(f(-0.1f, 1.1f) * w, f(0f, 1f) * h, f(0.3f, 0.7f) * max(w, h) * 0.55f,
                        colors[i % colors.size], f(0.35f, 0.7f))
            }
            grain(0x8A7F70, 500, 0.08f)
        }

        fun paper() {
            sky(listOf(0xF6F0E4, 0xEFE6D4))
            repeat(60) {
                val x = f(0f, 1f) * w
                val y = f(0f, 1f) * h
                ops += BackdropOp.Ribbon(
                        listOf(x, y, x + f(-0.06f, 0.06f) * w, y + f(0.01f, 0.04f) * h, x + f(-0.1f, 0.1f) * w, y + f(0.03f, 0.07f) * h),
                        unit * 0.0015f, 0xB8A98C, 0.12f)
            }
            grain(0x6B5B3E, 1600, 0.1f)
        }

        fun slate() {
            sky(listOf(0x1C2028, 0x222732, 0x2A303C))
            ops += BackdropOp.Blob(w * 0.7f, h * 0.2f, max(w, h) * 0.6f, 0x5A6578, 0.3f)
            ops += BackdropOp.Blob(w * 0.2f, h * 0.85f, max(w, h) * 0.5f, 0x000000, 0.35f)
            grain(0xFFFFFF, 1600, 0.07f)
        }

        private fun grain(color: Int, count: Int, alpha: Float) {
            repeat(count) {
                ops += BackdropOp.Dot(f(0f, 1f) * w, f(0f, 1f) * h, unit * f(0.0004f, 0.0012f), color, alpha * f(0.4f, 1f))
            }
        }

        private fun blendHex(a: Int, b: Int, t: Float): Int {
            fun ch(s: Int) = ((((a shr s) and 255) * (1 - t) + ((b shr s) and 255) * t).toInt()) and 255
            return (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
        }
    }
}
