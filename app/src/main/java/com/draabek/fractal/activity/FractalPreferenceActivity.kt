package com.draabek.fractal.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class FractalPreferenceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null)
            supportFragmentManager.beginTransaction().add(android.R.id.content, PrefFragment()).commit()
    }

    class PrefFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.sharedPreferencesName = PREFS_NAME
            addPreferencesFromResource(com.draabek.fractal.R.xml.options)
        }
    }

    companion object {
        const val PREFS_NAME = "defaults"
    }
}