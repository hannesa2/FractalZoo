package com.draabek.fractal.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.draabek.fractal.R
import com.draabek.fractal.canvas.FractalCpuView
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import com.draabek.fractal.fractal.FractalViewWrapper
import com.draabek.fractal.fractal.RenderListener
import com.draabek.fractal.gl.RenderImageView
import com.draabek.fractal.util.Utils
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    private lateinit var availableViews: MutableMap<Class<out FractalViewWrapper?>?, FractalViewWrapper>
    private var currentView: FractalViewWrapper? = null
    private lateinit var prefs: SharedPreferences
    private var progressBar: ProgressBar? = null
    @Throws(IOException::class)
    private fun readFully(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        val stringBuilder = StringBuilder()
        while (reader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
        return stringBuilder.toString()
    }

    @Throws(IOException::class)
    fun readFractalMetadata(): Array<String?> {
        val fractals = assets.list("fractals")
        val fractalStrings = arrayOfNulls<String>(fractals!!.size)
        for (i in fractals.indices) {
            val fractal = fractals[i]
            val `is` = assets.open("fractals/$fractal")
            val json = readFully(`is`)
            fractalStrings[i] = json
        }
        return fractalStrings
    }

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            instance.init(readFractalMetadata())
        } catch (e: IOException) {
            Log.e(LOG_KEY, "Exception loading fractal metadata")
            throw RuntimeException(e)
        }
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        //ugh
        var lastFractal = instance[prefs.getString(Utils.PREFS_CURRENT_FRACTAL_KEY, "Mandelbrot")!!]
        if (lastFractal == null) {
            lastFractal = instance["Mandelbrot"]
        }
        instance.current = lastFractal
        setContentView(R.layout.activity_main)

        //Put views into map where key is the view class, this is then requested from the fractal
        val renderImageView = findViewById<RenderImageView>(R.id.fractalGlView)
        availableViews = HashMap()
        availableViews[renderImageView.javaClass] = renderImageView
        val cpuView = findViewById<FractalCpuView>(R.id.fractalCpuView)
        availableViews[cpuView.javaClass] = cpuView
        progressBar = findViewById(R.id.indeterminateBar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        unveilCorrectView(instance.current!!.name)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            Log.d(LOG_KEY, "ACTION_UP")
            if (supportActionBar == null) return false
            if (supportActionBar!!.isShowing) supportActionBar!!.hide() else supportActionBar!!.show()
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(LOG_KEY, "onCreateOptionsMenu")
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(LOG_KEY, "onOptionsItemSelected: " + item.itemId)
        return when (item.itemId) {
            R.id.fractalList -> {
                Log.d(LOG_KEY, "Fractal list menu item pressed")
                val intent = Intent(this, FractalListActivity::class.java)
                intent.putExtra(
                    FractalListActivity.INTENT_HIERARCHY_PATH,
                    getIntent().getStringExtra(FractalListActivity.INTENT_HIERARCHY_PATH)
                )
                startActivityForResult(intent, CHOOSE_FRACTAL_CODE)
                true
            }
            R.id.save -> {
                Log.d(LOG_KEY, "Save menu item pressed")
                attemptSave()
            }
            R.id.parameters -> {
                Log.d(LOG_KEY, "Parameters menu item pressed")
                val intent2 = Intent(this, FractalParametersActivity::class.java)
                startActivity(intent2)
                true
            }
            R.id.options -> {
                Log.d(LOG_KEY, "Options menu item pressed")
                val intent3 = Intent(this, FractalPreferenceActivity::class.java)
                startActivity(intent3)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun attemptSave(): Boolean {
        currentView!!.saveBitmap()
        return true
    }

    /**
     * Make visible correct view according to the Fractal.getViewWrapper method
     *
     * @param newFractal Name of the current fractal
     */
    private fun unveilCorrectView(newFractal: String?) {
        var fractal = instance[newFractal!!]
        if (fractal == null) {
            Log.e(this.javaClass.name, String.format("Fractal %s not found", newFractal))
            fractal = instance["Mandelbrot"]
        }
        assert(fractal != null)
        if (currentView != null) currentView!!.setVisibility(View.GONE)
        val requiredViewClass = fractal!!.viewWrapper
        val available = availableViews[requiredViewClass] ?: throw RuntimeException("No appropriate view available")
        currentView = available
        instance.current = fractal
        Log.d(LOG_KEY, fractal.name + " is current")
        currentView!!.setVisibility(View.VISIBLE)
        currentView!!.clear()
        currentView!!.setRenderListener(object : RenderListener {
            override fun onRenderRequested() {
                Log.i(
                    this.javaClass.name, String.format(
                        "Rendering requested on %s",
                        instance.current!!.name
                    )
                )
                progressBar!!.post { progressBar!!.visibility = View.VISIBLE }
            }

            override fun onRenderComplete(millis: Long) {
                Log.i(this.javaClass.name, String.format("Rendering complete in %d ms", millis))
                progressBar!!.post { if (!currentView!!.isRendering) progressBar!!.visibility = View.GONE }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CHOOSE_FRACTAL_CODE) {
            try {
                val pickedFractal = data!!.getStringExtra(CURRENT_FRACTAL_KEY)
                unveilCorrectView(pickedFractal)
            } catch (e: Exception) {
                Log.e(LOG_KEY, "Exception on fractal switch")
                e.printStackTrace()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    public override fun onStart() {
        super.onStart()
    }

    public override fun onStop() {
        super.onStop()
    }

    companion object {
        private val LOG_KEY = MainActivity::class.java.name
        const val CURRENT_FRACTAL_KEY = "current_fractal"
        const val CHOOSE_FRACTAL_CODE = 1
    }
}