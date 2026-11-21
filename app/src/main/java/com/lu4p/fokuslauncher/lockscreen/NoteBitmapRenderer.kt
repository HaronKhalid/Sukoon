package com.lu4p.fokuslauncher.lockscreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.graphics.createBitmap
import com.lu4p.fokuslauncher.data.font.CustomFontStore
import com.lu4p.fokuslauncher.notes.NoteAlignment
import com.lu4p.fokuslauncher.notes.NoteCard
import com.lu4p.fokuslauncher.notes.NoteDecoration
import com.lu4p.fokuslauncher.notes.NoteStyles
import com.lu4p.fokuslauncher.notes.NoteWidgetStyle
import com.lu4p.fokuslauncher.typography.FontChoice
import com.lu4p.fokuslauncher.typography.FontRegistry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LockBackdrop {
    data class Solid(val argb: Int) : LockBackdrop
    data class Gradient(val top: Int, val bottom: Int) : LockBackdrop
    /** A picked photo; it is centre-cropped to the screen and dimmed slightly for legibility. */
    data class Photo(val bitmap: Bitmap) : LockBackdrop
}

data class LockClockLine(val time: String, val date: String)

data class LockRenderSpec(
        val width: Int,
        val height: Int,
        /** Note text before decoration (checklist boxes / journal stamp are added here). */
        val text: String,
        val look: NoteWidgetStyle,
        val decoration: NoteDecoration,
        val stamp: String?,
        val backdrop: LockBackdrop,
        val position: LockPosition,
        val scale: Float,
        val clock: LockClockLine?,
        val typeface: Typeface,
)

/**
 * Draws a note onto a bitmap with StaticLayout and Canvas. Layout decisions (wrapping, line
 * limits, script line boost, placement) come from [LockLayoutMath]; this class only paints.
 */
object NoteBitmapRenderer {
    fun render(spec: LockRenderSpec): Bitmap {
        val w = spec.width.coerceAtLeast(16)
        val h = spec.height.coerceAtLeast(16)
        val bmp = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgLuma = drawBackdrop(canvas, spec.backdrop, w, h)

        val look = spec.look.coerced()
        val a = look.appearance
        val ppd = LockLayoutMath.pxPerDp(w)
        val sizePx = LockLayoutMath.textSizePx(a.fontSizeSp, w, spec.scale)
        val padPx = a.paddingDp * ppd
        val boost = LockLayoutMath.lineBoost(a.fontFamily, spec.text)
        val resolved = NoteCard.resolve(look, a)
        val fillLuma =
                if ((resolved.backgroundArgb ushr 24) > 0x80) LockLayoutMath.luma(resolved.backgroundArgb)
                else bgLuma
        val textColor =
                when {
                    a.textColor != 0 -> a.textColor
                    resolved.defaultTextArgb != null -> resolved.defaultTextArgb
                    else -> LockLayoutMath.defaultTextColor(fillLuma)
                }
        val paint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = spec.typeface
                    textSize = sizePx
                    color = textColor
                    if (spec.decoration == NoteDecoration.GLOW) setShadowLayer(sizePx * 0.6f, 0f, 0f, textColor)
                }

        val cardW = LockLayoutMath.cardWidthPx(w, look.widthFraction)
        val textW = LockLayoutMath.textWidthPx(cardW, padPx)
        val lineH = LockLayoutMath.lineHeightPx(sizePx, a.lineSpacing, boost)
        val quoteH = if (spec.decoration == NoteDecoration.QUOTE_MARK) sizePx * 1.7f else 0f

        val timeSize = 56f * ppd * spec.scale.coerceIn(0.5f, 2f)
        val dateSize = 14f * ppd * spec.scale.coerceIn(0.5f, 2f)
        val clockH = if (spec.clock != null) timeSize * 1.25f + dateSize * 1.6f + 16f * ppd else 0f

        val maxLines =
                LockLayoutMath.maxLinesThatFit(h * 0.7f - 2 * padPx - clockH - quoteH, lineH, a.maxLines)
        val display = NoteStyles.decorateText(spec.decoration, spec.text, spec.stamp)
        val wrapped = LockLayoutMath.wrap(display, textW, maxLines) { paint.measureText(it) }
        val body = wrapped.lines.joinToString("\n")
        val layout =
                StaticLayout.Builder.obtain(body, 0, body.length, paint, Math.ceil(textW.toDouble()).toInt())
                        .setAlignment(
                                when (a.alignment) {
                                    NoteAlignment.START -> Layout.Alignment.ALIGN_NORMAL
                                    NoteAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
                                    NoteAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
                                }
                        )
                        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                        .setLineSpacing(0f, LockLayoutMath.spacingMultiplier(a.lineSpacing, boost))
                        .setIncludePad(false)
                        .build()

        val cardH = maxOf(layout.height + 2 * padPx + quoteH, 40f * ppd * look.heightScale)
        val groupH = clockH + cardH
        var y = LockLayoutMath.blockTop(spec.position, h, groupH)

        spec.clock?.let { c ->
            val cp =
                    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        typeface = spec.typeface
                        color = LockLayoutMath.defaultTextColor(bgLuma)
                        textAlign = Paint.Align.CENTER
                        textSize = timeSize
                    }
            canvas.drawText(c.time, w / 2f, y + timeSize, cp)
            cp.textSize = dateSize
            cp.alpha = 200
            canvas.drawText(c.date, w / 2f, y + timeSize * 1.25f + dateSize * 1.2f, cp)
            y += clockH
        }

        val left = (w - cardW) / 2f
        val rect = RectF(left, y, left + cardW, y + cardH)
        val radius = look.cornerRadiusDp * ppd
        if (resolved.backgroundArgb != 0) {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = resolved.backgroundArgb }
            if (resolved.elevated) fill.setShadowLayer(8f * ppd, 0f, 3f * ppd, 0x55000000)
            canvas.drawRoundRect(rect, radius, radius, fill)
        }
        if (spec.decoration == NoteDecoration.RULED) {
            val lp = Paint().apply { color = (textColor and 0x00FFFFFF) or 0x2E000000; strokeWidth = 1.5f * ppd }
            canvas.save()
            canvas.clipRect(rect)
            var ly = rect.top + padPx + quoteH + lineH * 0.95f
            while (ly < rect.bottom) {
                canvas.drawLine(rect.left, ly, rect.right, ly, lp)
                ly += lineH
            }
            canvas.restore()
        }
        if (resolved.showBorder) {
            val bp =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 1.5f * ppd
                        color = if (look.borderColor != 0) look.borderColor else (textColor and 0x00FFFFFF) or 0x99000000.toInt()
                    }
            canvas.drawRoundRect(rect, radius, radius, bp)
        }
        var textTop = rect.top + padPx
        if (spec.decoration == NoteDecoration.QUOTE_MARK) {
            val qp =
                    TextPaint(paint).apply {
                        textSize = sizePx * 3f
                        alpha = 115
                        clearShadowLayer()
                    }
            val qx =
                    when (LockLayoutMath.physicalAlign(a.alignment, LockLayoutMath.isRtlParagraph(spec.text))) {
                        0 -> rect.centerX() - qp.measureText("“") / 2f
                        1 -> rect.right - padPx - qp.measureText("“")
                        else -> rect.left + padPx
                    }
            canvas.drawText("“", qx, textTop + sizePx * 1.9f, qp)
            textTop += quoteH
        }
        canvas.save()
        canvas.translate(left + padPx, textTop)
        layout.draw(canvas)
        canvas.restore()
        return bmp
    }

    /** Paints the backdrop and returns its approximate relative luminance. */
    private fun drawBackdrop(canvas: Canvas, b: LockBackdrop, w: Int, h: Int): Float {
        when (b) {
            is LockBackdrop.Solid -> {
                canvas.drawColor(b.argb or 0xFF000000.toInt())
                return LockLayoutMath.luma(b.argb)
            }
            is LockBackdrop.Gradient -> {
                val p = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), b.top, b.bottom, Shader.TileMode.CLAMP) }
                canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
                return (LockLayoutMath.luma(b.top) + LockLayoutMath.luma(b.bottom)) / 2f
            }
            is LockBackdrop.Photo -> {
                val src = b.bitmap
                val c = com.lu4p.fokuslauncher.wallpaper.WallpaperMath.centerCrop(src.width, src.height, w, h)
                canvas.drawBitmap(
                        src,
                        android.graphics.Rect(c.left, c.top, c.right, c.bottom),
                        RectF(0f, 0f, w.toFloat(), h.toFloat()),
                        Paint(Paint.FILTER_BITMAP_FLAG),
                )
                canvas.drawColor(Color.argb(70, 0, 0, 0))
                return 0.08f
            }
        }
    }
}

/** Turns stored font values into android.graphics.Typeface (bundled, system, imported), never throwing. */
@Singleton
class LockTypefaces
@Inject
constructor(
        @param:ApplicationContext private val context: Context,
        private val customFonts: CustomFontStore,
) {
    fun resolve(storage: String?, weight: Int): Typeface {
        val w = weight.coerceIn(100, 900)
        val base: Typeface? =
                runCatching {
                            when (val c = FontRegistry.resolve(storage)) {
                                FontChoice.FollowLauncher -> null
                                is FontChoice.System -> Typeface.create(c.familyName, Typeface.NORMAL)
                                is FontChoice.Custom ->
                                        customFonts.resolveFile(c.storageValue)?.takeIf { it.isFile }
                                                ?.let { Typeface.createFromFile(it) }
                                is FontChoice.Bundled ->
                                        Typeface.Builder(context.assets, c.font.assetPath)
                                                .setFontVariationSettings("'wght' $w")
                                                .build()
                            }
                        }
                        .getOrNull()
        val t = base ?: Typeface.DEFAULT
        val bundled = FontRegistry.resolve(storage) is FontChoice.Bundled
        return if (!bundled && android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(t, w, false) else t
    }
}
