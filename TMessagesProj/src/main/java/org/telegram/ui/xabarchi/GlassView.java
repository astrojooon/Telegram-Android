package org.telegram.ui.xabarchi;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.RuntimeShader;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

public class GlassView extends FrameLayout implements TiltGlareController.Listener {

    private static final float BACKDROP_SCALE = 0.25f;
    private static final long MIN_CAPTURE_INTERVAL_MS = 16L;

    private static boolean globalCapturingBackdrop = false;

    private final GlassDrawable glassOverlay;
    private final BackdropView backdropView;

    private float cornerRadiusPx;
    private boolean capsuleMode = false;

    @Nullable
    private View backdropSourceRoot;
    @Nullable
    private Bitmap backdropBitmap;
    @Nullable
    private Canvas backdropCanvas;

    @Nullable
    private RuntimeShader refractionShader;
    @Nullable
    private RenderEffect cachedBlurSatEffect;
    @Nullable
    private RenderEffect cachedFinalEffect;

    private float lastTiltX = 0f;
    private float lastTiltY = 0f;
    private int lastEffectW = -1;
    private int lastEffectH = -1;
    private int lastTintArgb = 0;
    private long lastCaptureTimeMs = 0L;
    private boolean effectDirty = true;

    private final int[] selfLoc = new int[2];
    private final int[] rootLoc = new int[2];

    private final ViewTreeObserver.OnPreDrawListener preDrawListener = () -> {
        if (isAttachedToWindow() && isShown() && getVisibility() == VISIBLE && getWidth() > 0 && getHeight() > 0) {
            captureBackdropThrottled();
        }
        return true;
    };

    public GlassView(@NonNull Context context) {
        this(context, null);
    }

    public GlassView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GlassView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setClipToOutline(true);
        setElevation(AndroidUtilities.dp(10));

        cornerRadiusPx = AndroidUtilities.dp(24);
        glassOverlay = new GlassDrawable(context, 24f);
        glassOverlay.setCustomBackdropHandledByView(true);
        glassOverlay.setDrawShadow(false);

        backdropView = new BackdropView(context);
        super.addView(backdropView, 0, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float r = capsuleMode ? Math.min(view.getWidth(), view.getHeight()) * 0.5f : cornerRadiusPx;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), r);
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            setOutlineAmbientShadowColor(LiquidGlassConfig.getShadowColor(context));
            setOutlineSpotShadowColor(LiquidGlassConfig.getShadowColor(context));
        }
    }

    public void setCornerRadiusDp(float dp) {
        capsuleMode = false;
        cornerRadiusPx = AndroidUtilities.dp(dp);
        glassOverlay.setCornerRadiusDp(dp);
        effectDirty = true;
        invalidateOutline();
        invalidate();
    }

    public void setCapsule(boolean capsule) {
        capsuleMode = capsule;
        effectDirty = true;
        invalidateOutline();
        invalidate();
    }

    public void setBackdropSourceRoot(@Nullable View root) {
        this.backdropSourceRoot = root;
    }

    public void refreshGlassMode() {
        cachedFinalEffect = null;
        effectDirty = true;
        updateRenderEffectIfNeeded();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnPreDrawListener(preDrawListener);
        if (LiquidGlassConfig.isTiltGlareEnabled()) {
            TiltGlareController.getInstance(getContext()).addListener(this);
        }
        glassOverlay.attachTiltListener();
        effectDirty = true;
        updateRenderEffectIfNeeded();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
        TiltGlareController.getInstance(getContext()).removeListener(this);
        glassOverlay.detachTiltListener();
        releaseBackdropBitmap();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float r = capsuleMode ? Math.min(w, h) * 0.5f : cornerRadiusPx;
        glassOverlay.setBounds(0, 0, w, h);
        glassOverlay.setCornerRadiusDp(r / AndroidUtilities.density);
        effectDirty = true;
        updateRenderEffectIfNeeded();
    }

    @Override
    public void onTiltUpdated(float tiltX, float tiltY) {
        if (!isShown()) {
            return;
        }
        lastTiltX = tiltX;
        lastTiltY = tiltY;
        if (LiquidGlassConfig.supportsAgslShader()) {
            effectDirty = true;
            updateRenderEffectIfNeeded();
        } else {
            invalidate();
        }
    }

    private void captureBackdropThrottled() {
        if (globalCapturingBackdrop || LiquidGlassConfig.isSimpleMode()) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (now - lastCaptureTimeMs < MIN_CAPTURE_INTERVAL_MS) {
            return;
        }
        lastCaptureTimeMs = now;

        View source = backdropSourceRoot != null ? backdropSourceRoot : getRootView();
        if (source == null || source == this) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        int bw = Math.max(4, Math.round(w * BACKDROP_SCALE));
        int bh = Math.max(4, Math.round(h * BACKDROP_SCALE));

        if (backdropBitmap == null || backdropBitmap.getWidth() != bw || backdropBitmap.getHeight() != bh) {
            releaseBackdropBitmap();
            try {
                backdropBitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
                backdropCanvas = new Canvas(backdropBitmap);
            } catch (OutOfMemoryError ignored) {
                return;
            }
        }

        getLocationInWindow(selfLoc);
        source.getLocationInWindow(rootLoc);
        float dx = selfLoc[0] - rootLoc[0];
        float dy = selfLoc[1] - rootLoc[1];

        globalCapturingBackdrop = true;
        int prevVis = getVisibility();
        try {
            setVisibility(INVISIBLE);
            backdropBitmap.eraseColor(Color.TRANSPARENT);
            int save = backdropCanvas.save();
            backdropCanvas.scale(BACKDROP_SCALE, BACKDROP_SCALE);
            backdropCanvas.translate(-dx, -dy);
            source.draw(backdropCanvas);
            backdropCanvas.restoreToCount(save);
        } catch (Throwable ignored) {
        } finally {
            setVisibility(prevVis);
            globalCapturingBackdrop = false;
        }

        int sampled = GlassDrawable.sampleDominantColor(backdropBitmap);
        glassOverlay.setSampledBackdropColor(sampled);
        int newTint = LiquidGlassConfig.getGlassTintColor(getContext(), sampled);
        if (newTint != lastTintArgb) {
            lastTintArgb = newTint;
            effectDirty = true;
        }
        updateRenderEffectIfNeeded();
        backdropView.invalidate();
    }

    private void updateRenderEffectIfNeeded() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        if (LiquidGlassConfig.isSimpleMode() || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                backdropView.setRenderEffect(null);
            }
            return;
        }
        if (!effectDirty && w == lastEffectW && h == lastEffectH && cachedFinalEffect != null) {
            return;
        }

        float blurPx = AndroidUtilities.dpf2(LiquidGlassConfig.BLUR_RADIUS_DP);
        if (cachedBlurSatEffect == null) {
            cachedBlurSatEffect = LiquidGlassShaders.createBlurAndSaturationEffect(blurPx);
        }

        if (LiquidGlassConfig.supportsAgslShader() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (refractionShader == null) {
                refractionShader = LiquidGlassShaders.createRefractionShader();
            }
            if (refractionShader != null) {
                float r = capsuleMode ? Math.min(w, h) * 0.5f : cornerRadiusPx;
                int tint = lastTintArgb != 0 ? lastTintArgb : LiquidGlassConfig.getGlassTintColor(getContext(), Color.TRANSPARENT);
                float glare = LiquidGlassConfig.isTiltGlareEnabled() ? 0.14f : 0f;
                cachedFinalEffect = LiquidGlassShaders.createFullLiquidGlassEffect(
                        refractionShader,
                        w,
                        h,
                        r,
                        AndroidUtilities.dpf2(18f),
                        AndroidUtilities.dpf2(9f),
                        lastTiltX,
                        lastTiltY,
                        tint,
                        glare,
                        cachedBlurSatEffect
                );
            } else {
                cachedFinalEffect = cachedBlurSatEffect;
            }
        } else {
            cachedFinalEffect = cachedBlurSatEffect;
        }

        backdropView.setRenderEffect(cachedFinalEffect);
        lastEffectW = w;
        lastEffectH = h;
        effectDirty = false;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (globalCapturingBackdrop) {
            return;
        }
        drawChild(canvas, backdropView, getDrawingTime());
        glassOverlay.setBounds(0, 0, getWidth(), getHeight());
        glassOverlay.draw(canvas);
        int count = getChildCount();
        for (int i = 1; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                drawChild(canvas, child, getDrawingTime());
            }
        }
    }

    private void releaseBackdropBitmap() {
        if (backdropBitmap != null) {
            backdropBitmap.recycle();
            backdropBitmap = null;
        }
        backdropCanvas = null;
    }

    private final class BackdropView extends View {
        private final android.graphics.RectF dst = new android.graphics.RectF();

        BackdropView(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            if (LiquidGlassConfig.isSimpleMode()) {
                return;
            }
            if (backdropBitmap != null && !backdropBitmap.isRecycled()) {
                dst.set(0f, 0f, getWidth(), getHeight());
                canvas.drawBitmap(backdropBitmap, null, dst, null);
            }
        }
    }
}
