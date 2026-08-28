package com.qtone.seekbar;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2018/5/9
 * 描    述: it works for draw indicator text
 * ================================================
 */
public class SeekBarState {
    public String indicatorText;
    /**
     * now progress value
     */
    public float value;
    public boolean isMin;
    public boolean isMax;

    @Override
    public String toString() {
        return "indicatorText: " + indicatorText + " ,isMin: " + isMin + " ,isMax: " + isMax;
    }
}