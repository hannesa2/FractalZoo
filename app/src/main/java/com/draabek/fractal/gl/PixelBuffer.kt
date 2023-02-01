package com.draabek.fractal.gl

import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.IntBuffer
import javax.microedition.khronos.egl.*
import javax.microedition.khronos.opengles.GL10

class PixelBuffer(private var mWidth: Int, private var mHeight: Int) {
    // borrow this interface
    private var mRenderer: GLSurfaceView.Renderer? = null
    private var mBitmap: Bitmap? = null
    private var mEGL: EGL10
    private var mEGLDisplay: EGLDisplay

    private var mEGLConfigs: Array<EGLConfig?>
    private var mEGLConfig: EGLConfig?
    private var mEGLContext: EGLContext
    private var mEGLSurface: EGLSurface
    private var mGL: GL10
    private var mThreadOwner: String

    init {
        val numConfig = IntArray(1)
        mEGLConfigs = arrayOfNulls(numConfig[0])

        val version = IntArray(2)
        val attribList = intArrayOf(
            EGL10.EGL_WIDTH, mWidth,
            EGL10.EGL_HEIGHT, mHeight,
            EGL10.EGL_NONE
        )
        val contextAttribs = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
        )
        // No error checking performed, minimum required code to elucidate logic
        mEGL = EGLContext.getEGL() as EGL10
        mEGLDisplay = mEGL.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        mEGL.eglInitialize(mEGLDisplay, version)
        mEGLConfig = chooseConfig() // Choosing a config is a little more complicated
        mEGLContext = mEGL.eglCreateContext(mEGLDisplay, mEGLConfig, EGL10.EGL_NO_CONTEXT, contextAttribs)
        mEGLSurface = mEGL.eglCreatePbufferSurface(mEGLDisplay, mEGLConfig, attribList)
        mEGL.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)
        mGL = mEGLContext.gl as GL10

        // Record thread owner of OpenGL context
        mThreadOwner = Thread.currentThread().name
    }

    fun setRenderer(renderer: GLSurfaceView.Renderer?) {
        mRenderer = renderer

        // Does this thread own the OpenGL context?
        if (Thread.currentThread().name != mThreadOwner) {
            Log.e(TAG, "setRenderer: This thread does not own the OpenGL context.")
            return
        }

        // Call the renderer initialization routines
        mRenderer!!.onSurfaceCreated(mGL, mEGLConfig)
        mRenderer!!.onSurfaceChanged(mGL, mWidth, mHeight)
    }
    // Do we have a renderer?

    // Does this thread own the OpenGL context?

    // Call the renderer draw routine
    val bitmap: Bitmap?
        get() {
            // Do we have a renderer?
            if (mRenderer == null) {
                Log.e(TAG, "getBitmap: Renderer was not set.")
                return null
            }

            // Does this thread own the OpenGL context?
            if (Thread.currentThread().name != mThreadOwner) {
                Log.e(TAG, "getBitmap: This thread does not own the OpenGL context.")
                return null
            }

            // Call the renderer draw routine
            mRenderer!!.onDrawFrame(mGL)
            convertToBitmap()
            return mBitmap
        }

    private fun chooseConfig(): EGLConfig? {
        val attribList = intArrayOf(
            EGL10.EGL_DEPTH_SIZE, 0,
            EGL10.EGL_STENCIL_SIZE, 0,
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_NONE
        )

        // No error checking performed, minimum required code to elucidate logic
        // Expand on this logic to be more selective in choosing a configuration
        val numConfig = IntArray(1)
        mEGL.eglChooseConfig(mEGLDisplay, attribList, null, 0, numConfig)
        val configSize = numConfig[0]
        mEGLConfigs = arrayOfNulls(configSize)
        mEGL.eglChooseConfig(mEGLDisplay, attribList, mEGLConfigs, configSize, numConfig)
        if (LIST_CONFIGS) {
            listConfig()
        }
        return mEGLConfigs[0] // Best match is probably the first configuration
    }

    private fun listConfig() {
        Log.i(TAG, "Config List {")
        for (config in mEGLConfigs) {

            // Expand on this logic to dump other attributes        
            val d: Int = getConfigAttrib(config, EGL10.EGL_DEPTH_SIZE)
            val s: Int = getConfigAttrib(config, EGL10.EGL_STENCIL_SIZE)
            val r: Int = getConfigAttrib(config, EGL10.EGL_RED_SIZE)
            val g: Int = getConfigAttrib(config, EGL10.EGL_GREEN_SIZE)
            val b: Int = getConfigAttrib(config, EGL10.EGL_BLUE_SIZE)
            val a: Int = getConfigAttrib(config, EGL10.EGL_ALPHA_SIZE)
            Log.i(
                TAG, "    <d,s,r,g,b,a> = <" + d + "," + s + "," +
                        r + "," + g + "," + b + "," + a + ">"
            )
        }
        Log.i(TAG, "}")
    }

    private fun getConfigAttrib(config: EGLConfig?, attribute: Int): Int {
        val value = IntArray(1)
        return if (mEGL.eglGetConfigAttrib(
                mEGLDisplay, config,
                attribute, value
            )
        ) value[0] else 0
    }

    private fun convertToBitmap() {
        val ib = IntBuffer.allocate(mWidth * mHeight)
        val ibt = IntBuffer.allocate(mWidth * mHeight)
        mGL.glReadPixels(0, 0, mWidth, mHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, ib)

        // Convert upside down mirror-reversed image to right-side up normal image.
        for (i in 0 until mHeight) {
            for (j in 0 until mWidth) {
                ibt.put((mHeight - i - 1) * mWidth + j, ib[i * mWidth + j])
            }
        }
        mBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888)
        mBitmap?.copyPixelsFromBuffer(ibt)
    }

    companion object {
        const val TAG = "PixelBuffer"
        const val LIST_CONFIGS = false

        //This constant appears as high as API 17
        const val EGL_CONTEXT_CLIENT_VERSION = 12440
    }
}