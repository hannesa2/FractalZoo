package com.draabek.fractal.fractal

import com.draabek.fractal.palette.ColorPalette

abstract class Fractal() {
    var name = ""
    var thumbPath: String? = null
    var parameters: MutableMap<String, Float> = LinkedHashMap()
    var colorPalette: ColorPalette? = null

    constructor(name: String) : this() {
        this.name = name
    }

    abstract val viewWrapper: Class<out FractalViewWrapper?>?
    fun updateSettings(newSettings: Map<String, Float>?) {
        parameters.putAll(newSettings!!)
    }

    override fun toString(): String {
        return name
    }
}
