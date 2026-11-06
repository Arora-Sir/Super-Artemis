package com.limelight.preferences;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

// NOTE: Maps a stored setting value to a SeekBar position and back, and gives the step of the minus and plus buttons.
// NOTE: A linear scale moves by the setting own step.
// NOTE: A calibrated power scale gives the low and medium values ample room on the bar.
// NOTE: 35 to 40 Mbps sits near the center, and button steps provide fractional precision.
// NOTE: Pure Java with no Android types, so unit tests run it directly.
public final class SliderScale {
    // Number of SeekBar positions a scaled curve uses.
    static final int LOG_POSITIONS = 1000;
    // Power exponent for the non-linear scale. An exponent of 3.0 centers 38 Mbps at the 50 percent mark.
    private static final double POWER_EXPONENT = 3.0;

    // Button step ladder of the scaled curve, in units of the divisor.
    // Below 50 the step is 0.5, below 100 it is 1.0, and 5.0 above that.
    private static final double[] LADDER_LIMITS = {50, 100};
    private static final double[] LADDER_STEPS = {0.5, 1.0, 5.0};

    private final int min;
    private final int max;
    private final int step;
    private final int divisor;
    private final boolean logarithmic;
    // Lowest value on the scaled part of the bar. A scale starting at 0 keeps 0 alone on position 0.
    private final int logMin;

    public SliderScale(int min, int max, int step, int divisor, boolean logarithmic) {
        this.min = min;
        this.max = max;
        this.step = Math.max(1, step);
        this.divisor = Math.max(1, divisor);
        this.logarithmic = logarithmic;
        this.logMin = Math.max(min, this.step);
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean isLogarithmic() {
        return logarithmic;
    }

    // Highest SeekBar position. The SeekBar runs from 0 to this.
    public int positions() {
        if (logarithmic) {
            return LOG_POSITIONS;
        }
        return (max - min) / step;
    }

    // Value shown for a SeekBar position, already rounded to what the buttons can reach.
    public int valueAt(int position) {
        int p = Math.max(0, Math.min(positions(), position));
        if (!logarithmic) {
            return clamp(min + p * step);
        }
        if (min < logMin) {
            // Position 0 is the value below the scaled part (0 for a quarter of the bitrate in metered settings).
            if (p == 0) {
                return min;
            }
            double t = (p - 1) / (double) (LOG_POSITIONS - 1);
            double v = logMin + (max - logMin) * Math.pow(t, POWER_EXPONENT);
            return clamp((int) Math.round(v));
        }
        double t = p / (double) LOG_POSITIONS;
        double v = logMin + (max - logMin) * Math.pow(t, POWER_EXPONENT);
        return clamp((int) Math.round(v));
    }

    // SeekBar position for a value. Values between positions land on the nearest one.
    public int positionOf(int value) {
        int v = clamp(value);
        if (!logarithmic) {
            return (v - min) / step;
        }
        if (min < logMin) {
            if (v < logMin) {
                return 0;
            }
            double t = Math.pow((v - logMin) / (double) (max - logMin), 1.0 / POWER_EXPONENT);
            return 1 + (int) Math.round(t * (LOG_POSITIONS - 1));
        }
        double t = Math.pow((v - logMin) / (double) (max - logMin), 1.0 / POWER_EXPONENT);
        return (int) Math.round(t * LOG_POSITIONS);
    }

    // Step of the buttons at a value: setting step on linear scale, ladder on scaled curve.
    public int buttonStep(int value) {
        if (!logarithmic) {
            return step;
        }
        double units = value / (double) divisor;
        for (int i = 0; i < LADDER_LIMITS.length; i++) {
            if (units < LADDER_LIMITS[i]) {
                return Math.max(step, (int) Math.round(LADDER_STEPS[i] * divisor));
            }
        }
        return Math.max(step, (int) Math.round(LADDER_STEPS[LADDER_STEPS.length - 1] * divisor));
    }

    // Value after plus: the next value on the grid of the current step.
    public int plus(int value) {
        int s = buttonStep(value);
        return clamp((int) (Math.floor((double) value / s) * s) + s);
    }

    // Value after minus: step is the one just below the value.
    public int minus(int value) {
        if (value <= min) {
            return min;
        }
        int s = buttonStep(value - 1);
        int down = (int) (Math.ceil((double) value / s) * s) - s;
        if (down < logMin && logarithmic && min < logMin) {
            return value > logMin ? logMin : min;
        }
        return clamp(down);
    }

    // Keeps a value inside the range and on the grid the buttons and the bar can reach.
    public int clamp(int value) {
        int v = Math.max(min, Math.min(max, value));
        if (logarithmic && min < logMin && v < logMin) {
            return v * 2 < logMin ? min : logMin;
        }
        int s = logarithmic ? buttonStep(v) : step;
        int base = logarithmic ? 0 : min;
        int rounded = base + Math.round((v - base) / (float) s) * s;
        return Math.max(min, Math.min(max, rounded));
    }

    // Keeps a value inside the range on the setting step only.
    public int bound(int value) {
        int v = Math.max(min, Math.min(max, value));
        int rounded = min + Math.round((v - min) / (float) step) * step;
        return Math.max(min, Math.min(max, rounded));
    }

    // Text of a value for the field: one decimal when the setting has a divisor.
    public String format(int value, Locale locale) {
        if (divisor == 1) {
            return String.valueOf(value);
        }
        DecimalFormat f = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(locale));
        return f.format(value / (double) divisor);
    }

    // Value from user input. Accepts both comma and period as decimal mark.
    public Integer parse(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim().replace(',', '.');
        if (t.isEmpty()) {
            return null;
        }
        try {
            double d = Double.parseDouble(t);
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return null;
            }
            return bound((int) Math.round(d * divisor));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
