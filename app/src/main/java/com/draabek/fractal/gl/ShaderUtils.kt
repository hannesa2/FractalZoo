package com.draabek.fractal.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import java.util.*

/**
 * Created by Vojtech Drabek on 2018-03-10.
 */
object ShaderUtils {
    /**
     * Utility method for compiling a OpenGL shader.
     *
     *
     *
     * **Note:** When developing shaders, use the checkGlError()
     * method to debug shader coding errors.
     *
     * @param type       - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    fun loadShader(type: Int, shaderCode: String?): Int {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        val shader = GLES20.glCreateShader(type)

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        val s = GLES20.glGetShaderInfoLog(shader)
        if (s != null && s != "") Log.d(ShaderUtils::class.java.name, s)
        return shader
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    fun checkGlError(glOperation: String) {
        var error: Int
        if (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e(ShaderUtils::class.java.name, "$glOperation: glError $error")
            throw RuntimeException(String.format(Locale.US, "%s: glError %d", glOperation, error))
        }
    }

    /**
     * Pass fractal settings as uniforms to the shader
     *
     * @param settings      Map<String></String>, Float> of fractal float settings
     * @param shaderProgram Handle to the compiled shader program to attach the uniforms to
     */
    fun applyFloatUniforms(settings: Map<String, Float?>, shaderProgram: Int) {
        for (setting in settings.keys) {
            val uniformHandle = GLES20.glGetUniformLocation(shaderProgram, setting)
            if (uniformHandle == -1) {
                Log.w(ShaderUtils::class.java.name, "Unable to find uniform for $setting")
                throw RuntimeException("glGetUniformLocation $setting error")
            }
            // For now only support single float uniforms
            val o: Any? = settings[setting]
            val f = o as Float
            GLES20.glUniform1f(uniformHandle, f)
            checkGlError("glUniform1f")
        }
    }

    /**
     * Pass current width and height as uniform to the shader
     *
     * @param width         Current screen width
     * @param height        Current screen height
     * @param shaderProgram Handle to the compiled shader program to attach the uniforms to
     */
    fun applyResolutionUniform(width: Int, height: Int, shaderProgram: Int) {
        val resolutionHandle = GLES20.glGetUniformLocation(shaderProgram, "resolution")
        if (resolutionHandle == -1) {
            Log.w(ShaderUtils::class.java.name, "Unable to find uniform for resolution")
            throw RuntimeException("glGetUniformLocation resolution error")
        }
        GLES20.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
        checkGlError("glUniform2f")
    }

    /**
     * Loads a texture bitmap into OpenGL. Used for palettes
     *
     * @param bitmap The bitmap to load
     * @return The loaded texture ID
     */
    fun loadTexture(bitmap: Bitmap): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)
        if (textureHandle[0] == 0) {
            throw RuntimeException("Error generating texture name.")
        }

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

        // Set filtering
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle()
        return textureHandle[0]
    }
}