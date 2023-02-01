package com.draabek.fractal.canvas

import android.graphics.Canvas

abstract class CanvasFractal : CpuFractal() {
    abstract fun draw(canvas: Canvas?)
}