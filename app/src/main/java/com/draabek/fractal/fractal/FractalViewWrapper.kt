package com.draabek.fractal.fractal

interface FractalViewWrapper {
    fun saveBitmap()
    fun setVisibility(visibility: Int)
    val isRendering: Boolean
    fun setRenderListener(renderListener: RenderListener?)
    fun clear()
}
