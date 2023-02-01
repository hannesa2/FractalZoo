package com.draabek.fractal.palette

/**
 * A color palette (sequence of colors from 0 to getSize())
 */
abstract class ColorPalette {
    abstract fun getColorInt(intensity: Float): Int
    abstract val colorsInt: IntArray?
}
