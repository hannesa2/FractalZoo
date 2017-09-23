package com.draabek.fractal;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.draabek.fractal.fractal.CpuFractal;
import com.draabek.fractal.fractal.Fractal;
import com.draabek.fractal.fractal.FractalRegistry;
import com.draabek.fractal.fractal.GLSLFractal;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.io.Reader;

/*
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;

*/

public class FractalActivity extends AppCompatActivity {
    private static final String LOG_KEY = FractalActivity.class.getName();
    public static final String CURRENT_FRACTAL_KEY = "current_fractal";
    public static final int CHOOSE_FRACTAL_CODE = 1;

    private MyGLSurfaceView myGLSurfaceView;
    private FractalCpuView cpuView;
    private FractalViewHandler currentView;
    private SharedPreferences prefs;

    public FractalActivity() {
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Reader jsonReader = new InputStreamReader(this.getResources().openRawResource(R.raw.fractallist));
        JsonParser parser = new JsonParser();
        JsonElement fractalElement = parser.parse(jsonReader);
        JsonArray fractalArray = fractalElement.getAsJsonArray();
        FractalRegistry.getInstance().init(this, fractalArray);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //ugh
        FractalRegistry.getInstance().setCurrent(
                FractalRegistry.getInstance().get(prefs.getString(Utils.PREFS_CURRENT_FRACTAL_KEY, "Mandelbrot"))
        );
        setContentView(R.layout.main);
        myGLSurfaceView = (MyGLSurfaceView) findViewById(R.id.fractalGlView);
        cpuView = (FractalCpuView) findViewById(R.id.fractalCpuView);
        unveilCorrectView(FractalRegistry.getInstance().getCurrent().getName());
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LOG_KEY, "onCreateOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG_KEY, "onOptionsItemSelected: " + item.getItemId());
        switch (item.getItemId()) {
            case R.id.fractalList:
                Log.d(LOG_KEY, "Fractal list menu item pressed");
                Intent intent = new Intent(this, FractalListActivity.class);
                startActivityForResult(intent, CHOOSE_FRACTAL_CODE);
                return true;
            case R.id.save:
                Log.d(LOG_KEY, "Save menu item pressed");
                return attemptSave();
            case R.id.options:
                Log.d(LOG_KEY, "Options menu item pressed");
                Intent intent2 = new Intent(this, FractalPreferenceActivity.class);
                startActivity(intent2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean attemptSave() {
        currentView.saveBitmap();
        return true;
    }

    private void unveilCorrectView(String newFractal) {
        Fractal f = FractalRegistry.getInstance().get(newFractal);
        if (currentView != null) currentView.setVisibility(View.GONE);
        if (f instanceof CpuFractal) {
            currentView = cpuView;
            myGLSurfaceView.setVisibility(View.GONE);
        } else if (f instanceof GLSLFractal) {
            currentView = myGLSurfaceView;
            cpuView.setVisibility(View.GONE);
        }
        FractalRegistry.getInstance().setCurrent(f);
        Log.d(LOG_KEY, f.getName() + " is current");
        currentView.setVisibility(View.VISIBLE);
        currentView.invalidate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_FRACTAL_CODE) {
            try {
                String pickedFractal = data.getStringExtra(CURRENT_FRACTAL_KEY);
                unveilCorrectView(pickedFractal);
            } catch (Exception e) {
                Log.e(LOG_KEY, "Exception on fractal switch: " + e);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}