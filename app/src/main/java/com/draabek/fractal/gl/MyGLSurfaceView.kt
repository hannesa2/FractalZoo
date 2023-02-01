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

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Toast
import com.draabek.fractal.R
import com.draabek.fractal.activity.SaveBitmapActivity
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import com.draabek.fractal.fractal.FractalViewWrapper
import com.draabek.fractal.fractal.RenderListener
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.min

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user
 * interacting with drawn objects.
 */
class MyGLSurfaceView : GLSurfaceView, FractalViewWrapper {

    private lateinit var renderer: MyGLRenderer

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    fun init() {

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 8, 0)
        // Set the Renderer for drawing on the GLSurfaceView
        renderer = MyGLRenderer()
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data
        renderMode = RENDERMODE_WHEN_DIRTY
        //this.invalidate();
    }

    private var mPreviousX = 0f
    private var mPreviousY = 0f
    private var mPreviousX2 = 0f
    private var mPreviousY2 = 0f
    override fun setRenderListener(renderListener: RenderListener?) {
        throw UnsupportedOperationException()
    }

    override fun clear() {}
    override fun onTouchEvent(e: MotionEvent): Boolean {
        val TOUCH_SCALE_FACTOR = 1.5f / min(width, height)
        val x = e.x
        val y = e.y
        var x2 = 0f
        var y2 = 0f
        if (e.pointerCount > 1) {
            x2 = e.getX(1)
            y2 = e.getY(1)
        }
        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                Timber.d("GL MOVE")
                if (e.pointerCount == 1) {
                    val dx = x - mPreviousX
                    val dy = y - mPreviousY
                    val fractalX = instance.current!!.parameters["centerX"]
                    val fractalY = instance.current!!.parameters["centerY"]
                    if (fractalX == null && fractalY == null) {
                        Timber.i("Fractal has no movable center")
                    } else {
                        if (fractalX != null) {
                            instance.current!!
                                .parameters["centerX"] = fractalX + dx * TOUCH_SCALE_FACTOR
                            Timber.v("X shift: " + dx * TOUCH_SCALE_FACTOR)
                        }
                        if (fractalY != null) {
                            //- instead of + because OpenGL has y axis upside down
                            instance.current!!
                                .parameters["centerY"] = fractalY - dy * TOUCH_SCALE_FACTOR
                            Timber.v("Y shift: " + dy * TOUCH_SCALE_FACTOR)
                        }
                    }
                } else if (e.pointerCount == 2 && (mPreviousY2 > 0 || mPreviousX2 > 0)) {
                    val scale = instance.current!!.parameters["scale"]
                    if (scale == null) {
                        Timber.i("Fractal is not scaleable")
                    } else {
                        // Probably abs() is sufficient, but this is better for clarity
                        val oldDist = Math.sqrt(
                            ((mPreviousX - mPreviousX2) * (mPreviousX - mPreviousX2) +
                                    (mPreviousY - mPreviousY2) * (mPreviousY - mPreviousY2)).toDouble()
                        ).toFloat()
                        val newDist = Math.sqrt(((x - x2) * (x - x2) + (y - y2) * (y - y2)).toDouble()).toFloat()
                        if (oldDist > 0) {
                            instance.current!!.parameters["scale"] = scale * newDist / oldDist
                            Timber.v("Scale: " + scale * newDist / oldDist)
                        }
                    }
                }
                requestRender()
            }
            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> requestRender()
        }
        mPreviousX = x
        mPreviousY = y
        mPreviousX2 = x2
        mPreviousY2 = y2
        return true
    }

    private fun captureBitmapCallback(bitmap: Bitmap) {
        try {
            val tmpFile = File.createTempFile("bitmap", "jpg", context.cacheDir)
            bitmap.compress(
                Bitmap.CompressFormat.JPEG, 100,
                FileOutputStream(tmpFile)
            )
            val intent = Intent(this.context, SaveBitmapActivity::class.java)
            intent.action = Intent.ACTION_SEND
            intent.putExtra(context.getString(R.string.intent_extra_bitmap_file), tmpFile.absolutePath)
            context.startActivity(intent)
        } catch (e: IOException) {
            Toast.makeText(this.context, "Could not save current image", Toast.LENGTH_SHORT).show()
            Timber.e(e)
        }
    }

    override fun saveBitmap() {
        renderer.captureSurface()
        requestRender()
    }

    override val isRendering: Boolean
        get() = TODO("Not yet implemented")

    private inner class MyGLRenderer : Renderer {
        private var mSquare: Square? = null
        private var width = 0
        private var height = 0
        private var capturing = false
        var isRendering = false

        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {

            // Set the background frame color
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
            mSquare = Square()
            capturing = false
        }

        fun captureSurface() {
            capturing = true
        }

        private fun saveCurrentSurface(width: Int, height: Int): Bitmap {
            val bb = ByteBuffer.allocate(width * height * 4)
            GLES20.glReadPixels(0, 0, width, height, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, bb)
            val orig = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            orig.copyPixelsFromBuffer(bb)
            val matrix = Matrix()
            matrix.postScale(1f, -1f, width / 2.0f, height / 2.0f)
            return Bitmap.createBitmap(orig, 0, 0, width, height, matrix, true)
        }

        override fun onDrawFrame(unused: GL10) {
            this.isRendering = true
            // Draw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // Draw square
            mSquare!!.draw(width, height)
            //MyGLSurfaceView.this.post(() -> MyGLSurfaceView.this.setVisibility(VISIBLE));
            if (capturing) {
                captureBitmapCallback(saveCurrentSurface(width, height))
                capturing = false
            }
            this.isRendering = false
        }

        override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
            // Adjust the viewport based on geometry changes,
            // such as screen rotation
            GLES20.glViewport(0, 0, width, height)
            this.width = width
            this.height = height
        }
    }
}