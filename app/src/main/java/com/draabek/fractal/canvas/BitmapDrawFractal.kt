package com.draabek.fractal.canvas

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF

abstract class BitmapDrawFractal : CpuFractal() {

    abstract fun redrawBitmap(bitmap: Bitmap?, rect: RectF?): Bitmap?
    @Deprecated("")
    abstract fun redrawBitmapPart(bitmap: Bitmap?, rect: RectF?, part: Rect?): Bitmap?
}