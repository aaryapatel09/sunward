package app.sunward;
/** No SPF multiplier: these are label-based reminders, never a burn-time estimate. */
public final class Rules {
    public static final long MINUTE = 60_000L;
    public static long due(long applied, long wetStarted, int waterMinutes) {
        long normal = applied + 120 * MINUTE;
        if (wetStarted <= 0) return normal;
        if (waterMinutes != 40 && waterMinutes != 80) return Math.min(normal, wetStarted);
        return Math.min(normal, wetStarted + waterMinutes * MINUTE);
    }
    public static String uvBand(double uv) {
        if (!Double.isFinite(uv) || uv < 0) return "Unavailable";
        if (uv < 3) return "Low";
        if (uv < 6) return "Moderate";
        if (uv < 8) return "High";
        if (uv < 11) return "Very high";
        return "Extreme";
    }
    public static int nearestHour(long[] times, long nowSeconds) {
        int best = -1; long distance = Long.MAX_VALUE;
        for (int i=0; i<times.length; i++) { long d=Math.abs(times[i]-nowSeconds); if(d<distance){best=i;distance=d;} }
        return distance <= 3600 ? best : -1;
    }
}
