package com.lu4p.fokuslauncher.backdrop

/**
 * Pure, Android-free description of a procedural backdrop: an ordered list of draw operations in
 * pixel coordinates of a [width] x [height] canvas. Keeping it as data makes generation
 * deterministic and unit-testable on the plain JVM; [BackdropRenderer] turns it into pixels.
 * Colours are 0xRRGGBB ints (alpha is carried per operation).
 */
class BackdropScene(val width: Int, val height: Int, val ops: List<BackdropOp>) {
    override fun equals(other: Any?): Boolean =
            other is BackdropScene && width == other.width && height == other.height && ops == other.ops

    override fun hashCode(): Int = (width * 31 + height) * 31 + ops.hashCode()
}

sealed interface BackdropOp {
    /** Vertical gradient over the whole canvas. [stops] are 0..1 and match [colors]. */
    data class VGradient(val colors: List<Int>, val stops: List<Float>) : BackdropOp

    /** Soft radial blob fading to transparent at [r]. */
    data class Blob(val cx: Float, val cy: Float, val r: Float, val color: Int, val alpha: Float) : BackdropOp

    /** Filled silhouette: [points] (x0,y0,x1,y1,...) closed down to the bottom edge. */
    data class Silhouette(val points: List<Float>, val color: Int, val alpha: Float) : BackdropOp

    data class Rect(val l: Float, val t: Float, val r: Float, val b: Float, val color: Int, val alpha: Float) : BackdropOp

    data class Dot(val x: Float, val y: Float, val r: Float, val color: Int, val alpha: Float) : BackdropOp

    /** Smooth stroked ribbon through [points] (x,y pairs). */
    data class Ribbon(val points: List<Float>, val width: Float, val color: Int, val alpha: Float) : BackdropOp

    /** Vertical light curtain hanging from [points] (x,y pairs), fading out over [length]. */
    data class Curtain(val points: List<Float>, val length: Float, val color: Int, val alpha: Float) : BackdropOp
}

/** Catalogue entry; [dark] says whether light text belongs on it (themes must match). */
data class BackdropInfo(val id: String, val dark: Boolean, val swatch: List<Int>)
