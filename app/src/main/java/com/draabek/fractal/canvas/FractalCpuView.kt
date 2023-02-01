package com.draabek.fractal.canvas

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.preference.PreferenceManager
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import com.draabek.fractal.R
import com.draabek.fractal.activity.SaveBitmapActivity
import com.draabek.fractal.fractal.FractalRegistry.Companion.instance
import com.draabek.fractal.fractal.FractalViewWrapper
import com.draabek.fractal.fractal.RenderListener
import com.draabek.fractal.util.Utils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FractalCpuView : SurfaceView, SurfaceHolder.Callback, FractalViewWrapper {

    private var fractalBitmap: Bitmap? = null
    private var fractal: CpuFractal? = null
    private var position: RectF? = null
    private var oldPosition: RectF? = null
    private var paint: Paint? = null
    private var bufferCanvas: Canvas? = null
    private var surfaceHolder: SurfaceHolder? = null
    private lateinit var prefs: SharedPreferences
    override var isRendering = false
        private set
    private var renderListener: RenderListener? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context) {
        init(context)
    }

    private fun init(context: Context) {
        surfaceHolder = getHolder()
        surfaceHolder?.addCallback(this)
        setOnTouchListener(MotionTracker())
        paint = Paint()
        prefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onDraw(canvas: Canvas) {
        isRendering = true
        if (renderListener != null) {
            renderListener!!.onRenderRequested()
        }
        val start = System.currentTimeMillis()
        Log.d(LOG_KEY, "onDraw")
        surfaceHolder = getHolder()
        synchronized(surfaceHolder!!) {
            if (fractalBitmap == null || fractalBitmap!!.height != canvas.height || fractalBitmap!!.width != canvas.width || bufferCanvas == null) {
                Log.v(LOG_KEY, "Reallocate buffer")
                fractalBitmap = Bitmap.createBitmap(
                    width, height,
                    Bitmap.Config.ARGB_8888
                )
                bufferCanvas = Canvas(fractalBitmap!!)
            }
            if (fractal is BitmapDrawFractal) {
                Log.v(LOG_KEY, "Start drawing to buffer")
                fractalBitmap = (fractal as BitmapDrawFractal).redrawBitmap(fractalBitmap, position)
            } else if (fractal is CanvasFractal) {
                Log.v(LOG_KEY, "Draw to canvas")
                (fractal as CanvasFractal).draw(bufferCanvas)
            } else {
                throw RuntimeException("Wrong fractal type for " + this.javaClass.name)
            }
            canvas.drawBitmap(fractalBitmap!!, 0f, 0f, paint)
        }
        Log.d(LOG_KEY, "finished onDraw")
        isRendering = false
        if (renderListener != null) {
            renderListener!!.onRenderComplete(System.currentTimeMillis() - start)
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int, width: Int,
        height: Int
    ) {
        this.surfaceHolder = holder
        Log.d(LOG_KEY, "surface changed")
        fractal = instance.current as CpuFractal?
        invalidate()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        setWillNotDraw(false)
        Log.d(LOG_KEY, "surface created")
        fractalBitmap = Bitmap.createBitmap(
            width, height,
            Bitmap.Config.ARGB_8888
        )
        position = RectF(1f, -2f, -1f, 1f)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(LOG_KEY, "surface destroyed")
        bufferCanvas = null
        fractalBitmap = null
        //consider apply instead of commit
        prefs.edit().putString(Utils.PREFS_CURRENT_FRACTAL_KEY, instance.current!!.name).apply()
    }

    fun startTranslate() {
        oldPosition = RectF()
        oldPosition!!.set(position!!)
    }

    fun translate(xshift: Float, yshift: Float) {
        gestureRedraw(xshift, yshift, 1f)
        Log.d(LOG_KEY, "Translate: $xshift horizontally, $yshift vertically")
    }

    fun startScale() {
        oldPosition = RectF()
        oldPosition!!.set(position!!)
    }

    fun endGesture() {
        Log.d(LOG_KEY, "Gesture ended, redrawing fractal")
        oldPosition = null
        invalidate()
    }

    fun scale(scale: Float) {
        gestureRedraw(0f, 0f, scale)
        Log.d(LOG_KEY, "Scale: $scale")
    }

    fun gestureRedraw(dx: Float, dy: Float, scale: Float) {
        Log.d(LOG_KEY, "Redrawing gesture")
        var canvas: Canvas? = null
        try {
            synchronized(surfaceHolder!!) {
                canvas = surfaceHolder!!.lockCanvas()
                if (scale != 1f) canvas?.scale(scale, scale)
                if (dx != 0f || dy != 0f) canvas?.translate(dx, dy)
            }
        } finally {
            if (canvas != null) {
                surfaceHolder!!.unlockCanvasAndPost(canvas)
                invalidate()
            }
        }
    }

    fun tap(x: Float, y: Float) {
        Log.d(LOG_KEY, "Single tap at point [$x, $y]")
    }

    override fun saveBitmap() {
        try {
            val tmpFile = File.createTempFile("bitmap", "jpg", this.context.cacheDir)
            fractalBitmap!!.compress(
                Bitmap.CompressFormat.JPEG, 100,
                FileOutputStream(tmpFile)
            )
            val intent = Intent(this.context, SaveBitmapActivity::class.java)
            intent.action = Intent.ACTION_SEND
            intent.putExtra(this.context.getString(R.string.intent_extra_bitmap_file), tmpFile.absolutePath)
            this.context.startActivity(intent)
        } catch (e: IOException) {
            Toast.makeText(this.context, "Could not save current image", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun setRenderListener(renderListener: RenderListener?) {
        this.renderListener = renderListener
    }

    override fun clear() {
        if (surfaceHolder == null) {
            return
        }
        var canvas: Canvas? = null
        try {
            synchronized(surfaceHolder!!) {
                canvas = surfaceHolder!!.lockCanvas()
                canvas?.drawColor(Color.BLACK)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (canvas != null) {
                surfaceHolder!!.unlockCanvasAndPost(canvas)
                invalidate()
            }
        }
    }

    internal inner class MotionTracker : OnTouchListener {
        private var distance = 0f
        private var origin: PointF? = null
        private var isMoveGesture = false
        private var isGesture = false
        fun update(evt: MotionEvent) {
            Log.d(LOG_KEY, "Update on screen touch")
            val x = evt.x
            val y = evt.y
            val action = evt.action and MotionEvent.ACTION_MASK
            if (action == MotionEvent.ACTION_DOWN) {
                Log.d(LOG_KEY, "Touch down")
                isGesture = false
            } else if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)
                && !isGesture
            ) {
                //single tap ocurred
                if (action == MotionEvent.ACTION_POINTER_UP) {
                    tap(x, y)
                }
            } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
                Log.d(LOG_KEY, "Second pointer landed, starting pinch gesture")
                isGesture = true
                isMoveGesture = false
                startScale()
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (!isGesture) {
                    isGesture = true
                    isMoveGesture = true
                    Log.d(LOG_KEY, "Starting move gesture")
                    origin = PointF(x, y)
                    startTranslate()
                } else if (isMoveGesture) {
                    Log.d(LOG_KEY, "Continuing move gesture")
                    if (origin == null) {
                        Log.d(LOG_KEY, "Should not happen: move gesture already started and origin is null")
                    } else {
                        translate(x - origin!!.x, y - origin!!.y)
                    }
                } else {
                    Log.d(LOG_KEY, "Continuing pinch gesture")
                    val n = evt.pointerCount
                    if (n < 2) return
                    val x2 = evt.getX(1)
                    val y2 = evt.getY(1)
                    val newDistance = Math.sqrt(((x - x2) * (x - x2) + (y - y2) * (y - y2)).toDouble()).toFloat()
                    if (distance != 0f) {
                        val ratio = newDistance / distance
                        scale(ratio)
                    } else {
                        distance = newDistance
                    }
                }
            } else if (action == MotionEvent.ACTION_UP) {
                Log.d(LOG_KEY, "Everything up, resetting gestures")
                isGesture = false
                distance = 0f
                origin = null
                endGesture()
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            update(event)
            v.performClick()
            return true
        }
    }

    companion object {
        private val LOG_KEY = FractalCpuView::class.java.name
    }
}