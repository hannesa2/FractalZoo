package com.draabek.fractal.gl

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

/**
 * Simple bitmap disk cache. Used when the view has nothing to show yet
 */
class RenderImageCache internal constructor() {
    private val cacheFileNames: MutableMap<String, String>

    init {
        cacheFileNames = HashMap()
    }

    fun add(bitmap: Bitmap, fractalName: String) {
        val cacheFile: File
        try {
            cacheFile = File.createTempFile(fractalName, "cache")
            if (cacheFile.exists()) {
                if (!cacheFile.delete()) {
                    Log.w(this.javaClass.name, String.format("Could not delete cache for %s", fractalName))
                }
            }
            cacheFileNames[fractalName] = cacheFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(cacheFile))
        } catch (e: FileNotFoundException) {
            Log.e(
                this.javaClass.name, String.format(
                    "Could not save cache to %s: %s", cacheFile.absolutePath, "" + e
                )
            )
        }
        cacheFileNames[fractalName] = cacheFile.absolutePath
    }

    operator fun get(fractalName: String): Bitmap? {
        val absolutePath = cacheFileNames[fractalName] ?: return null
        return BitmapFactory.decodeFile(absolutePath)
    }
}