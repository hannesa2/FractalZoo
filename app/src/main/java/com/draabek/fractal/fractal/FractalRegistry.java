package com.draabek.fractal.fractal;

import static java.lang.Class.forName;

import android.content.Context;
import android.util.Log;

import com.draabek.fractal.activity.FractalZooApplication;
import com.draabek.fractal.gl.GLSLFractal;
import com.draabek.fractal.palette.ColorPalette;
import com.draabek.fractal.util.SimpleTree;
import com.draabek.fractal.util.Utils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FractalRegistry {
    private static final String LOG_KEY = FractalRegistry.class.getName();
    private static FractalRegistry instance = null;
    private final Map<String, Fractal> fractals;
    private final SimpleTree<String> hierarchy;
    private Fractal currentFractal = null;

    private FractalRegistry() {
        fractals = new LinkedHashMap<>();
        hierarchy = new SimpleTree<>("All fractals");
    }

    private boolean initialized = false;

    public static FractalRegistry getInstance() {
        if (instance == null) { //throw new IllegalStateException("Fractal registry not initialized");
            instance = new FractalRegistry();
        }
        return instance;
    }

    public Map<String, Fractal> getFractals() {
        return fractals;
    }

    private Fractal fractalFromJsonObject(JsonObject jsonObject) {
        String name = jsonObject.get("name").getAsString();
        String clazz = jsonObject.get("class").getAsString();
        String shaders = jsonObject.get("shaders") != null ? jsonObject.get("shaders").getAsString() : null;
        String settingsString = jsonObject.get("parameters") != null ?
                jsonObject.get("parameters").toString() : null;
        String thumbPath = jsonObject.get("thumbnail") != null ?
                jsonObject.get("thumbnail").getAsString() : null;
        String paletteString = jsonObject.get("palette") != null ?
                jsonObject.get("palette").getAsString() : null;
        Context ctx = FractalZooApplication.getContext();
        String[] loadedShaders = loadShaders(ctx, shaders);
        Class cls;
        try {
            cls = forName(clazz);
            //this is frontal lobotomy
            Fractal fractal = (Fractal) cls.newInstance();
            fractal.setName(name);
            if (thumbPath != null) {
                fractal.setThumbPath("thumbs/" + thumbPath);
            }
            if (fractal instanceof GLSLFractal) {
                if (loadedShaders == null) {
                    if (Utils.DEBUG) {
                        throw new RuntimeException("No shaders loaded for " + fractal);
                    } else {
                        Log.e(LOG_KEY, "No shaders loaded for " + fractal);
                        return null;
                    }
                }
                ((GLSLFractal) fractal).setShaders(loadedShaders);
            }
            if (settingsString != null) {
                Map<String, Float> retMap = new Gson().fromJson(
                        settingsString, new TypeToken<HashMap<String, Float>>() {
                        }.getType()
                );
                fractal.updateSettings(retMap);
            }
            if (paletteString != null) {
                ColorPalette colorPalette = (ColorPalette) Class.forName(paletteString).newInstance();
                fractal.setColorPalette(colorPalette);
            }
            return fractal;
        } catch (ClassNotFoundException e) {
            Log.w(LOG_KEY, "Cannot find fractal class " + clazz);
        } catch (IllegalAccessException e) {
            Log.w(LOG_KEY, "Cannot access fractal class " + clazz);
        } catch (InstantiationException e) {
            if (Utils.DEBUG) {
                throw new RuntimeException(e);
            } else {
                Log.w(LOG_KEY, "Cannot instantiate fractal class " + clazz);
            }
        }
        return null;
    }

    private String[] loadShaders(Context ctx, String shaderPath) {
        if (shaderPath == null) {
            return null;
        }
        if (!(shaderPath.contains("/"))) shaderPath = "shaders/" + shaderPath;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    ctx.getAssets().open(shaderPath + "_fragment.glsl")
            ));
            StringBuilder fragmentShader = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                fragmentShader.append(line).append("\n");
            }
            InputStream vertexIS;
            try {
                vertexIS = ctx.getAssets().open(shaderPath + "_vertex.glsl");
            } catch (IOException e) {
                Log.d(LOG_KEY, "Using default vertex shader for " + shaderPath);
                vertexIS = ctx.getAssets().open("shaders/default_vertex.glsl");
            }
            if (vertexIS == null) {//fallback to simplest vertex shader
                Log.e(LOG_KEY, "Not even default vertex shader found for fractal " + shaderPath);
                return null;
            }
            br = new BufferedReader(new InputStreamReader(vertexIS));
            StringBuilder vertexShader = new StringBuilder();
            while ((line = br.readLine()) != null) {
                vertexShader.append(line).append("\n");
            }
            return new String[]{vertexShader.toString(), fragmentShader.toString()};
        } catch (IOException e) {
            Log.w(LOG_KEY, "IOException loading fractal shaders for" + shaderPath);
            return null;
        }
    }

    private void processJsonElement(JsonElement jsonElement) {
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        String pathInList = jsonObject.get("path").getAsString();
        jsonObject.remove("path");
        String[] s = pathInList.split("\\|");
        Fractal fractal = fractalFromJsonObject(jsonObject);
        if (fractal != null) {
            fractals.put(fractal.getName(), fractal);
            hierarchy.putPath(s, fractal.getName());
        } else {
            Log.e(LOG_KEY, String.format("Cannot load fractal %s",
                    jsonObject.get("name").getAsString()));
        }
    }

    public void init(String[] fractalList) {
        if (initialized) return;
        for (String fractal : fractalList) {
            JsonElement jsonElement = new Gson().fromJson(fractal, JsonElement.class);
            processJsonElement(jsonElement);
        }
        initialized = true;
    }

    public Fractal get(String name) {
		/*Fractal f = fractals.get(name);
		if (f != null) return f;
		if (Utils.DEBUG) {
            throw new RuntimeException("Fractal not found in registry: " + name);
        } else {
            Log.w(LOG_KEY, "Fractal not found in registry: " + name);
            return get("Mandelbrot");
        }*/
        return fractals.get(name);
    }

    public List<String> getOnLevel(Deque<String> level) {
        return hierarchy.getChildren(level);
    }

    public Fractal getCurrent() {
        return currentFractal;
    }

    public void setCurrent(Fractal currentFractal) {
        this.currentFractal = currentFractal;
    }
}
