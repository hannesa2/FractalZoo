package com.draabek.fractal.palette;

public class CopperPalette extends ColorPalette {

    private int[] intCache;

    public CopperPalette() {
        this(256);
    }

    public CopperPalette(int size) {
        intCache = new int[size];
        for (int i = 1; i < size; i++) {
            int rval = (int) ((Math.log(i) / Math.log(size) + Math.exp(-i)) * 0xff) & 0xff;
            intCache[i] = rval << 16 | rval / 2 << 8 | 0xff000000;
        }
    }

    @Override
    public int getColorInt(float intensity) {
        return intCache[(int) (intensity * intCache.length)];
    }

    @Override
    public int[] getColorsInt() {
        return intCache;
    }

}
