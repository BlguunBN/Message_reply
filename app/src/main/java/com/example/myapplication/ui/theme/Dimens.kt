package com.example.myapplication.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Single source of truth for spacing.
 * Keep values on a 4dp grid for consistent rhythm.
 */
object Dimens {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    // Common layout
    val screenPadding = lg
    val sectionSpacing = lg
    val listItemSpacing = md

    // Components
    val cardPadding = lg
    val avatarSize = 40.dp
    val chipHPadding = 10.dp
    val chipVPadding = 6.dp
    val inlineGap = md
    val textTightGap = xs
    val textNormalGap = sm
}
