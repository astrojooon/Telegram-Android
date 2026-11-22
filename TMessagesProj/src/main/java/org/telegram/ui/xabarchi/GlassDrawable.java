package org.telegram.ui.xabarchi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

public class GlassDrawable extends Drawable implements TiltGlareController.Listener {

    private final Context context;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backdropPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    private final RectF drawRect = new RectF();
    private final RectF insetRect = new RectF();
    private final Path clipPath = new Path();

    private float cornerRadiusPx;
    private float[] cornerRadii;
    private boolean drawShadow = true;
    private boolean customBackdropHandledByView = false;
    private int sampledTintColor = Color.TRANSPARENT;
    private int alphaOverride = 255;

    private float tiltX = 0f;
    private float tiltY = 0f;
    private boolean listeningToTilt = false;

    @Nullable
    private Bitmap cachedBackdrop;

    private int lastGradientW = -1;
    private int lastGradientH = -1;
    private boolean lastDarkMode = false;

    public GlassDrawable(Context context, float cornerRadiusDp) {
        this.context = context;
        this.cornerRadiusPx = AndroidUtilities.dp(cornerRadiusDp);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(Math.max(1f, AndroidUtilities.dpf2(1f)));
        innerShadowPaint.setStyle(Paint.Style.STROKE);
        innerShadowPaint.setStrokeWidth(Math.max(1f, AndroidUtilities.dpf2(1.25f)));
        backdropPaint.setColorFilter(LiquidGlassShaders.getSaturationFilter());
    }

    public GlassDrawable setCornerRadiusDp(float dp) {
        this.cornerRadiusPx = AndroidUtilities.dp(dp);
        this.cornerRadii = null;
        invalidateSelf();
        return this;
    }

    public GlassDrawable setCornerRadiiPx(float[] radii8) {
        this.cornerRadii = radii8;
        invalidateSelf();
        return this;
    }

    public GlassDrawable setDrawShadow(boolean drawShadow) {
        this.drawShadow = drawShadow;
        invalidateSelf();
        return this;
    }

    public GlassDrawable setCustomBackdropHandledByView(boolean handled) {
        this.customBackdropHandledByView = handled;
        return this;
    }

    public void setSampledBackdropColor(int color) {
        if (this.sampledTintColor != color) {
            this.sampledTintColor = color;
            lastGradientW = -1;
            invalidateSelf();
        }
    }

    public void setBackdropBitmap(@Nullable Bitmap bitmap) {
        this.cachedBackdrop = bitmap;
        if (bitmap != null && !bitmap.isRecycled()) {
            this.sampledTintColor = sampleDominantColor(bitmap);
            lastGradientW = -1;
        }
        invalidateSelf();
    }

    public void attachTiltListener() {
        if (!listeningToTilt && LiquidGlassConfig.isTiltGlareEnabled()) {
            TiltGlareController.getInstance(context).addListener(this);
            listeningToTilt = true;
        }
    }

    public void detachTiltListener() {
        if (listeningToTilt) {
            TiltGlareController.getInstance(context).removeListener(this);
            listeningToTilt = false;
        }
    }

    public static void applyAsBackground(View target, float cornerRadiusDp) {
        if (target == null) {
            return;
        }
        GlassDrawable drawable = new GlassDrawable(target.getContext(), cornerRadiusDp);
        target.setBackground(drawable);
        target.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                drawable.attachTiltListener();
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                drawable.detachTiltListener();
            }
        });
        if (target.isAttachedToWindow()) {
            drawable.attachTiltListener();
        }
    }

    @Override
    public void onTiltUpdated(float tx, float ty) {
        this.tiltX = tx;
        this.tiltY = ty;
        invalidateSelf();
    }

    @Override
    protected void onBoundsChange(@NonNull Rect bounds) {
        super.onBoundsChange(bounds);
        lastGradientW = -1;
    }

    private void rebuildShadersIfNeeded(float w, float h, boolean dark) {
        if (w <= 0 || h <= 0) {
            return;
        }
        int iw = (int) w;
        int ih = (int) h;
        if (iw == lastGradientW && ih == lastGradientH && dark == lastDarkMode) {
            return;
        }
        lastGradientW = iw;
        lastGradientH = ih;
        lastDarkMode = dark;

        int tint = LiquidGlassConfig.getGlassTintColor(context, sampledTintColor);
        int topTint = adjustAlpha(tint, dark ? 1.08f : 1.15f);
        int bottomTint = adjustAlpha(tint, dark ? 0.92f : 0.85f);

        if (customBackdropHandledByView && LiquidGlassConfig.supportsAgslShader()) {
            fillPaint.setShader(null);
            fillPaint.setColor(Color.TRANSPARENT);
        } else {
            fillPaint.setShader(new LinearGradient(
                    0f, 0f, 0f, h,
                    new int[]{topTint, bottomTint},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            ));
        }

        int rimStart = LiquidGlassConfig.getRimHighlightColor(context);
        rimPaint.setShader(new LinearGradient(
                0f, 0f, w * 0.75f, h * 0.75f,
                new int[]{rimStart, Color.TRANSPARENT},
                new float[]{0f, 0.72f},
                Shader.TileMode.CLAMP
        ));

        int innerShadow = LiquidGlassConfig.getInnerShadowColor(context);
        innerShadowPaint.setShader(new LinearGradient(
                w, h, w * 0.25f, h * 0.25f,
                new int[]{innerShadow, Color.TRANSPARENT},
                new float[]{0f, 0.65f},
                Shader.TileMode.CLAMP
        ));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        float w = b.width();
        float h = b.height();
        boolean dark = LiquidGlassConfig.isDarkTheme(context);
        rebuildShadersIfNeeded(w, h, dark);

        drawRect.set(b);
        clipPath.reset();
        if (cornerRadii != null) {
            clipPath.addRoundRect(drawRect, cornerRadii, Path.Direction.CW);
        } else {
            float r = Math.min(cornerRadiusPx, Math.min(w, h) * 0.5f);
            clipPath.addRoundRect(drawRect, r, r, Path.Direction.CW);
        }

        if (drawShadow && !customBackdropHandledByView) {
            fillPaint.setShadowLayer(
                    AndroidUtilities.dp(14),
                    0f,
                    AndroidUtilities.dp(6),
                    LiquidGlassConfig.getShadowColor(context)
            );
        } else {
            fillPaint.clearShadowLayer();
        }

        int save = canvas.save();
        canvas.clipPath(clipPath);

        if (!customBackdropHandledByView && cachedBackdrop != null && !cachedBackdrop.isRecycled() && !LiquidGlassConfig.isSimpleMode()) {
            canvas.drawBitmap(cachedBackdrop, null, drawRect, backdropPaint);
        }

        fillPaint.setAlpha(alphaOverride);
        canvas.drawPath(clipPath, fillPaint);

        if (LiquidGlassConfig.isTiltGlareEnabled() && !(customBackdropHandledByView && LiquidGlassConfig.supportsAgslShader())) {
            float cx = b.left + w * (0.35f + tiltX * 0.30f);
            float cy = b.top + h * (0.25f + tiltY * 0.25f);
            int glarePeak = dark ? Color.argb(38, 255, 255, 255) : Color.argb(66, 255, 255, 255);
            glarePaint.setShader(new LinearGradient(
                    cx - w * 0.45f, cy - h * 0.45f,
                    cx + w * 0.45f, cy + h * 0.45f,
                    new int[]{Color.TRANSPARENT, glarePeak, Color.TRANSPARENT},
                    new float[]{0f, 0.5f, 1f},
                    Shader.TileMode.CLAMP
            ));
            canvas.drawRect(drawRect, glarePaint);
        }

        float inset = innerShadowPaint.getStrokeWidth() * 0.5f;
        insetRect.set(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset);
        drawRoundStroke(canvas, insetRect, innerShadowPaint);

        float rimInset = rimPaint.getStrokeWidth() * 0.5f;
        insetRect.set(b.left + rimInset, b.top + rimInset, b.right - rimInset, b.bottom - rimInset);
        drawRoundStroke(canvas, insetRect, rimPaint);

        canvas.restoreToCount(save);
    }

    private void drawRoundStroke(Canvas canvas, RectF rect, Paint paint) {
        if (cornerRadii != null) {
            Path p = new Path();
            p.addRoundRect(rect, cornerRadii, Path.Direction.CW);
            canvas.drawPath(p, paint);
        } else {
            float r = Math.min(cornerRadiusPx, Math.min(rect.width(), rect.height()) * 0.5f);
            canvas.drawRoundRect(rect, r, r, paint);
        }
    }

    public static int sampleDominantColor(Bitmap bmp) {
        if (bmp == null || bmp.isRecycled() || bmp.getWidth() == 0 || bmp.getHeight() == 0) {
            return Color.TRANSPARENT;
        }
        int r = 0, g = 0, b = 0, count = 0;
        int stepX = Math.max(1, bmp.getWidth() / 4);
        int stepY = Math.max(1, bmp.getHeight() / 4);
        for (int x = stepX / 2; x < bmp.getWidth(); x += stepX) {
            for (int y = stepY / 2; y < bmp.getHeight(); y += stepY) {
                int px = bmp.getPixel(x, y);
                r += Color.red(px);
                g += Color.green(px);
                b += Color.blue(px);
                count++;
            }
        }
        if (count == 0) {
            return Color.TRANSPARENT;
        }
        return Color.rgb(r / count, g / count, b / count);
    }

    private static int adjustAlpha(int color, float factor) {
        int a = Math.min(255, Math.max(0, Math.round(Color.alpha(color) * factor)));
        return Color.argb(a, Color.red(color), Color.green(color), Color.blue(color));
    }

    @Override
    public void setAlpha(int alpha) {
        this.alphaOverride = alpha;
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
