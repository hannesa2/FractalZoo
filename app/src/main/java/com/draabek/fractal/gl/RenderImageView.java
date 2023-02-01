package com.draabek.fractal.gl;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.draabek.fractal.R;
import com.draabek.fractal.activity.SaveBitmapActivity;
import com.draabek.fractal.fractal.FractalRegistry;
import com.draabek.fractal.fractal.FractalViewWrapper;
import com.draabek.fractal.fractal.RenderListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import timber.log.Timber;

/**
 * Render image shaders asynchronously using OpenGL ES 2.0
 * Created by Vojtech Drabek on 2018-01-13
 */

public class RenderImageView extends androidx.appcompat.widget.AppCompatImageView implements FractalViewWrapper {

    private final Map<Long, Boolean> terminateThreads = new Hashtable<>();
    private boolean renderingFlag;
    private boolean reinitFlag;
    private PixelBuffer pixelBuffer;
    private SquareRenderer squareRenderer;

    private float mPreviousX;
    private float mPreviousY;
    private float mPreviousX2;
    private float mPreviousY2;
    private boolean gestureInProgress;

    private RenderImageCache renderImageCache;
    private RenderListener renderListener;

    public RenderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        listenToLayout();
    }

    public RenderImageView(Context context) {
        super(context);
        listenToLayout();
    }

    /**
     *
     */
    private void listenToLayout() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                init();
                reinitFlag = true;
                requestRender();
            }
        });
        renderImageCache = new RenderImageCache();
    }


    @Override
    public void setVisibility(int visibility) {
        int oldVisibility = this.getVisibility();
        super.setVisibility(visibility);
        if (visibility == VISIBLE && (oldVisibility != VISIBLE)) {
            requestRender();
        }
    }

    public void init() {
        Thread glThread = new Thread(() -> {
            Boolean exiting;
            do {
                exiting = terminateThreads.get(Thread.currentThread().getId());
                /*
                FIXME without this ugly check init is called everytime on startup.
                Turns out Android fires onGlobalLayout even with Visibility.GONE
                */
                if (this.getVisibility() == VISIBLE) {
                    if (reinitFlag) {
                        if ((exiting == null) || (exiting)) {
                            pixelBuffer = new PixelBuffer(getWidth(), getHeight());
                            squareRenderer = new SquareRenderer();
                            pixelBuffer.setRenderer(squareRenderer);
                            reinitFlag = false;
                        }
                    }
                    if (renderingFlag) {
                        exiting = terminateThreads.get(Thread.currentThread().getId());
                        if ((exiting == null) || (exiting)) {
                            long start = System.currentTimeMillis();
                            this.renderListener.onRenderRequested();
                            Bitmap bitmap = pixelBuffer.getBitmap();
                            this.post(() -> {
                                this.setImageBitmap(bitmap);
                                this.renderingFlag = false;
                                this.renderImageCache.add(bitmap, FractalRegistry.getInstance().getCurrent().getName());
                                this.renderListener.onRenderComplete(System.currentTimeMillis() - start);
                            });
                        }
                    }
                }
            } while ((exiting == null) || exiting);
        });
        glThread.start();
        terminateThreads.put(glThread.getId(), false);
    }

    //or just save current bitmap redundantly
    public Bitmap getBitmap() {
        BitmapDrawable bitmapDrawable = ((BitmapDrawable) getDrawable());
        Bitmap bitmap;
        if (bitmapDrawable == null) {
            buildDrawingCache();
            bitmap = getDrawingCache();
            destroyDrawingCache();
        } else {
            bitmap = bitmapDrawable.getBitmap();
        }
        return bitmap;
    }

    @Override
    public void saveBitmap() {
        try {
            File tmpFile = File.createTempFile("bitmap", ".img", getContext().getCacheDir());
            Bitmap bitmap = this.getBitmap();
            for (int i = 0; (i < 60) && (bitmap == null); i++) {
                Timber.d("Wait for draw %s", i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Timber.e(e);
                }
                bitmap = this.getBitmap();
            }
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100,
                    new FileOutputStream(tmpFile))) throw new IOException("Could not compress bitmap");
            Intent intent = new Intent(this.getContext(), SaveBitmapActivity.class);
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(getContext().getString(R.string.intent_extra_bitmap_file), tmpFile.getAbsolutePath());
            getContext().startActivity(intent);
        } catch (IOException e) {
            Toast.makeText(this.getContext(), "Could not save current image", Toast.LENGTH_SHORT).show();
            Timber.e(e);
        }
    }

    @Override
    public boolean isRendering() {
        return renderingFlag;
    }

    private void cancelAllThreads() {
        for (long key : terminateThreads.keySet()) {
            terminateThreads.put(key, true);
        }
    }

    public void requestRender() {
        if (isRendering()) cancelAllThreads();
        renderingFlag = true;
        if (getBitmap() != null) {
            Bitmap cachedBitmap = renderImageCache.get(
                    FractalRegistry.getInstance().getCurrent().getName());
            setImageBitmap(cachedBitmap);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float TOUCH_SCALE_FACTOR = 1.5f / Math.min(getWidth(), getHeight());

        float x = e.getX();
        float y = e.getY();
        float x2 = 0;
        float y2 = 0;
        if (e.getPointerCount() > 1) {
            x2 = e.getX(1);
            y2 = e.getY(1);
        }
        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (mPreviousX == 0) mPreviousX = x;
                if (mPreviousY == 0) mPreviousY = y;

                gestureInProgress = true;
                Timber.d("GL MOVE");
                if (e.getPointerCount() == 1) {
                    float dx = x - mPreviousX;
                    float dy = y - mPreviousY;
                    Float fractalX = FractalRegistry.getInstance().getCurrent()
                            .getParameters().get("centerX");
                    Float fractalY = FractalRegistry.getInstance().getCurrent()
                            .getParameters().get("centerY");
                    if ((fractalX == null) && (fractalY == null)) {
                        Timber.i("Fractal has no movable center");
                    } else {
                        if (fractalX != null) {
                            FractalRegistry.getInstance().getCurrent()
                                    .getParameters().put("centerX", fractalX + dx * TOUCH_SCALE_FACTOR);
                            Timber.v("X shift: %s", dx * TOUCH_SCALE_FACTOR);
                        }
                        if (fractalY != null) {
                            //- instead of + because OpenGL has y axis upside down
                            FractalRegistry.getInstance().getCurrent()
                                    .getParameters().put("centerY", fractalY - dy * TOUCH_SCALE_FACTOR);
                            Timber.v("Y shift: %s", dy * TOUCH_SCALE_FACTOR);
                        }
                    }
                } else if ((e.getPointerCount() == 2) && ((mPreviousY2 > 0) || (mPreviousX2 > 0))) {
                    Float scale = FractalRegistry.getInstance().getCurrent()
                            .getParameters().get("scale");
                    if (scale == null) {
                        Timber.i("Fractal is not scaleable");
                    } else {
                        // Probably abs() is sufficient, but this is better for clarity
                        float oldDist = (float) Math.sqrt((mPreviousX - mPreviousX2) * (mPreviousX - mPreviousX2) +
                                (mPreviousY - mPreviousY2) * (mPreviousY - mPreviousY2));
                        float newDist = (float) Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
                        if (oldDist > 0) {
                            FractalRegistry.getInstance().getCurrent().getParameters().put("scale",
                                    scale * newDist / oldDist);
                            Timber.v("Scale: %s", scale * newDist / oldDist);
                        }
                    }
                }
                mPreviousX = x;
                mPreviousY = y;
                mPreviousX2 = x2;
                mPreviousY2 = y2;
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                if (gestureInProgress) {
                    requestRender();
                    gestureInProgress = false;
                    return true;
                } else {
                    return performClick();
                }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public void setRenderListener(RenderListener renderListener) {
        this.renderListener = renderListener;
    }

    @Override
    public void clear() {
        setImageBitmap(null);
    }


}
