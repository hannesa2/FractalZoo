package com.draabek.fractal.activity

import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.draabek.fractal.R
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import java.util.*

class FractalParametersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fractal_parameters)
        val heading = findViewById<TextView>(R.id.fractal_parameters_text)
        val currentParameters = instance.current!!.parameters
        val relativeLayout = findViewById<RelativeLayout>(R.id.layout_parameters)
        var lastId = heading.id
        val width = windowManager.defaultDisplay.width / 2
        for (parameter in currentParameters.keys) {
            val label = TextView(this)
            label.text = parameter
            val id = (Math.random() * Int.MAX_VALUE).toInt()
            label.id = id
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            label.width = width
            label.ellipsize = TextUtils.TruncateAt.END
            var layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.addRule(RelativeLayout.BELOW, lastId)
            relativeLayout.addView(label, layoutParams)
            val editText = EditText(this)
            editText.setText(String.format(Locale.getDefault(), "%f", currentParameters[parameter]))
            editText.width = width
            editText.id = (Math.random() * Int.MAX_VALUE).toInt()
            editText.onFocusChangeListener = OnFocusChangeListener { v: View, hasFocus: Boolean ->
                if (!hasFocus) {
                    val newText = (v as EditText).text.toString()
                    try {
                        val f = newText.toFloat()
                        currentParameters[parameter] = f
                    } catch (e: NumberFormatException) {
                        editText.setText(String.format(Locale.getDefault(), "%f", currentParameters[parameter]))
                    }
                }
            }
            layoutParams = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.addRule(RelativeLayout.RIGHT_OF, id)
            layoutParams.addRule(RelativeLayout.BELOW, lastId)
            relativeLayout.addView(editText, layoutParams)
            lastId = id
        }
        val button = Button(this)
        button.setText(R.string.parameters_ok_button)
        button.setOnClickListener { v: View? -> finish() }
        val layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        layoutParams.addRule(RelativeLayout.BELOW, lastId)
        layoutParams.setMargins(10, 10, 10, 10)
        relativeLayout.addView(button, layoutParams)
    }
}