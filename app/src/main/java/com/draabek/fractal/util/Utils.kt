package com.draabek.fractal.util

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.IOException
import java.io.InputStream

object Utils {
    const val PREFS_CURRENT_FRACTAL_KEY = "prefs_current_fractal_key"
    const val PREFS_CURRENT_FRACTAL_PATH = "prefs_current_fractal_path"
    @JvmStatic
    fun getBitmapFromAsset(mgr: AssetManager, path: String?): Bitmap? {
        var inputStream: InputStream? = null
        var bitmap: Bitmap?
        try {
            inputStream = mgr.open(path!!)
            bitmap = BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            bitmap = null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (ignored: IOException) {
                }
            }
        }
        return bitmap
    }
}