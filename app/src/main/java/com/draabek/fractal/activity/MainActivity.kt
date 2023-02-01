package com.draabek.fractal.activity

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
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
import timber.log.Timber
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
        val fractalList = arrayOfNulls<String>(fractals!!.size)
        for (i in fractals.indices) {
            val fractal = fractals[i]
            val inputStream = assets.open("fractals/$fractal")
            val json = readFully(inputStream)
            fractalList[i] = json
        }
        return fractalList
    }

    /**
     * Called when the activity is first created.
     */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            instance.init(MainActivity@ this, readFractalMetadata())
        } catch (e: IOException) {
            Timber.e("Exception loading fractal metadata")
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
            Timber.d("ACTION_UP")
            if (supportActionBar == null) return false
            if (supportActionBar!!.isShowing) supportActionBar!!.hide() else supportActionBar!!.show()
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Timber.d("onOptionsItemSelected: " + item.itemId)
        return when (item.itemId) {
            R.id.fractalList -> {
                Timber.d("Fractal list menu item pressed")
                val intent = Intent(this, FractalListActivity::class.java)
                intent.putExtra(
                    FractalListActivity.INTENT_HIERARCHY_PATH,
                    getIntent().getStringExtra(FractalListActivity.INTENT_HIERARCHY_PATH)
                )
                startActivityForResult(intent, CHOOSE_FRACTAL_CODE)
                true
            }
            R.id.save -> {
                Timber.d("Save menu item pressed")
                attemptSave()
            }
            R.id.parameters -> {
                Timber.d("Parameters menu item pressed")
                val intent2 = Intent(this, FractalParametersActivity::class.java)
                startActivity(intent2)
                true
            }
            R.id.options -> {
                Timber.d("Options menu item pressed")
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
            Timber.e(String.format("Fractal %s not found", newFractal))
            fractal = instance["Mandelbrot"]
        }
        assert(fractal != null)
        if (currentView != null) currentView!!.setVisibility(View.GONE)
        val requiredViewClass = fractal!!.viewWrapper
        val available = availableViews[requiredViewClass] ?: throw RuntimeException("No appropriate view available")
        currentView = available
        instance.current = fractal
        Timber.d("<${fractal.name}> is current")
        currentView!!.setVisibility(View.VISIBLE)
        currentView!!.clear()
        currentView!!.setRenderListener(object : RenderListener {
            override fun onRenderRequested() {
                Timber.i("Rendering requested on <${instance.current!!.name}>")
                progressBar!!.post { progressBar!!.visibility = View.VISIBLE }
            }

            override fun onRenderComplete(millis: Long) {
                Timber.i("Rendering complete in $millis ms on <${instance.current!!.name}>")
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
                Timber.e("Exception on fractal switch")
                Timber.e(e)
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
        const val CURRENT_FRACTAL_KEY = "current_fractal"
        const val CHOOSE_FRACTAL_CODE = 1
    }
}