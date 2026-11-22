package org.telegram.ui.xabarchi;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.LanguageSelectActivity;

public class XabarchiSettingsActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_xabarchi_back_chevron);
        actionBar.setTitle(LocaleController.getString("XabarchiGlassSettings", R.string.XabarchiGlassSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setBackground(new LiquidGlassWallpaperDrawable(context));

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(100));
        scroll.addView(list);

        addToggleCard(context, list,
                LocaleController.getString("XabarchiSimpleMode", R.string.XabarchiSimpleMode),
                LocaleController.getString("XabarchiSimpleModeDesc", R.string.XabarchiSimpleModeDesc),
                LiquidGlassConfig.isSimpleMode(),
                checked -> {
                    LiquidGlassConfig.setSimpleMode(checked);
                    TiltGlareController.getInstance(context).refreshPreferenceState();
                });

        addToggleCard(context, list,
                LocaleController.getString("XabarchiTiltGlare", R.string.XabarchiTiltGlare),
                LocaleController.getString("XabarchiTiltGlareDesc", R.string.XabarchiTiltGlareDesc),
                LiquidGlassConfig.isTiltGlareEnabled(),
                checked -> {
                    LiquidGlassConfig.setTiltGlareEnabled(checked);
                    TiltGlareController.getInstance(context).refreshPreferenceState();
                });

        addActionCard(context, list,
                LocaleController.getString("Language", R.string.Language),
                () -> presentFragment(new LanguageSelectActivity()));

        addActionCard(context, list,
                LocaleController.getString("XabarchiAboutTitle", R.string.XabarchiAboutTitle),
                () -> presentFragment(new XabarchiAboutActivity()));

        fragmentView = scroll;
        return fragmentView;
    }

    private interface ToggleCallback {
        void onChanged(boolean checked);
    }

    private void addActionCard(Context context, LinearLayout parent, String title, Runnable action) {
        GlassView card = new GlassView(context);
        card.setCornerRadiusDp(20f);
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tv.setTextColor(LiquidGlassConfig.IOS_BLUE);
        tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(20), AndroidUtilities.dp(18));
        card.addView(tv);
        card.setOnClickListener(v -> {
            XabarchiMotion.bounceTap(v);
            action.run();
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = AndroidUtilities.dp(12);
        parent.addView(card, lp);
    }

    private void addToggleCard(Context context, LinearLayout parent, String title, String subtitle, boolean initial, ToggleCallback callback) {
        GlassView card = new GlassView(context);
        card.setCornerRadiusDp(20f);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textCol.addView(titleView);

        TextView subView = new TextView(context);
        subView.setText(subtitle);
        subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        textCol.addView(subView);

        row.addView(textCol, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Switch sw = new Switch(context);
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            XabarchiMotion.hapticTabSwitch(buttonView);
            callback.onChanged(isChecked);
        });
        row.addView(sw);
        card.addView(row);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = AndroidUtilities.dp(12);
        parent.addView(card, lp);
    }
}
