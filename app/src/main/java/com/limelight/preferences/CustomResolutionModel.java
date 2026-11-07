package com.limelight.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: Pure model of the Custom Resolution dialog, kept free of Android classes so it runs in plain unit tests.
// NOTE: Holds the aspect ratio choices, the stops of each ratio, the nearest stop of a typed size and the validation rules.
final class CustomResolutionModel {

    // Limits of a typed width or height, the same range Moonlight PC accepts for a custom resolution.
    static final int MIN_SIDE = 256;
    static final int MAX_SIDE = 8192;
    // Native stops are rounded to this step, so every stop is even and fits the encoder block grid better.
    static final int NATIVE_ROUND = 8;

    enum Ratio {
        NATIVE, R16_9, R16_10, R21_9, R4_3
    }

    enum Problem {
        NONE, EMPTY, OUT_OF_RANGE, ODD
    }

    static final class Stop {
        final int width;
        final int height;
        // Short label under the bar: "1080p", "4K" or a share of the native screen such as "75%".
        final String label;

        Stop(int width, int height, String label) {
            this.width = width;
            this.height = height;
            this.label = label;
        }

        long pixels() {
            return (long) width * height;
        }
    }

    private static final int[] NATIVE_SHARES = {25, 50, 75, 100};

    private final int nativeWidth;
    private final int nativeHeight;

    // nativeWidth and nativeHeight are the device screen in landscape order (the longer side first).
    CustomResolutionModel(int nativeWidth, int nativeHeight) {
        this.nativeWidth = Math.max(nativeWidth, nativeHeight);
        this.nativeHeight = Math.min(nativeWidth, nativeHeight);
    }

    int nativeWidth() {
        return nativeWidth;
    }

    int nativeHeight() {
        return nativeHeight;
    }

    // Label of a ratio on its segment; Native is localized by the dialog itself.
    static String ratioLabel(Ratio r) {
        switch (r) {
            case R16_9: return "16:9";
            case R16_10: return "16:10";
            case R21_9: return "21:9";
            case R4_3: return "4:3";
            default: return "";
        }
    }

    // Width divided by height of a ratio, used to draw the small frame on its segment.
    float ratioValue(Ratio r) {
        switch (r) {
            case R16_9: return 16f / 9f;
            case R16_10: return 16f / 10f;
            case R21_9: return 21f / 9f;
            case R4_3: return 4f / 3f;
            default: return nativeHeight > 0 ? (float) nativeWidth / nativeHeight : 16f / 9f;
        }
    }

    // The stops of a ratio, smallest first.
    List<Stop> stops(Ratio r) {
        List<Stop> list = new ArrayList<>();
        switch (r) {
            case R16_9:
                list.add(new Stop(1280, 720, "720p"));
                list.add(new Stop(1600, 900, "900p"));
                list.add(new Stop(1920, 1080, "1080p"));
                list.add(new Stop(2560, 1440, "1440p"));
                list.add(new Stop(3840, 2160, "4K"));
                break;
            case R16_10:
                list.add(new Stop(1280, 800, "800p"));
                list.add(new Stop(1440, 900, "900p"));
                list.add(new Stop(1920, 1200, "1200p"));
                list.add(new Stop(2560, 1600, "1600p"));
                list.add(new Stop(3840, 2400, "2400p"));
                break;
            case R21_9:
                list.add(new Stop(1720, 720, "720p"));
                list.add(new Stop(2560, 1080, "1080p"));
                list.add(new Stop(3440, 1440, "1440p"));
                list.add(new Stop(5120, 2160, "2160p"));
                break;
            case R4_3:
                list.add(new Stop(960, 720, "720p"));
                list.add(new Stop(1200, 900, "900p"));
                list.add(new Stop(1440, 1080, "1080p"));
                list.add(new Stop(1920, 1440, "1440p"));
                break;
            default:
                for (int share : NATIVE_SHARES) {
                    if (share == 100) {
                        list.add(new Stop(nativeWidth, nativeHeight, "100%"));
                    } else {
                        list.add(new Stop(roundTo(nativeWidth * share / 100f), roundTo(nativeHeight * share / 100f), share + "%"));
                    }
                }
                break;
        }
        return Collections.unmodifiableList(list);
    }

    private static int roundTo(float value) {
        return Math.max(NATIVE_ROUND, Math.round(value / NATIVE_ROUND) * NATIVE_ROUND);
    }

    // Index of the stop equal to the size, or -1.
    int indexOf(Ratio r, int width, int height) {
        List<Stop> list = stops(r);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).width == width && list.get(i).height == height) {
                return i;
            }
        }
        return -1;
    }

    // Ratio the dialog opens on: the first ratio that has the saved size as a stop, Native otherwise.
    Ratio ratioFor(int width, int height) {
        for (Ratio r : Ratio.values()) {
            if (indexOf(r, width, height) >= 0) {
                return r;
            }
        }
        return Ratio.NATIVE;
    }

    // Stop whose height is closest to the given height; used when the user switches the ratio.
    int closestByHeight(Ratio r, int height) {
        List<Stop> list = stops(r);
        int best = 0;
        for (int i = 1; i < list.size(); i++) {
            if (Math.abs(list.get(i).height - height) < Math.abs(list.get(best).height - height)) {
                best = i;
            }
        }
        return best;
    }

    // Position of a size on the bar, from 0 to stops-1: a stop sits on its index, a typed size between the two stops whose
    // pixel counts surround it.
    float position(Ratio r, int width, int height) {
        List<Stop> list = stops(r);
        int exact = indexOf(r, width, height);
        if (exact >= 0) {
            return exact;
        }
        long px = (long) width * height;
        if (px <= 0 || px <= list.get(0).pixels()) {
            return 0;
        }
        int last = list.size() - 1;
        if (px >= list.get(last).pixels()) {
            return last;
        }
        int i = 0;
        while (i < last && list.get(i + 1).pixels() <= px) {
            i++;
        }
        long a = list.get(i).pixels();
        long b = list.get(i + 1).pixels();
        return i + (float) (px - a) / (b - a);
    }

    // Stop the minus button goes to: the previous stop, or for a typed size the largest stop below it.
    int minus(Ratio r, int width, int height) {
        List<Stop> list = stops(r);
        int exact = indexOf(r, width, height);
        if (exact >= 0) {
            return Math.max(0, exact - 1);
        }
        long px = (long) width * height;
        int i = -1;
        for (int k = 0; k < list.size(); k++) {
            if (list.get(k).pixels() < px) {
                i = k;
            }
        }
        return Math.max(0, i);
    }

    // Stop the plus button goes to: the next stop, or for a typed size the smallest stop above it.
    int plus(Ratio r, int width, int height) {
        List<Stop> list = stops(r);
        int exact = indexOf(r, width, height);
        if (exact >= 0) {
            return Math.min(list.size() - 1, exact + 1);
        }
        long px = (long) width * height;
        for (int k = 0; k < list.size(); k++) {
            if (list.get(k).pixels() > px) {
                return k;
            }
        }
        return list.size() - 1;
    }

    // What is wrong with a typed size; the decoder check is made by the dialog, it needs the device's codecs.
    static Problem check(Integer width, Integer height) {
        if (width == null || height == null || width <= 0 || height <= 0) {
            return Problem.EMPTY;
        }
        if (width < MIN_SIDE || height < MIN_SIDE || width > MAX_SIDE || height > MAX_SIDE) {
            return Problem.OUT_OF_RANGE;
        }
        if (width % 2 != 0 || height % 2 != 0) {
            return Problem.ODD;
        }
        return Problem.NONE;
    }

    // Reads "1920x1080" (also with "×", "х", "X" or spaces) into {width, height}; null when it is not two numbers.
    static int[] parse(String value) {
        if (value == null) {
            return null;
        }
        // Separators: Latin x and X, the multiplication sign, Cyrillic small and capital ha, an asterisk, or plain spaces
        String[] parts = value.trim().split("\\s*[xX\u00D7\u0445\u0425*]\\s*|\\s+");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
