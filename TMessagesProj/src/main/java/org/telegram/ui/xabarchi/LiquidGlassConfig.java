package org.telegram.ui.xabarchi;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;

import org.telegram.messenger.ApplicationLoader;

public final class LiquidGlassConfig {
    private static final String PREFS_NAME = "xabarchi_liquid_glass_prefs";
    private static final String KEY_SIMPLE_MODE = "simple_mode";
    private static final String KEY_TILT_GLARE = "tilt_glare_enabled";
    private static final String KEY_REFRACTION = "refraction_enabled";
    private static final String KEY_FIRST_LOCALE_APPLIED = "first_uz_locale_applied";

    public static final int IOS_BLUE = 0xFF1C8DFF;
    public static final int IOS_BLUE_TOP = 0xFF33A1FF;
    public static final int IOS_BLUE_BOTTOM = 0xFF0A7AFF;

    public static final float BLUR_RADIUS_DP = 24f;
    public static final float SATURATION_BOOST = 1.8f;

    private static volatile boolean cachedLoaded = false;
    private static volatile boolean simpleMode = false;
    private static volatile boolean tiltGlareEnabled = true;
    private static volatile boolean refractionEnabled = true;

    private LiquidGlassConfig() {
    }

    private static SharedPreferences prefs() {
        Context ctx = ApplicationLoader.applicationContext;
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void ensureLoaded() {
        if (cachedLoaded || ApplicationLoader.applicationContext == null) {
            return;
        }
        SharedPreferences p = prefs();
        simpleMode = p.getBoolean(KEY_SIMPLE_MODE, false);
        tiltGlareEnabled = p.getBoolean(KEY_TILT_GLARE, true);
        refractionEnabled = p.getBoolean(KEY_REFRACTION, true);
        cachedLoaded = true;
    }

    public static boolean isSimpleMode() {
        ensureLoaded();
        return simpleMode;
    }

    public static void setSimpleMode(boolean enabled) {
        simpleMode = enabled;
        if (ApplicationLoader.applicationContext != null) {
            prefs().edit().putBoolean(KEY_SIMPLE_MODE, enabled).apply();
        }
    }

    public static boolean isTiltGlareEnabled() {
        ensureLoaded();
        return !simpleMode && tiltGlareEnabled;
    }

    public static void setTiltGlareEnabled(boolean enabled) {
        tiltGlareEnabled = enabled;
        if (ApplicationLoader.applicationContext != null) {
            prefs().edit().putBoolean(KEY_TILT_GLARE, enabled).apply();
        }
    }

    public static boolean isRefractionEnabled() {
        ensureLoaded();
        return !simpleMode && refractionEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    public static boolean supportsRenderEffectBlur() {
        ensureLoaded();
        return !simpleMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static boolean supportsAgslShader() {
        ensureLoaded();
        return !simpleMode && refractionEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }

    public static boolean isFirstUzLocaleApplied() {
        if (ApplicationLoader.applicationContext == null) {
            return true;
        }
        return prefs().getBoolean(KEY_FIRST_LOCALE_APPLIED, false);
    }

    public static void markFirstUzLocaleApplied() {
        if (ApplicationLoader.applicationContext != null) {
            prefs().edit().putBoolean(KEY_FIRST_LOCALE_APPLIED, true).apply();
        }
    }

    public static boolean isDarkTheme(Context context) {
        if (context == null) {
            return false;
        }
        int mask = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mask == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int getGlassTintColor(Context context, int sampledBackdropColor) {
        boolean dark = isDarkTheme(context);
        int base = dark ? Color.argb(122, 34, 36, 42) : Color.argb(112, 252, 253, 255);
        if (sampledBackdropColor == Color.TRANSPARENT) {
            return base;
        }
        int baseA = Color.alpha(base);
        int r = (int) (Color.red(base) * 0.86f + Color.red(sampledBackdropColor) * 0.14f);
        int g = (int) (Color.green(base) * 0.86f + Color.green(sampledBackdropColor) * 0.14f);
        int b = (int) (Color.blue(base) * 0.86f + Color.blue(sampledBackdropColor) * 0.14f);
        return Color.argb(baseA, clamp255(r), clamp255(g), clamp255(b));
    }

    public static int getShadowColor(Context context) {
        return isDarkTheme(context) ? Color.argb(115, 0, 0, 0) : Color.argb(46, 15, 25, 45);
    }

    public static int getRimHighlightColor(Context context) {
        return isDarkTheme(context) ? Color.argb(128, 255, 255, 255) : Color.argb(165, 255, 255, 255);
    }

    public static int getInnerShadowColor(Context context) {
        return isDarkTheme(context) ? Color.argb(95, 0, 0, 0) : Color.argb(38, 12, 22, 42);
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
