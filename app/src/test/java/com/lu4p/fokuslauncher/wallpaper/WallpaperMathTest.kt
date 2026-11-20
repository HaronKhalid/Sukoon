package com.lu4p.fokuslauncher.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WallpaperMathTest {
    @Test
    fun wideSourceCropsSidesCentred() {
        val r = WallpaperMath.centerCrop(4000, 2000, 1000, 2000)
        assertEquals(CropRect(1500, 0, 2500, 2000), r)
    }

    @Test
    fun tallSourceCropsTopBottomCentred() {
        val r = WallpaperMath.centerCrop(1000, 4000, 1000, 2000)
        assertEquals(CropRect(0, 1000, 1000, 3000), r)
    }

    @Test
    fun sameAspectIsUnchanged() {
        assertEquals(CropRect(0, 0, 1080, 2400), WallpaperMath.centerCrop(1080, 2400, 540, 1200))
    }

    @Test
    fun sampleSizeKeepsAtLeastMaxEdge() {
        assertEquals(1, WallpaperMath.sampleSize(1500, 1000, 2048))
        assertEquals(2, WallpaperMath.sampleSize(4096, 3000, 2048))
        assertEquals(4, WallpaperMath.sampleSize(8192, 6000, 2048))
    }

    @Test
    fun averageIgnoresTransparent() {
        val px = intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt(), 0x00123456)
        assertEquals(0xFF7F007F.toInt(), WallpaperMath.averageColor(px))
        assertNull(WallpaperMath.averageColor(intArrayOf(0)))
    }

    @Test
    fun dominantPicksMostCommonBucket() {
        val dark = 0xFF101010.toInt()
        val px = IntArray(10) { if (it < 7) dark else 0xFFFFFFFF.toInt() }
        assertEquals(0xFF202020.toInt(), WallpaperMath.dominantColor(px))
        assertNull(WallpaperMath.dominantColor(IntArray(0)))
    }

    @Test
    fun recentsAreNewestFirstDedupedAndCapped() {
        val (list, evicted) = WallpaperMath.pushRecent(listOf("a", "b", "c"), "d", 3)
        assertEquals(listOf("d", "a", "b"), list)
        assertEquals(listOf("c"), evicted)
        val (again, none) = WallpaperMath.pushRecent(listOf("a", "b"), "b", 3)
        assertEquals(listOf("b", "a"), again)
        assertEquals(emptyList<String>(), none)
    }
}
