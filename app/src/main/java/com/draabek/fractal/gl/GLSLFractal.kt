package com.draabek.fractal.gl

import com.draabek.fractal.fractal.Fractal
import com.draabek.fractal.fractal.FractalViewWrapper

class GLSLFractal : Fractal() {
    /**
     * A fractal subclass rendered using the GLSL language
     * directly on the graphics card
     */
    var shaders: Array<String>? = null

    override val viewWrapper: Class<out FractalViewWrapper>
        get() = RenderImageView::class.java

}
