package com.draabek.fractal.fractal

import android.content.Context
import com.draabek.fractal.gl.GLSLFractal
import com.draabek.fractal.palette.ColorPalette
import com.draabek.fractal.util.SimpleTree
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class FractalRegistry private constructor() {
    private val fractals: MutableMap<String, Fractal> = LinkedHashMap()
    private val hierarchy: SimpleTree<String> = SimpleTree("All fractals")
    var current: Fractal? = null
    private var initialized = false

    fun getFractals(): Map<String, Fractal> {
        return fractals
    }

    private fun fractalFromJsonObject(context: Context, jsonObject: JsonObject): Fractal? {
        val name = jsonObject["name"].asString
        val clazz = jsonObject["class"].asString
        val shaders = if (jsonObject["shaders"] != null) jsonObject["shaders"].asString else null
        val settingsString = if (jsonObject["parameters"] != null) jsonObject["parameters"].toString() else null
        val thumbPath = if (jsonObject["thumbnail"] != null) jsonObject["thumbnail"].asString else null
        val paletteString = if (jsonObject["palette"] != null) jsonObject["palette"].asString else null
        val loadedShaders = loadShaders(context, shaders)
        val cls: Class<*>
        try {
            cls = Class.forName(clazz)
            //this is frontal lobotomy
            val fractal = cls.newInstance() as Fractal
            fractal.name = name
            if (thumbPath != null) {
                fractal.thumbPath = "thumbs/$thumbPath"
            }
            if (fractal is GLSLFractal) {
                if (loadedShaders == null) {
                    throw RuntimeException("No shaders loaded for $fractal")
                }
                fractal.setShaders(loadedShaders)
            }
            if (settingsString != null) {
                val retMap = Gson().fromJson<Map<String, Float>>(
                    settingsString, object : TypeToken<HashMap<String?, Float?>?>() {}.type
                )
                fractal.updateSettings(retMap)
            }
            if (paletteString != null) {
                val colorPalette = Class.forName(paletteString).newInstance() as ColorPalette
                fractal.colorPalette = colorPalette
            }
            return fractal
        } catch (e: ClassNotFoundException) {
            Timber.w("Cannot find fractal class $clazz")
        } catch (e: IllegalAccessException) {
            Timber.w("Cannot access fractal class $clazz")
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        }
        return null
    }

    private fun loadShaders(ctx: Context, shaderPathX: String?): Array<String>? {
        var shaderPath = shaderPathX ?: return null
        if (!shaderPath.contains("/")) shaderPath = "shaders/$shaderPath"
        return try {
            var br = BufferedReader(
                InputStreamReader(
                    ctx.assets.open(shaderPath + "_fragment.glsl")
                )
            )
            val fragmentShader = StringBuilder()
            var line: String?
            while (br.readLine().also { line = it } != null) {
                fragmentShader.append(line).append("\n")
            }
            val vertexIS: InputStream = try {
                ctx.assets.open(shaderPath + "_vertex.glsl")
            } catch (e: IOException) {
                Timber.d("Using default vertex shader for $shaderPath")
                ctx.assets.open("shaders/default_vertex.glsl")
            }
            br = BufferedReader(InputStreamReader(vertexIS))
            val vertexShader = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                vertexShader.append(line).append("\n")
            }
            arrayOf(vertexShader.toString(), fragmentShader.toString())
        } catch (e: IOException) {
            Timber.w("IOException loading fractal shaders for$shaderPath")
            null
        }
    }

    private fun processJsonElement(context: Context, jsonElement: JsonElement) {
        val jsonObject = jsonElement.asJsonObject
        val pathInList = jsonObject["path"].asString
        jsonObject.remove("path")
        val s = pathInList.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val fractal = fractalFromJsonObject(context, jsonObject)
        if (fractal != null) {
            fractals[fractal.name] = fractal
            hierarchy.putPath(s, fractal.name)
        } else {
            Timber.e(String.format("Cannot load fractal %s", jsonObject["name"].asString))
        }
    }

    fun init(context: Context, fractalList: Array<String?>) {
        if (initialized)
            return
        for (fractal in fractalList) {
            val jsonElement = Gson().fromJson(fractal, JsonElement::class.java)
            processJsonElement(context, jsonElement)
        }
        initialized = true
    }

    operator fun get(name: String): Fractal? {
        /*Fractal f = fractals.get(name);
		if (f != null) return f;
		if (Utils.DEBUG) {
            throw new RuntimeException("Fractal not found in registry: " + name);
        } else {
            Timber.w("Fractal not found in registry: " + name);
            return get("Mandelbrot");
        }*/
        return fractals[name]
    }

    fun getOnLevel(level: Deque<String>?): List<String>? {
        return hierarchy.getChildren(level)
    }

    companion object {
        @JvmStatic
        val instance = FractalRegistry()
    }
}