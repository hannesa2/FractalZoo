package com.draabek.fractal.canvas

import com.draabek.fractal.fractal.Fractal
import com.draabek.fractal.fractal.FractalViewWrapper

open class CpuFractal : Fractal() {
    override val viewWrapper: Class<out FractalViewWrapper?>
        get() = FractalCpuView::class.java
}
