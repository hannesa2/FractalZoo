package com.draabek.fractal.fractal

interface RenderListener {
    fun onRenderRequested()
    fun onRenderComplete(millis: Long)
}
