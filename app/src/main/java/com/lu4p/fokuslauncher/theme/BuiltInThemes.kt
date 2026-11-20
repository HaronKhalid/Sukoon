package com.lu4p.fokuslauncher.theme

object BuiltInThemes {
    const val ID_AMOLED_BLACK = "amoled-black"
    const val ID_CUSTOM = "custom"

    private fun theme(
            id: String,
            name: String,
            appearance: ThemeAppearance,
            background: Int,
            primaryText: Int,
            secondaryText: Int,
            accent: Int,
            surface: Int,
            clockFont: String = ThemeTypography.FONT_SYSTEM,
    ) =
            ThemeDefinition(
                    id = id,
                    name = name,
                    appearance = appearance,
                    colors = ThemeColors(background, primaryText, secondaryText, accent, surface),
                    typography = ThemeTypography(clockFont = clockFont),
            )

    val AmoledBlack =
            theme(ID_AMOLED_BLACK, "AMOLED Black", ThemeAppearance.DARK,
                    0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFB0B0B0.toInt(), 0xFFFFFFFF.toInt(), 0xFF1A1A1A.toInt())
    val MidnightNavy =
            theme("midnight-navy", "Midnight Navy", ThemeAppearance.DARK,
                    0xFF080B12.toInt(), 0xFFF5F5F5.toInt(), 0xFF929BAD.toInt(), 0xFFA6B6D4.toInt(), 0xFF141A28.toInt(),
                    clockFont = "inter")
    val Cream =
            theme("cream", "Cream", ThemeAppearance.LIGHT,
                    0xFFF6F0E1.toInt(), 0xFF2B2620.toInt(), 0xFF6F665A.toInt(), 0xFF9A6B3F.toInt(), 0xFFEBE2CE.toInt())
    val Monochrome =
            theme("monochrome", "Monochrome", ThemeAppearance.DARK,
                    0xFF111111.toInt(), 0xFFEDEDED.toInt(), 0xFF8C8C8C.toInt(), 0xFFCFCFCF.toInt(), 0xFF1E1E1E.toInt())
    val Nature =
            theme("nature", "Nature", ThemeAppearance.DARK,
                    0xFF0D140F.toInt(), 0xFFEAF2E6.toInt(), 0xFF8FA58F.toInt(), 0xFF8FBC8F.toInt(), 0xFF18241B.toInt())
    val Ocean =
            theme("ocean", "Ocean", ThemeAppearance.DARK,
                    0xFF06141C.toInt(), 0xFFE6F3F8.toInt(), 0xFF7FA3B3.toInt(), 0xFF4FB3D9.toInt(), 0xFF0F2430.toInt())
    val SoftPastel =
            theme("soft-pastel", "Soft Pastel", ThemeAppearance.LIGHT,
                    0xFFF7F1F5.toInt(), 0xFF34303A.toInt(), 0xFF75707C.toInt(), 0xFFB48EAD.toInt(), 0xFFEBE1EA.toInt())
    /** Starting template for user-created themes; editing it produces a new saved custom theme. */
    val Custom =
            theme(ID_CUSTOM, "Custom", ThemeAppearance.DARK,
                    0xFF101114.toInt(), 0xFFF2F2F2.toInt(), 0xFF9A9AA2.toInt(), 0xFF7FB2FF.toInt(), 0xFF1B1C21.toInt())

    val all: List<ThemeDefinition> =
            listOf(AmoledBlack, MidnightNavy, Cream, Monochrome, Nature, Ocean, SoftPastel, Custom)

    val default: ThemeDefinition = AmoledBlack

    fun byId(id: String): ThemeDefinition? = all.firstOrNull { it.id == id }

    fun isBuiltInId(id: String): Boolean = all.any { it.id == id }
}
