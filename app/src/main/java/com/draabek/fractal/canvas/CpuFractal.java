package com.draabek.fractal.canvas;

import com.draabek.fractal.fractal.Fractal;
import com.draabek.fractal.fractal.FractalViewWrapper;

/**
 * Created by Vojta on 07.09.2017.
 */

public class CpuFractal extends Fractal {

    @Override
    public Class<? extends FractalViewWrapper> getViewWrapper() {
        return FractalCpuView.class;
    }
}
