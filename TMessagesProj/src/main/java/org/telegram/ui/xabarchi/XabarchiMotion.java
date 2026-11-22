package org.telegram.ui.xabarchi;

import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public final class XabarchiMotion {
    private XabarchiMotion() {
    }

    public static SpringAnimation springTo(View view, DynamicAnimation.ViewProperty property, float target, float stiffness, float damping) {
        SpringAnimation anim = new SpringAnimation(view, property, target);
        SpringForce force = new SpringForce(target);
        force.setStiffness(stiffness);
        force.setDampingRatio(damping);
        anim.setSpring(force);
        anim.start();
        return anim;
    }

    public static void bounceTap(View view) {
        if (view == null) {
            return;
        }
        view.setScaleX(0.92f);
        view.setScaleY(0.92f);
        springTo(view, DynamicAnimation.SCALE_X, 1f, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
        springTo(view, DynamicAnimation.SCALE_Y, 1f, SpringForce.STIFFNESS_MEDIUM, SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY);
    }

    public static void hapticTabSwitch(View view) {
        if (view != null) {
            view.performHapticFeedback(
                    HapticFeedbackConstants.CLOCK_TICK,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            );
        }
    }

    public static void hapticSend(View view) {
        if (view != null) {
            view.performHapticFeedback(
                    HapticFeedbackConstants.CONTEXT_CLICK,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            );
        }
    }
}
