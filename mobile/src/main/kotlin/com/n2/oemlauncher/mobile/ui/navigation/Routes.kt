package com.n2.oemlauncher.mobile.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object Home
@Serializable data object Editor
@Serializable data object Appearance
@Serializable data class IconsPage(val appKey: String? = null)
@Serializable data object Preview
@Serializable data class Crop(val appKey: String)
@Serializable data object ImportResult
@Serializable data object Help
