package com.stillpoint.app.ui

import android.content.Context
import android.graphics.Typeface

/** Archivo is one variable font; these are the cuts of it the app uses. */
object Fonts {
    private const val PATH = "fonts/archivo.ttf"

    private fun cut(context: Context, weight: Int, width: Int): Typeface =
        Typeface.Builder(context.assets, PATH)
            .setFontVariationSettings("'wght' $weight, 'wdth' $width")
            .build()

    fun display(context: Context) = cut(context, weight = 800, width = 75)
    fun title(context: Context) = cut(context, weight = 700, width = 90)
    fun text(context: Context) = cut(context, weight = 400, width = 100)
    fun label(context: Context) = cut(context, weight = 600, width = 100)
}
