/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.draabek.fractal.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.util.Log
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.min

/**
 * A two-dimensional square for use as a drawn object in OpenGL ES 2.0.
 */
class Square {
    private val extraBufferId = IntArray(1)
    private val vertexBuffer: FloatBuffer
    private val drawListBuffer: ShortBuffer
    private var mProgram = 0
    private var currentFractal: GLSLFractal? = null
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     */
    init {

        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect( // (# of coordinate values * 4 bytes per float)
            squareCoords.size * 4
        )
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(squareCoords)
        vertexBuffer.position(0)

        // initialize byte buffer for the draw list
        val dlb = ByteBuffer.allocateDirect( // (# of coordinate values * 2 bytes per short)
            drawOrder.size * 2
        )
        dlb.order(ByteOrder.nativeOrder())
        drawListBuffer = dlb.asShortBuffer()
        drawListBuffer.put(drawOrder)
        drawListBuffer.position(0)
        updateCurrentFractal()
    }

    fun updateCurrentFractal() {
        val f = instance.current
        check(f is GLSLFractal) { "Current fractal not instance of " + GLSLFractal::class.java.name }
        currentFractal = f
        updateShaders()
    }

    private fun updateShaders() {
        // prepare shaders and OpenGL program
        val vertexShader = ShaderUtils.loadShader(
            GLES20.GL_VERTEX_SHADER,
            currentFractal!!.shaders!![0]
        )
        val fragmentShader = ShaderUtils.loadShader(
            GLES20.GL_FRAGMENT_SHADER,
            currentFractal!!.shaders!![1]
        )
        mProgram = GLES20.glCreateProgram() // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader) // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
        GLES20.glLinkProgram(mProgram) // create OpenGL program executables
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(mProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(this.javaClass.name, "Could not link program")
            val infoLog = GLES20.glGetShaderInfoLog(mProgram)
            GLES20.glDeleteProgram(mProgram)
            GLES20.glFlush()
            mProgram = 0
            val msg = String.format(
                "Failed to compile shader for %s\n%s",
                instance.current!!.name, infoLog
            )
            Log.e(LOG_KEY, msg)
            //this sequence is strange, hopefully there will not be infinite loop
            instance.current = instance["Mandelbrot"]
        }
        if (instance.current!!.parameters["glBuffer"] != null) {
            GLES20.glGenFramebuffers(1, extraBufferId, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, extraBufferId[0])
        }
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     */
    fun draw(width: Int, height: Int) {
        if (currentFractal != instance.current) {
            updateCurrentFractal()
        }
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        val mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")

        // Enable a handle to the square vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle)

        // Prepare the square coordinate data
        GLES20.glVertexAttribPointer(
            mPositionHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, vertexBuffer
        )

        // Get rendering parameters and apply as uniforms
        val settings: Map<String, Float> = currentFractal!!.parameters
        ShaderUtils.applyFloatUniforms(settings, mProgram)
        ShaderUtils.applyResolutionUniform(min(width, height), min(width, height), mProgram)
        val paletteHandle = GLES20.glGetUniformLocation(mProgram, "palette")
        if (paletteHandle != -1) {
            val colors = currentFractal!!.colorPalette!!.colorsInt
            val bitmap = Bitmap.createBitmap(colors!!, colors.size, 1, Bitmap.Config.ARGB_8888)
            val handle = ShaderUtils.loadTexture(bitmap)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle)
            GLES20.glUniform1i(paletteHandle, 0)
        } // Draw the square
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES, drawOrder.size,
            GLES20.GL_UNSIGNED_SHORT, drawListBuffer
        )

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle)
    }

    companion object {
        val LOG_KEY = Square::class.java.name

        // number of coordinates per vertex in this array
        private const val COORDS_PER_VERTEX = 3
        private val squareCoords = floatArrayOf(
            -1.0f, 1.0f, 0.0f,  // top right
            -1.0f, -1.0f, 0.0f,  // bottom right
            1.0f, -1.0f, 0.0f,  // bottom left
            1.0f, 1.0f, 0.0f
        ) // top left
        private const val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    }
}