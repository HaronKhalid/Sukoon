package com.lu4p.fokuslauncher.home.layout

/**
 * Top padding in dp for a home element. [base] is the padding at [ElementSpacing.NORMAL], so the
 * default configuration keeps the original spacing; other spacings shift it. Nothing above means no padding.
 */
fun layoutPadDp(base: Int, spacing: ElementSpacing, hasAbove: Boolean): Int =
        if (!hasAbove) 0 else (base + spacing.dp - ElementSpacing.NORMAL.dp).coerceAtLeast(0)
