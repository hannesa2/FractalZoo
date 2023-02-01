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
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import com.draabek.fractal.fractal.FractalViewWrapper
import com.draabek.fractal.fractal.RenderListener
import timber.log.Timber
import kotlin.math.min

/**
 * A view container where OpenGL ES graphics can be drawn on screen.
 * This view can also be used to capture touch events, such as a user interacting with drawn objects.
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
        renderer = MyGLRenderer(context)
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

    override fun saveBitmap() {
        renderer.captureSurface()
        requestRender()
    }

    override val isRendering = renderer.isRendering

}