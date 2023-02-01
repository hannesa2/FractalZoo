package com.draabek.fractal.activity

import android.app.ListActivity
import android.content.Intent
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import com.draabek.fractal.R
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import com.draabek.fractal.util.Utils
import com.draabek.fractal.util.Utils.getBitmapFromAsset
import timber.log.Timber
import java.util.*

class FractalListActivity : ListActivity() {

    private var hierarchyPath: Deque<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val keyPathStr = intent.getStringExtra(INTENT_HIERARCHY_PATH)
        val path = keyPathStr?.split("\\|".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray() ?: arrayOf()
        hierarchyPath = ArrayDeque(Arrays.asList(*path))
        val data: MutableList<Map<String, String?>> = ArrayList()
        for (item in instance.getOnLevel(hierarchyPath)!!) {
            val row: MutableMap<String, String?> = HashMap()
            row["name"] = item
            val fractal = instance.getFractals()[item]
            row["thumbnail"] = fractal?.thumbPath
            data.add(row)
        }
        val adapter = SimpleAdapter(
            this,
            data,
            R.layout.list_view_row, arrayOf("name", "thumbnail"), intArrayOf(R.id.list_view_name, R.id.list_view_thumb)
        )
        adapter.viewBinder = SimpleAdapter.ViewBinder { view: View?, data1: Any?, textRepresentation: String? ->
            if (view is ImageView && data1 != null) {
                val s = data1 as String
                if (s.startsWith("file://")) {
                    val imageUri = Uri.parse(s)
                    view.setImageURI(imageUri)
                } else {
                    val bmp = getBitmapFromAsset(assets, s)
                    view.setImageBitmap(bmp)
                }
                Timber.v("Thumb uri: $data1")
            } else if (view is TextView) {
                val localizedName: String? = try {
                    resources.getString(
                        resources.getIdentifier(data1 as String?, "string", "com.draabek.fractal")
                    )
                } catch (e: NotFoundException) {
                    data1 as String?
                }
                view.text = localizedName
            }
            true
        }
        listAdapter = adapter
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val item = (listView.getItemAtPosition(position) as Map<String?, String?>)["name"]
        val fractal = instance[item!!]
        if (fractal == null) {
            Timber.d(String.format("Menu item %s clicked", item))
            val intent = Intent(this, FractalListActivity::class.java)
            //String path = String.join(" ", hierarchyPath); //API 26
            val pathBuilder = StringBuilder()
            for (i in hierarchyPath!!.indices) {
                pathBuilder.append(hierarchyPath!!.pop()).append("|")
            }
            pathBuilder.append(item)
            val path = pathBuilder.toString()
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = prefs.edit()
            editor.putString(Utils.PREFS_CURRENT_FRACTAL_PATH, path)
            editor.apply()
            if (path != "") intent.putExtra(INTENT_HIERARCHY_PATH, pathBuilder.toString())
            startActivity(intent)
        } else {
            Timber.d(String.format("Fractal %s clicked", fractal.name))
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra(MainActivity.CURRENT_FRACTAL_KEY, fractal.name)
            intent.putExtra(INTENT_HIERARCHY_PATH, getIntent().getStringExtra(INTENT_HIERARCHY_PATH))
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = prefs.edit()
            editor.putString(Utils.PREFS_CURRENT_FRACTAL_KEY, fractal.name)
            editor.apply()
            startActivity(intent)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (hierarchyPath!!.size < 1) return super.onKeyDown(keyCode, event)
            val intent = Intent(this, FractalListActivity::class.java)
            val pathBuilder = StringBuilder()
            for (i in 0 until hierarchyPath!!.size - 1) {
                pathBuilder.append(hierarchyPath!!.pop()).append("|")
            }
            if (pathBuilder.isNotEmpty()) {
                pathBuilder.deleteCharAt(pathBuilder.length - 1)
                intent.putExtra(INTENT_HIERARCHY_PATH, pathBuilder.toString())
            }
            startActivity(intent)
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        const val INTENT_HIERARCHY_PATH = "INTENT_HIERARCHY_PATH"
    }
}