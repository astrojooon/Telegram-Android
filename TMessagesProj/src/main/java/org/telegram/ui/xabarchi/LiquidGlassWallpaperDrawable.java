package org.telegram.ui.xabarchi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class LiquidGlassWallpaperDrawable extends Drawable {

    private final Context context;
    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blob1Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blob2Paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blob3Paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int lastW = -1;
    private int lastH = -1;
    private boolean lastDark = false;

    public LiquidGlassWallpaperDrawable(Context context) {
        this.context = context;
    }

    private void buildGradients(int w, int h, boolean dark) {
        if (w <= 0 || h <= 0 || (w == lastW && h == lastH && dark == lastDark)) {
            return;
        }
        lastW = w;
        lastH = h;
        lastDark = dark;

        if (dark) {
            basePaint.setShader(new LinearGradient(
                    0, 0, w, h,
                    new int[]{0xFF0A1020, 0xFF10192D, 0xFF0B1322},
                    new float[]{0f, 0.52f, 1f},
                    Shader.TileMode.CLAMP
            ));
            blob1Paint.setShader(new RadialGradient(
                    w * 0.18f, h * 0.15f, Math.max(w, h) * 0.55f,
                    0x551C8DFF, Color.TRANSPARENT, Shader.TileMode.CLAMP
            ));
            blob2Paint.setShader(new RadialGradient(
                    w * 0.85f, h * 0.45f, Math.max(w, h) * 0.52f,
                    0x448B5CF6, Color.TRANSPARENT, Shader.TileMode.CLAMP
            ));
            blob3Paint.setShader(new RadialGradient(
                    w * 0.35f, h * 0.88f, Math.max(w, h) * 0.50f,
                    0x4406B6D4, Color.TRANSPARENT, Shader.TileMode.CLAMP
            ));
        } else {
            basePaint.setShader(new LinearGradient(
                    0, 0, w, h,
                    new int[]{0xFFE8F2FF, 0xFFF4EEFF, 0xFFE9F8FF},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            blob1Paint.setShader(new RadialGradient(
                    w * 0.15f, h * 0.12f, Math.max(w, h) * 0.55f,
                    0x667CC6FF, Color.TRANSPARENT, Shader.TileMode.CLAMP
            ));
            blob2Paint.setShader(new RadialGradient(
                    w * 0.88f, h * 0.42f, Math.max(w, h) * 0.52f,
                    0x55C7A6FF, Color.TRANSPARENT, Shader.TileMode.CLAMP
            ));
            blob3Paint.setShader(new RadialGradient(
                    w * 0.28f, h * 0.90f, Math.max(w, h) * 0.48f,
                    0x557CE8E0, Color.TRANSPARENT, Shader.TileMode.CLAMP
            ));
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        buildGradients(b.width(), b.height(), LiquidGlassConfig.isDarkTheme(context));
        canvas.drawRect(b, basePaint);
        canvas.drawRect(b, blob1Paint);
        canvas.drawRect(b, blob2Paint);
        canvas.drawRect(b, blob3Paint);
    }

    @Override
    public void setAlpha(int alpha) {
        basePaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        basePaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }
}
