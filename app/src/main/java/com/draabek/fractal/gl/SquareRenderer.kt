package com.draabek.fractal.gl

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Simple Renderer for PixelBuffer
 */
class SquareRenderer : GLSurfaceView.Renderer {
    private var square: Square? = null
    private var width = 0
    private var height = 0
    var isRenderInProgress = false
        private set

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        square = Square()
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    override fun onDrawFrame(gl: GL10) {
        isRenderInProgress = true
        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        square!!.draw(width, height)
        isRenderInProgress = false
    }
}