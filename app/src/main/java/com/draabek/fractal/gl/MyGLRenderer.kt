package com.draabek.fractal.gl

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.widget.Toast
import com.draabek.fractal.R
import com.draabek.fractal.activity.SaveBitmapActivity
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer(private var context: Context) : GLSurfaceView.Renderer {
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
        // Adjust the viewport based on geometry changes, such as screen rotation
        GLES20.glViewport(0, 0, width, height)
        this.width = width
        this.height = height
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
}