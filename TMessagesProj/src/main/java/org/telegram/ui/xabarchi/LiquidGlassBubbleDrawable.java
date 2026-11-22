package org.telegram.ui.xabarchi;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;

public class LiquidGlassBubbleDrawable extends Drawable {

    private final Context context;
    private final boolean isOutgoing;
    private final GlassDrawable incomingGlass;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path bubblePath = new Path();
    private final RectF rectF = new RectF();

    private int lastW = -1;
    private int lastH = -1;

    public LiquidGlassBubbleDrawable(Context context, boolean isOutgoing) {
        this.context = context;
        this.isOutgoing = isOutgoing;
        this.incomingGlass = isOutgoing ? null : new GlassDrawable(context, 18f).setDrawShadow(false);
        rimPaint.setStyle(Paint.Style.STROKE);
        rimPaint.setStrokeWidth(AndroidUtilities.dpf2(1f));
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) {
            return;
        }
        float h = b.height();
        float r = AndroidUtilities.dp(18);
        float tail = AndroidUtilities.dp(6);

        if (!isOutgoing && incomingGlass != null) {
            float[] radii = new float[]{r, r, r, r, r, r, AndroidUtilities.dp(5), AndroidUtilities.dp(5)};
            incomingGlass.setCornerRadiiPx(radii);
            incomingGlass.setBounds(b.left + (int) tail, b.top, b.right, b.bottom);
            incomingGlass.draw(canvas);
            return;
        }

        if (b.width() != lastW || b.height() != lastH) {
            lastW = b.width();
            lastH = b.height();
            fillPaint.setShader(new LinearGradient(
                    0f, b.top, 0f, b.bottom,
                    new int[]{LiquidGlassConfig.IOS_BLUE_TOP, LiquidGlassConfig.IOS_BLUE_BOTTOM},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            ));
            glossPaint.setShader(new LinearGradient(
                    0f, b.top, 0f, b.top + h * 0.45f,
                    new int[]{Color.argb(90, 255, 255, 255), Color.TRANSPARENT},
                    new float[]{0f, 1f},
                    Shader.TileMode.CLAMP
            ));
            rimPaint.setShader(new LinearGradient(
                    b.left, b.top, b.right, b.bottom,
                    new int[]{Color.argb(145, 255, 255, 255), Color.TRANSPARENT},
                    new float[]{0f, 0.7f},
                    Shader.TileMode.CLAMP
            ));
        }

        rectF.set(b.left, b.top, b.right - tail, b.bottom);
        bubblePath.reset();
        float[] radii = new float[]{r, r, r, r, AndroidUtilities.dp(5), AndroidUtilities.dp(5), r, r};
        bubblePath.addRoundRect(rectF, radii, Path.Direction.CW);

        bubblePath.moveTo(rectF.right - AndroidUtilities.dp(4), rectF.bottom - AndroidUtilities.dp(12));
        bubblePath.quadTo(rectF.right, rectF.bottom, b.right, rectF.bottom);
        bubblePath.quadTo(rectF.right - AndroidUtilities.dp(4), rectF.bottom + AndroidUtilities.dp(1), rectF.right - AndroidUtilities.dp(10), rectF.bottom - AndroidUtilities.dp(2));
        bubblePath.close();

        canvas.drawPath(bubblePath, fillPaint);

        int save = canvas.save();
        canvas.clipPath(bubblePath);
        canvas.drawRect(rectF.left, rectF.top, rectF.right, rectF.top + h * 0.48f, glossPaint);
        canvas.restoreToCount(save);

        canvas.drawPath(bubblePath, rimPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        if (incomingGlass != null) {
            incomingGlass.setAlpha(alpha);
        }
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        if (incomingGlass != null) {
            incomingGlass.setColorFilter(colorFilter);
        }
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
