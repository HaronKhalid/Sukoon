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
            gradient: ThemeGradient? = null,
            style: ThemeStyle = ThemeStyle(),
    ) =
            ThemeDefinition(
                    id = id,
                    name = name,
                    appearance = appearance,
                    colors = ThemeColors(background, primaryText, secondaryText, accent, surface),
                    typography = ThemeTypography(clockFont = clockFont),
                    gradient = gradient,
                    style = style,
            )

    private fun hex(v: String): Int = ThemeColorMath.parseHex(v)!!

    /** Gradient theme: the solid [ThemeColors.background] fallback is the middle stop. */
    private fun gradientTheme(
            id: String,
            name: String,
            appearance: ThemeAppearance,
            stops: List<String>,
            angle: Int,
            primaryText: String,
            secondaryText: String,
            accent: String,
            surface: String,
            style: ThemeStyle,
    ): ThemeDefinition {
        val argb = stops.map(::hex)
        return theme(id, name, appearance, argb[argb.size / 2], hex(primaryText), hex(secondaryText),
                hex(accent), hex(surface), gradient = ThemeGradient(argb, angle), style = style)
    }

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

    // ---- Sukoon originals (v2): gradients and style bundles ------------------------------------

    val Aurora = gradientTheme("aurora", "Aurora", ThemeAppearance.DARK,
            listOf("#0B1026", "#123B4A", "#2A1B4D"), 160, "#F2F7FA", "#A9BCCB", "#6EE7C8", "#16213A",
            ThemeStyle("stacked", "AESTHETIC", 300, 0.02f))
    val SunsetDunes = gradientTheme("sunset-dunes", "Sunset Dunes", ThemeAppearance.DARK,
            listOf("#1F1030", "#5A1F4A", "#8E3A3A"), 170, "#FFF1E6", "#F0C4B0", "#FFB067", "#3A1A3A",
            ThemeStyle("split-accent", "AESTHETIC", 400, 0.01f))
    val RoseQuartz = gradientTheme("rose-quartz", "Rose Quartz", ThemeAppearance.LIGHT,
            listOf("#FBE9EE", "#F6D5DF"), 180, "#3A1F2A", "#7A5563", "#B63D66", "#F3D3DC",
            ThemeStyle("minimal-text", "AESTHETIC", 400, 0.01f))
    val ForestMist = gradientTheme("forest-mist", "Forest Mist", ThemeAppearance.DARK,
            listOf("#0F1F1A", "#1E3A2F", "#2F4F45"), 150, "#EEF5F0", "#A8C0B3", "#9BD3A8", "#19302A",
            ThemeStyle("analog-minimal", "PURE_MINIMAL", 300, 0.02f))
    val DesertSand = gradientTheme("desert-sand", "Desert Sand", ThemeAppearance.LIGHT,
            listOf("#F3E7D3", "#E8D2AE"), 160, "#3B2A17", "#6B5636", "#A8561F", "#E9D8B9",
            ThemeStyle("classic-digital", "DEFAULT", 400, 0.0f))
    val LavenderDusk = gradientTheme("lavender-dusk", "Lavender Dusk", ThemeAppearance.DARK,
            listOf("#1E1A3A", "#3B2F6B"), 135, "#F3F0FF", "#BDB4E0", "#C3A6FF", "#2A2450",
            ThemeStyle("stacked", "AESTHETIC", 300, 0.03f))
    val Graphite =
            theme("graphite", "Graphite", ThemeAppearance.DARK,
                    0xFF1C1D20.toInt(), 0xFFECECEE.toInt(), 0xFF9A9CA3.toInt(), 0xFF8FB4FF.toInt(), 0xFF2A2B30.toInt(),
                    style = ThemeStyle("minimal-text", "PURE_MINIMAL", 400, 0.0f))
    val Paper =
            theme("paper", "Paper", ThemeAppearance.LIGHT,
                    0xFFFAF8F3.toInt(), 0xFF1F1E1B.toInt(), 0xFF66625A.toInt(), 0xFF2F5D8A.toInt(), 0xFFEFEBE2.toInt(),
                    style = ThemeStyle("minimal-text", "PURE_MINIMAL", 400, 0.0f))
    val Mint = gradientTheme("mint", "Mint", ThemeAppearance.LIGHT,
            listOf("#E6F7EF", "#CFEFE0"), 180, "#14352A", "#44695A", "#17815D", "#D3EDE0",
            ThemeStyle("classic-digital", "DEFAULT", 500, 0.0f))
    val OceanDeep = gradientTheme("ocean-deep", "Ocean Deep", ThemeAppearance.DARK,
            listOf("#021B2B", "#04405E"), 180, "#E8F6FC", "#8FBBD0", "#3DD0F2", "#0A3047",
            ThemeStyle("analog-ticks", "DEFAULT", 400, 0.01f))
    val NeonNight = gradientTheme("neon-night", "Neon Night", ThemeAppearance.DARK,
            listOf("#0A0014", "#1B0033"), 135, "#F5EEFF", "#B79BE0", "#39FFB6", "#1F0A3A",
            ThemeStyle("outline-bold", "FOCUS", 600, 0.04f))
    val Sakura = gradientTheme("sakura", "Sakura", ThemeAppearance.LIGHT,
            listOf("#FFF3F6", "#FFE0EA", "#F9D0DE"), 160, "#43232F", "#7E5062", "#C13566", "#FBDDE6",
            ThemeStyle("word-clock", "AESTHETIC", 400, 0.02f))
    val SlateBlue = gradientTheme("slate-blue", "Slate Blue", ThemeAppearance.DARK,
            listOf("#1E2A3A", "#2C3E55"), 170, "#EEF3F8", "#A3B3C6", "#7FB0FF", "#263650",
            ThemeStyle("classic-digital", "INFORMATION", 500, 0.0f))
    val Coffee = gradientTheme("coffee", "Coffee", ThemeAppearance.DARK,
            listOf("#1B120D", "#3A261B"), 180, "#F6EBDD", "#C2A98F", "#E0A96D", "#2C1D14",
            ThemeStyle("split-accent", "DEFAULT", 500, 0.0f))

    // ---- Backdrop themes: each pairs a procedural backdrop with matching colours ----------------

    private fun backdropTheme(
            id: String, name: String, dark: Boolean, stops: List<String>, primary: String, secondary: String,
            accent: String, surface: String, backdrop: String, face: String, preset: String,
            weight: Int = 400, spacing: Float = 0f,
    ) = gradientTheme(id, name, if (dark) ThemeAppearance.DARK else ThemeAppearance.LIGHT, stops, 180,
            primary, secondary, accent, surface,
            ThemeStyle(face, preset, weight, spacing, backdrop = backdrop))

    val DuskBloom = backdropTheme("dusk-bloom", "Dusk Bloom", true, listOf("#140B2E", "#2B1250"), "#F6F0FF", "#BFB0E0",
            "#FF8FC0", "#2A1850", "blobs-dusk", "stacked", "AESTHETIC", 300, 0.02f)
    val PeachBloom = backdropTheme("peach-bloom", "Peach Bloom", false, listOf("#FFF1E8", "#FFDCCF"), "#3A1F1A", "#7A5048",
            "#C2410C", "#FFD9C8", "blobs-peach", "minimal-text", "AESTHETIC", 400, 0.01f)
    val DuneGlow = backdropTheme("dune-glow", "Dune Glow", true, listOf("#1F1030", "#3A1A38"), "#FFF1E6", "#F0C4B0",
            "#FFB067", "#3A1A3A", "dunes-sunset", "split-accent", "AESTHETIC", 400, 0.01f)
    val SandDune = backdropTheme("sand-dune", "Sand Dune", false, listOf("#F8E8CC", "#F3DDB5"), "#2E200F", "#5E4A2C",
            "#9A4A12", "#EBD3A6", "dunes-sand", "classic-digital", "DEFAULT", 400, 0f)
    val DuskRidge = backdropTheme("dusk-ridge", "Dusk Ridge", true, listOf("#1B1840", "#2A2158"), "#F4F0FF", "#B8B0DC",
            "#FFB38A", "#2A2458", "ridges-dusk", "analog-minimal", "AESTHETIC", 300, 0.02f)
    val MistRidge = backdropTheme("mist-ridge", "Mist Ridge", true, listOf("#0E2A2A", "#16383A"), "#EAF6F2", "#9FC4BB",
            "#8EE0C0", "#1B3A3A", "ridges-mist", "stacked", "PURE_MINIMAL", 300, 0.02f)
    val CityLights = backdropTheme("city-lights", "City Lights", true, listOf("#0B0B2A", "#1A1240"), "#F7F2FF", "#B5A8D8",
            "#FFD27A", "#1E1646", "city-glow", "outline-bold", "FOCUS", 500, 0.03f)
    val DawnCity = backdropTheme("dawn-city", "Dawn City", true, listOf("#121C3A", "#1C2850"), "#F2F6FF", "#A9B8DA",
            "#FFB98A", "#1E2A52", "city-dawn", "classic-digital", "INFORMATION", 400, 0.01f)
    val StarField = backdropTheme("star-field", "Star Field", true, listOf("#02030A", "#0B1030"), "#F2F5FF", "#A0AAD0",
            "#9FC0FF", "#101638", "starfield", "minimal-text", "PURE_MINIMAL", 300, 0.04f)
    val AuroraVeil = backdropTheme("aurora-veil", "Aurora Veil", true, listOf("#040B1A", "#0A2030"), "#EFFAF8", "#9CC4C0",
            "#5CF0B8", "#0C2430", "aurora-ribbons", "split-accent", "AESTHETIC", 300, 0.02f)
    val RoseWash = backdropTheme("rose-wash", "Rose Wash", false, listOf("#FBF1F0", "#F8E4E4"), "#3A1F2A", "#7A5563",
            "#B02A5A", "#F5D9DF", "wash-rose", "word-clock", "AESTHETIC", 400, 0.02f)
    val OceanWash = backdropTheme("ocean-wash", "Ocean Wash", false, listOf("#EEF6F8", "#DDEBF2"), "#10303C", "#44687A",
            "#0E6E94", "#D2E6EE", "wash-ocean", "analog-ticks", "DEFAULT", 400, 0f)
    val PaperFibre = backdropTheme("paper-fibre", "Paper Fibre", false, listOf("#F6F0E4", "#EFE6D4"), "#1F1E1B", "#5E5A50",
            "#2F5D8A", "#EADFC8", "paper-grain", "minimal-text", "PURE_MINIMAL", 400, 0f)
    val SlateGrain = backdropTheme("slate-grain", "Slate Grain", true, listOf("#1C2028", "#2A303C"), "#EEF1F6", "#A8B2C4",
            "#8FB4FF", "#2A303C", "grain-slate", "classic-digital", "INFORMATION", 400, 0f)

    val backdropThemes: List<ThemeDefinition> = listOf(DuskBloom, PeachBloom, DuneGlow, SandDune, DuskRidge,
            MistRidge, CityLights, DawnCity, StarField, AuroraVeil, RoseWash, OceanWash, PaperFibre, SlateGrain)

    /** Sukoon's own first-run look. */
    val SukoonDefault: ThemeDefinition = Aurora

    val all: List<ThemeDefinition> =
            listOf(AmoledBlack, MidnightNavy, Cream, Monochrome, Nature, Ocean, SoftPastel, Custom,
                    Aurora, SunsetDunes, RoseQuartz, ForestMist, DesertSand, LavenderDusk, Graphite,
                    Paper, Mint, OceanDeep, NeonNight, Sakura, SlateBlue, Coffee) + backdropThemes

    /** Selecting this id means "keep the legacy Fokus look" (no theme applied). */
    const val ID_CLASSIC = "classic"

    /** Preview-only stand-in for the classic look; never part of [all] and never applied as a theme. */
    val ClassicPreview: ThemeDefinition = AmoledBlack.copy(id = ID_CLASSIC, name = "Classic (Fokus-like)")

    /** Gallery order: originals first, then the original eight (Custom is a template, not a style). */
    val gallery: List<ThemeDefinition> =
            backdropThemes + listOf(Aurora, SunsetDunes, RoseQuartz, ForestMist, DesertSand, LavenderDusk, Graphite, Paper,
                    Mint, OceanDeep, NeonNight, Sakura, SlateBlue, Coffee,
                    AmoledBlack, MidnightNavy, Cream, Monochrome, Nature, Ocean, SoftPastel)

    val default: ThemeDefinition = AmoledBlack

    fun byId(id: String): ThemeDefinition? = all.firstOrNull { it.id == id }

    fun isBuiltInId(id: String): Boolean = all.any { it.id == id }
}
