package org.telegram.ui.xabarchi;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public class LiquidGlassTabBar extends GlassView {

    public static final int TAB_CHATS = 0;
    public static final int TAB_CONTACTS = 1;
    public static final int TAB_CALLS = 2;
    public static final int TAB_SETTINGS = 3;

    public interface OnTabSelectedListener {
        void onTabSelected(int tabIndex);
    }

    private final LinearLayout row;
    private final TabItemView[] items = new TabItemView[4];
    private final Paint pillFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pillRect = new RectF();

    private final FloatValueHolder pillPosHolder = new FloatValueHolder(0f);
    private final SpringAnimation pillSpring;

    private int selectedIndex = TAB_CHATS;
    private float currentPillPos = 0f;
    private float stretchFactor = 0f;
    private OnTabSelectedListener listener;

    public LiquidGlassTabBar(@NonNull Context context) {
        super(context);
        setCapsule(true);

        pillRimPaint.setStyle(Paint.Style.STROKE);
        pillRimPaint.setStrokeWidth(AndroidUtilities.dpf2(1f));

        row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(5), AndroidUtilities.dp(6), AndroidUtilities.dp(5));
        addView(row, new LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtilities.dp(62)));

        addTab(TAB_CHATS, R.drawable.ic_xabarchi_tab_chats, LocaleController.getString("XabarchiTabChats", R.string.XabarchiTabChats));
        addTab(TAB_CONTACTS, R.drawable.ic_xabarchi_tab_contacts, LocaleController.getString("XabarchiTabContacts", R.string.XabarchiTabContacts));
        addTab(TAB_CALLS, R.drawable.ic_xabarchi_tab_calls, LocaleController.getString("XabarchiTabCalls", R.string.XabarchiTabCalls));
        addTab(TAB_SETTINGS, R.drawable.ic_xabarchi_tab_settings, LocaleController.getString("XabarchiTabSettings", R.string.XabarchiTabSettings));

        pillSpring = new SpringAnimation(pillPosHolder);
        SpringForce force = new SpringForce(0f);
        force.setStiffness(340f);
        force.setDampingRatio(0.68f);
        pillSpring.setSpring(force);
        pillSpring.addUpdateListener((animation, value, velocity) -> {
            currentPillPos = value;
            stretchFactor = Math.min(Math.abs(velocity) / 14f, 0.34f);
            invalidate();
        });

        updateSelectionColors();
    }

    private void addTab(int index, @DrawableRes int iconRes, String label) {
        TabItemView item = new TabItemView(getContext(), iconRes, label);
        item.setOnClickListener(v -> selectTab(index, true));
        items[index] = item;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
        row.addView(item, lp);
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public void refreshLabels() {
        items[TAB_CHATS].setLabel(LocaleController.getString("XabarchiTabChats", R.string.XabarchiTabChats));
        items[TAB_CONTACTS].setLabel(LocaleController.getString("XabarchiTabContacts", R.string.XabarchiTabContacts));
        items[TAB_CALLS].setLabel(LocaleController.getString("XabarchiTabCalls", R.string.XabarchiTabCalls));
        items[TAB_SETTINGS].setLabel(LocaleController.getString("XabarchiTabSettings", R.string.XabarchiTabSettings));
        updateSelectionColors();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void selectTab(int index, boolean notify) {
        if (index < 0 || index >= items.length) {
            return;
        }
        boolean changed = selectedIndex != index;
        selectedIndex = index;
        if (notify) {
            XabarchiMotion.hapticTabSwitch(this);
            XabarchiMotion.bounceTap(items[index]);
        }
        pillSpring.animateToFinalPosition(index);
        updateSelectionColors();
        if (notify && changed && listener != null) {
            listener.onTabSelected(index);
        }
    }

    private void updateSelectionColors() {
        boolean dark = LiquidGlassConfig.isDarkTheme(getContext());
        int active = LiquidGlassConfig.IOS_BLUE;
        int inactive = dark ? 0xB8E6ECFA : 0xA8222B3A;
        for (int i = 0; i < items.length; i++) {
            items[i].setActive(i == selectedIndex, active, inactive);
        }
        invalidate();
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child == row) {
            drawActiveLiquidPill(canvas);
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    private void drawActiveLiquidPill(Canvas canvas) {
        int innerLeft = row.getPaddingLeft();
        int innerWidth = getWidth() - row.getPaddingLeft() - row.getPaddingRight();
        if (innerWidth <= 0) {
            return;
        }
        float tabW = innerWidth / 4f;
        float centerX = innerLeft + (currentPillPos + 0.5f) * tabW;
        float halfW = (tabW * 0.48f) * (1f + stretchFactor);
        float verticalPad = AndroidUtilities.dp(5) + stretchFactor * AndroidUtilities.dp(4);

        pillRect.set(
                Math.max(innerLeft, centerX - halfW),
                verticalPad,
                Math.min(getWidth() - row.getPaddingRight(), centerX + halfW),
                getHeight() - verticalPad
        );

        boolean dark = LiquidGlassConfig.isDarkTheme(getContext());
        pillFillPaint.setShader(new LinearGradient(
                pillRect.left, pillRect.top, pillRect.left, pillRect.bottom,
                dark ? new int[]{0x40FFFFFF, 0x1EFFFFFF} : new int[]{0x99FFFFFF, 0x55E8F3FF},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        pillRimPaint.setColor(dark ? 0x55FFFFFF : 0xAAFFFFFF);

        float r = pillRect.height() * 0.5f;
        canvas.drawRoundRect(pillRect, r, r, pillFillPaint);
        canvas.drawRoundRect(pillRect, r, r, pillRimPaint);
    }

    private static final class TabItemView extends LinearLayout {
        private final ImageView iconView;
        private final TextView labelView;

        TabItemView(Context context, @DrawableRes int iconRes, String label) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);

            iconView = new ImageView(context);
            Drawable d = ContextCompat.getDrawable(context, iconRes);
            if (d != null) {
                iconView.setImageDrawable(d.mutate());
            }
            addView(iconView, new LayoutParams(AndroidUtilities.dp(23), AndroidUtilities.dp(23)));

            labelView = new TextView(context);
            labelView.setText(label);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10.5f);
            labelView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            labelView.setSingleLine(true);
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.topMargin = AndroidUtilities.dp(2);
            addView(labelView, lp);
        }

        void setLabel(String text) {
            labelView.setText(text);
        }

        void setActive(boolean active, int activeColor, int inactiveColor) {
            int c = active ? activeColor : inactiveColor;
            labelView.setTextColor(c);
            iconView.setColorFilter(new PorterDuffColorFilter(c, PorterDuff.Mode.SRC_IN));
        }
    }
}
