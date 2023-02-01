package com.draabek.fractal.canvas

/**
 * Native rendering code. Superseded by GLSL where possible
 */
object NativeLib {
    init {
        System.loadLibrary("fractalzoo-jni")
    }

    @JvmStatic
    external fun redrawMandelbrot(
        array: IntArray?,
        width: Int, height: Int,
        left: Double, top: Double, right: Double, bottom: Double,
        maxiter: Int
    )

    @JvmStatic
    external fun redrawMandelbrotPart(
        array: IntArray?,
        width: Int, height: Int,
        left: Double, top: Double, right: Double, bottom: Double,
        maxiter: Int,
        x: Int, y: Int, x2: Int, y2: Int
    )

    @JvmStatic
    external fun redrawLorenz(
        array: IntArray?,
        width: Int, height: Int,
        left: Double, top: Double, right: Double, bottom: Double,
        maxiter: Int
    )

    @JvmStatic
    external fun redrawLorenzPart(
        array: IntArray?,
        width: Int, height: Int,
        left: Double, top: Double, right: Double, bottom: Double,
        maxiter: Int,
        x: Int, y: Int, x2: Int, y2: Int
    )
}
