package com.lu4p.fokuslauncher.backdrop

import com.lu4p.fokuslauncher.theme.BuiltInThemes
import com.lu4p.fokuslauncher.theme.ThemeAppearance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackdropsTest {
    @Test
    fun `at least twelve backdrops with unique ids`() {
        assertTrue(Backdrops.all.size >= 12)
        assertEquals(Backdrops.all.size, Backdrops.all.map { it.id }.toSet().size)
    }

    @Test
    fun `generation is deterministic for the same id and size`() {
        for (b in Backdrops.all) {
            assertEquals(b.id, Backdrops.generate(b.id, 360, 760), Backdrops.generate(b.id, 360, 760))
        }
    }

    @Test
    fun `different ids and different sizes give different scenes`() {
        assertNotEquals(Backdrops.generate("dunes-sand", 360, 760), Backdrops.generate("dunes-sunset", 360, 760))
        assertNotEquals(Backdrops.generate("starfield", 360, 760), Backdrops.generate("starfield", 400, 760))
    }

    @Test
    fun `unknown and empty ids produce nothing`() {
        assertNull(Backdrops.generate("", 100, 100))
        assertNull(Backdrops.generate("nope", 100, 100))
        assertNull(Backdrops.generate(null, 100, 100))
    }

    @Test
    fun `size is clamped and geometry stays inside the canvas margin`() {
        for (b in Backdrops.all) {
            for ((rw, rh) in listOf(1 to 1, 360 to 760, 1080 to 2400, 99999 to 99999)) {
                val s = Backdrops.generate(b.id, rw, rh)!!
                assertTrue(s.width in Backdrops.MIN_SIZE..Backdrops.MAX_SIZE)
                assertTrue(s.height in Backdrops.MIN_SIZE..Backdrops.MAX_SIZE)
                val mx = s.width * 0.15f
                val my = s.height * 0.15f
                fun okXY(l: List<Float>) {
                    for (i in l.indices step 2) {
                        assertTrue("${b.id} x ${l[i]}", l[i] in -mx..s.width + mx)
                        assertTrue("${b.id} y ${l[i + 1]}", l[i + 1] in -my..s.height + my)
                    }
                }
                for (op in s.ops) when (op) {
                    is BackdropOp.Silhouette -> okXY(op.points)
                    is BackdropOp.Ribbon -> okXY(op.points)
                    is BackdropOp.Curtain -> okXY(op.points)
                    is BackdropOp.Dot -> okXY(listOf(op.x, op.y))
                    is BackdropOp.Blob -> assertTrue(op.alpha in 0f..1f && op.r > 0f)
                    is BackdropOp.Rect -> assertTrue(op.alpha in 0f..1f && op.l <= op.r && op.t <= op.b)
                    is BackdropOp.VGradient -> assertEquals(op.colors.size, op.stops.size)
                }
                assertTrue("op count bounded", s.ops.size < 4000)
            }
        }
    }

    @Test
    fun `every backdrop has a built-in theme whose appearance matches it`() {
        for (b in Backdrops.all) {
            val t = BuiltInThemes.backdropThemes.firstOrNull { it.style.backdrop == b.id }
            assertNotNull("theme for ${b.id}", t)
            assertEquals(b.id, if (b.dark) ThemeAppearance.DARK else ThemeAppearance.LIGHT, t!!.appearance)
        }
    }

    @Test
    fun `gallery has twenty plus themes and original ids are kept`() {
        assertTrue(BuiltInThemes.gallery.size >= 20)
        for (id in listOf("amoled-black", "midnight-navy", "cream", "aurora", "coffee")) assertNotNull(BuiltInThemes.byId(id))
        assertEquals(BuiltInThemes.all.size, BuiltInThemes.all.map { it.id }.toSet().size)
    }
}
