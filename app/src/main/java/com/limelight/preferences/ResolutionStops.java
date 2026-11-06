package com.limelight.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// NOTE: The stops of the resolution bar: the entries the Resolution list holds right now (the standard ones the decoder can play,
// the native screen and a custom one), sorted by pixel count, so "-" and "+" walk from the smallest picture to the largest.
// NOTE: Pure Java with no Android types, so the unit tests run it directly. [NOT-TESTED]
public final class ResolutionStops {
    public static final class Stop {
        public final int width;
        public final int height;
        public final String name;
        public final String value;

        Stop(int width, int height, String name, String value) {
            this.width = width;
            this.height = height;
            this.name = name;
            this.value = value;
        }

        long pixels() {
            return (long) width * height;
        }
    }

    private final List<Stop> stops = new ArrayList<>();

    // Takes the list's names and values ("1920x1080"); a value that is not two numbers is left off the bar
    public ResolutionStops(CharSequence[] names, CharSequence[] values) {
        for (int i = 0; i < values.length; i++) {
            int[] wh = parse(values[i].toString());
            if (wh != null) {
                stops.add(new Stop(wh[0], wh[1], names[i].toString(), values[i].toString()));
            }
        }
        // A stable sort keeps the list order between two entries of the same size (landscape and portrait native)
        Collections.sort(stops, (a, b) -> Long.compare(a.pixels(), b.pixels()));
    }

    // "1920x1080" to {1920, 1080}; anything else gives null
    public static int[] parse(String value) {
        if (value == null) {
            return null;
        }
        String[] parts = value.trim().split("[xX×]");
        if (parts.length != 2) {
            return null;
        }
        try {
            int w = Integer.parseInt(parts[0].trim());
            int h = Integer.parseInt(parts[1].trim());
            return (w > 0 && h > 0) ? new int[]{w, h} : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // The value written to the setting for two numbers
    public static String value(int width, int height) {
        return width + "x" + height;
    }

    public int size() {
        return stops.size();
    }

    public Stop get(int index) {
        return stops.get(index);
    }

    // Index of the stop with exactly this size, or -1 when the size is not on the bar
    public int indexOf(int width, int height) {
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).width == width && stops.get(i).height == height) {
                return i;
            }
        }
        return -1;
    }

    // Index of the stop nearest in pixel count, where the bar stands for a size that is not on it
    public int nearest(int width, int height) {
        long px = (long) width * height;
        int best = 0;
        long bestDiff = Long.MAX_VALUE;
        for (int i = 0; i < stops.size(); i++) {
            long d = Math.abs(stops.get(i).pixels() - px);
            if (d < bestDiff) {
                bestDiff = d;
                best = i;
            }
        }
        return best;
    }

    // Stop after "+": the first one larger than the current size
    public int next(int width, int height) {
        long px = (long) width * height;
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i).pixels() > px) {
                return i;
            }
        }
        return stops.size() - 1;
    }

    // Stop after "-": the last one smaller than the current size
    public int previous(int width, int height) {
        long px = (long) width * height;
        for (int i = stops.size() - 1; i >= 0; i--) {
            if (stops.get(i).pixels() < px) {
                return i;
            }
        }
        return 0;
    }
}
