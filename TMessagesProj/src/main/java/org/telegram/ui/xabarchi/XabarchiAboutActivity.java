package org.telegram.ui.xabarchi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

public class XabarchiAboutActivity extends BaseFragment {

    private static final String UPSTREAM_URL = "https://github.com/DrKLO/Telegram";
    private static final String GPL_URL = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html";

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_xabarchi_back_chevron);
        actionBar.setTitle(LocaleController.getString("XabarchiAboutTitle", R.string.XabarchiAboutTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.setBackground(new LiquidGlassWallpaperDrawable(context));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);
        container.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(40));
        scrollView.addView(container);

        GlassView heroCard = new GlassView(context);
        heroCard.setCornerRadiusDp(26f);
        LinearLayout heroInner = new LinearLayout(context);
        heroInner.setOrientation(LinearLayout.VERTICAL);
        heroInner.setGravity(Gravity.CENTER_HORIZONTAL);
        heroInner.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(28), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        heroCard.addView(heroInner);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.mipmap.ic_launcher);
        heroInner.addView(icon, new LinearLayout.LayoutParams(AndroidUtilities.dp(76), AndroidUtilities.dp(76)));

        TextView appTitle = new TextView(context);
        appTitle.setText(LocaleController.getString("XabarchiAppName", R.string.XabarchiAppName));
        appTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        appTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        appTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.topMargin = AndroidUtilities.dp(14);
        heroInner.addView(appTitle, titleLp);

        TextView versionText = new TextView(context);
        versionText.setText("v" + BuildVars.BUILD_VERSION_STRING + " \u2022 Liquid Glass");
        versionText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        versionText.setTextColor(LiquidGlassConfig.IOS_BLUE);
        heroInner.addView(versionText);

        TextView body = new TextView(context);
        body.setText(LocaleController.getString("XabarchiAboutBody", R.string.XabarchiAboutBody));
        body.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        body.setGravity(Gravity.CENTER);
        body.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = AndroidUtilities.dp(16);
        heroInner.addView(body, bodyLp);

        container.addView(heroCard, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        addLinkCard(context, container,
                LocaleController.getString("XabarchiSourceCode", R.string.XabarchiSourceCode),
                UPSTREAM_URL);
        addLinkCard(context, container,
                LocaleController.getString("XabarchiLicense", R.string.XabarchiLicense),
                GPL_URL);

        fragmentView = scrollView;
        return fragmentView;
    }

    private void addLinkCard(Context context, LinearLayout parent, String title, String url) {
        GlassView card = new GlassView(context);
        card.setCornerRadiusDp(20f);
        TextView tv = new TextView(context);
        tv.setText(title + "\n" + url);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        tv.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(16));
        card.addView(tv);
        card.setOnClickListener(v -> {
            XabarchiMotion.bounceTap(v);
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (Throwable ignored) {
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = AndroidUtilities.dp(14);
        parent.addView(card, lp);
    }
}
