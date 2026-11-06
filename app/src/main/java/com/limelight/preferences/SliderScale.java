package com.limelight.preferences;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

// NOTE: Maps a stored setting value to a SeekBar position and back, and gives the step of the "-" and "+" buttons.
// NOTE: A linear scale moves by the setting's own step. A logarithmic scale (the bitrate) gives the low values the same room
// on the bar as the high ones, so 2 or 3 Mbps is as easy to set as 100, and its button step grows with the value.
// NOTE: Pure Java with no Android types, so the unit tests run it directly. [NOT-TESTED]
public final class SliderScale {
    // Number of SeekBar positions a logarithmic scale uses; fine enough that each position moves less than one button step
    static final int LOG_POSITIONS = 1000;

    // Button step ladder of the logarithmic scale, in units of the divisor (Mbps for the bitrate):
    // below 5 the step is 0.5, below 20 it is 1, below 50 it is 2, below 100 it is 5, and 10 above that
    private static final double[] LADDER_LIMITS = {5, 20, 50, 100};
    private static final double[] LADDER_STEPS = {0.5, 1, 2, 5, 10};

    private final int min;
    private final int max;
    private final int step;
    private final int divisor;
    private final boolean logarithmic;
    // Lowest value on the logarithmic part of the bar; a scale that starts at 0 keeps 0 alone on position 0
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

    // Highest SeekBar position; the SeekBar runs from 0 to this
    public int positions() {
        if (logarithmic) {
            return LOG_POSITIONS;
        }
        return (max - min) / step;
    }

    // Value shown for a SeekBar position, already rounded to what the buttons can reach
    public int valueAt(int position) {
        int p = Math.max(0, Math.min(positions(), position));
        if (!logarithmic) {
            return clamp(min + p * step);
        }
        if (min < logMin) {
            // Position 0 is the value below the logarithmic part (0 for "a quarter of the bitrate" in the metered setting)
            if (p == 0) {
                return min;
            }
            double v = logMin * Math.pow((double) max / logMin, (p - 1) / (double) (LOG_POSITIONS - 1));
            return clamp((int) Math.round(v));
        }
        double v = logMin * Math.pow((double) max / logMin, p / (double) LOG_POSITIONS);
        return clamp((int) Math.round(v));
    }

    // SeekBar position for a value; values between positions land on the nearest one
    public int positionOf(int value) {
        int v = clamp(value);
        if (!logarithmic) {
            return (v - min) / step;
        }
        if (min < logMin) {
            if (v < logMin) {
                return 0;
            }
            double f = Math.log((double) v / logMin) / Math.log((double) max / logMin);
            return 1 + (int) Math.round(f * (LOG_POSITIONS - 1));
        }
        double f = Math.log((double) v / logMin) / Math.log((double) max / logMin);
        return (int) Math.round(f * LOG_POSITIONS);
    }

    // Step of the buttons at a value: the setting's own step on a linear scale, the ladder on a logarithmic one
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

    // Value after "+": the next value on the grid of the current step
    public int plus(int value) {
        int s = buttonStep(value);
        return clamp((int) (Math.floor((double) value / s) * s) + s);
    }

    // Value after "-": the step is the one just below the value, so 5.0 goes to 4.5 and not to 4.0
    public int minus(int value) {
        if (value <= min) {
            return min;
        }
        int s = buttonStep(value - 1);
        int down = (int) (Math.ceil((double) value / s) * s) - s;
        if (down < logMin && logarithmic && min < logMin) {
            // Below the lowest logarithmic value only the value of position 0 is left
            return value > logMin ? logMin : min;
        }
        return clamp(down);
    }

    // Keeps a value inside the range and on the grid the buttons and the bar can reach
    public int clamp(int value) {
        int v = Math.max(min, Math.min(max, value));
        if (logarithmic && min < logMin && v < logMin) {
            // A typed value between 0 and the lowest step goes to the nearer of the two
            return v * 2 < logMin ? min : logMin;
        }
        int s = logarithmic ? buttonStep(v) : step;
        int base = logarithmic ? 0 : min;
        int rounded = base + Math.round((v - base) / (float) s) * s;
        return Math.max(min, Math.min(max, rounded));
    }

    // Keeps a value inside the range on the setting's own step only (500 kbps for the bitrate), so a typed or a stored
    // value like 15,5 Mbps stays as it is instead of jumping to the coarser grid of the buttons
    public int bound(int value) {
        int v = Math.max(min, Math.min(max, value));
        int rounded = min + Math.round((v - min) / (float) step) * step;
        return Math.max(min, Math.min(max, rounded));
    }

    // Text of a value for the field: one decimal when the setting has a divisor (10,0 Mbps in Russian), else a whole number
    public String format(int value, Locale locale) {
        if (divisor == 1) {
            return String.valueOf(value);
        }
        DecimalFormat f = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(locale));
        return f.format(value / (double) divisor);
    }

    // Value from what the user typed; accepts both "," and "." as the decimal mark, returns null for text that is not a number
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
