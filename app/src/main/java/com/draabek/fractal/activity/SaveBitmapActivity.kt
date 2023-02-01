package com.draabek.fractal.activity

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.ContextCompat
import com.draabek.fractal.R
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

/**
 * A login screen that offers login via email/password.
 */
class SaveBitmapActivity : AppCompatActivity(), OnRequestPermissionsResultCallback {
    // UI references.
    private var radioGroup: RadioGroup? = null
    var filenameEdit: EditText? = null
    private var bitmapFile: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_bitmap)
        radioGroup = findViewById(R.id.save_bitmap_radio_group)
        filenameEdit = findViewById(R.id.bitmap_filename)
        val button = findViewById<Button>(R.id.save_bitmap_ok_button)
        bitmapFile = File(this.intent.getStringExtra(getString(R.string.intent_extra_bitmap_file)))
        val suggestedPath = file
        filenameEdit?.text?.append(suggestedPath.absolutePath)
        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View) {
                if (radioGroup?.getCheckedRadioButtonId() == R.id.bitmap_filename_radio) {
                    if (handlePermissions(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            getString(R.string.save_bitmap_storage_write_rationale), REQUEST_WRITE
                        )
                    ) {
                        saveToFile()
                    }
                } else if (radioGroup?.getCheckedRadioButtonId() == R.id.bitmap_set_as_background_radio) {
                    // handlePermissions(Manifest.permission.SET_WALLPAPER,
                    //       getString(R.string.save_bitmap_set_wallpaper_rationale), REQUEST_WALLPAPER);
                    setAsWallpaper()
                } else {
                    Log.e(this.javaClass.name, "Unknown radio button in SaveBitmapActivity")
                }
            }
        })
    }

    private fun saveToFile() {
        val filename = filenameEdit!!.text.toString()
        val path = filename.substring(0, filename.lastIndexOf("/") - 1)
        val dir = File(path)
        if (!storageAvailable()) {
            Log.e(LOG_KEY, "External storage not available")
            return
        }
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(LOG_KEY, "Directory specified does not exist and could not be created")
            return
        }
        val f = File(filename)
        saveBitmap(bitmapFile, f)
        Toast.makeText(
            this@SaveBitmapActivity, getString(R.string.save_bitmap_success_toast)
                    + f.absolutePath,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    private fun setAsWallpaper() {
        val myWallpaperManager = WallpaperManager.getInstance(this@SaveBitmapActivity)
        try {
            myWallpaperManager.setStream(FileInputStream(bitmapFile))
        } catch (e: IOException) {
            // Just be ugly in the logcat
            e.printStackTrace()
            finish()
        }
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveToFile()
            } else {
                Toast.makeText(
                    this@SaveBitmapActivity, getString(R.string.save_bitmap_storage_write_rationale),
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == REQUEST_WALLPAPER) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setAsWallpaper()
            } else {
                Toast.makeText(
                    this@SaveBitmapActivity, getString(R.string.save_bitmap_set_wallpaper_rationale),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        finish()
    }

    private fun handlePermissions(permission: String, rationale: String, code: Int): Boolean {
        if (ContextCompat.checkSelfPermission(this@SaveBitmapActivity, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@SaveBitmapActivity, permission)) {
                //Just display a Toast
                Toast.makeText(this@SaveBitmapActivity, rationale, Toast.LENGTH_SHORT).show()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(
                    { ActivityCompat.requestPermissions(this@SaveBitmapActivity, arrayOf(permission), code) },
                    Toast.LENGTH_SHORT.toLong()
                )
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this@SaveBitmapActivity, arrayOf(permission), code)
            }
            return false
        }
        return true
    }

    private fun saveBitmap(bitmapTempFile: File?, path: File) {
        var sourceChannel: FileChannel? = null
        var destChannel: FileChannel? = null
        try {
            sourceChannel = FileInputStream(bitmapTempFile).channel
            destChannel = FileOutputStream(path).channel
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size())
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                sourceChannel?.close()
                destChannel?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun storageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        // We can read and write the media
        return Environment.MEDIA_MOUNTED == state
    }

    private val file: File
        get() {
            val fileName = instance.current.toString() + System.currentTimeMillis() + ".jpg"
            return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath, fileName)
        }

    companion object {
        private val LOG_KEY = SaveBitmapActivity::class.java.name
        private const val REQUEST_WRITE = 1
        private const val REQUEST_WALLPAPER = 2
    }
}