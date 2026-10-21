package com.n2.oemlauncher.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp
import com.n2.oemlauncher.mobile.config.TypeTokens

/**
 * Phone type scale of the design language (section 5). The system font is used as is — no
 * font is bundled — and digits are tabular so counters do not jump while they animate.
 */
private val LineMetrics = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight,
    letterSpacingEm: Float = 0f,
): TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (size * letterSpacingEm).sp,
    fontFeatureSettings = "tnum",
    lineHeightStyle = LineMetrics,
)

val CompanionTypography = Typography(
    headlineMedium = style(TypeTokens.DISPLAY_SP, TypeTokens.DISPLAY_LINE, FontWeight.Medium),
    titleLarge = style(TypeTokens.TITLE_SP, TypeTokens.TITLE_LINE, FontWeight.Medium),
    titleMedium = style(TypeTokens.SUBTITLE_SP, TypeTokens.SUBTITLE_LINE, FontWeight.Medium),
    titleSmall = style(TypeTokens.BODY_SP, TypeTokens.LABEL_LINE, FontWeight.Medium),
    bodyLarge = style(TypeTokens.BODY_SP, TypeTokens.BODY_LINE, FontWeight.Normal),
    bodyMedium = style(TypeTokens.BODY_SP, TypeTokens.BODY_LINE, FontWeight.Normal),
    bodySmall = style(TypeTokens.CAPTION_SP, TypeTokens.CAPTION_LINE, FontWeight.Normal),
    labelLarge = style(TypeTokens.LABEL_SP, TypeTokens.LABEL_LINE, FontWeight.Medium),
    labelMedium = style(TypeTokens.CAPTION_SP, TypeTokens.CAPTION_LINE, FontWeight.Medium),
    labelSmall = style(
        TypeTokens.CAPTION_SP, TypeTokens.CAPTION_LINE, FontWeight.SemiBold,
        TypeTokens.GROUP_LETTER_SPACING_EM,
    ),
)
