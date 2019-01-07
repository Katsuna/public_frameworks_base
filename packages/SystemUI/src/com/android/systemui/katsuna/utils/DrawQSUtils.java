package com.android.systemui.katsuna.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.android.systemui.R;
import com.katsuna.commons.entities.ColorProfile;
import com.katsuna.commons.entities.ColorProfileKeyV2;
import com.katsuna.commons.entities.UserProfile;
import com.katsuna.commons.utils.ColorCalcV2;
import com.katsuna.commons.utils.DrawUtils;
import com.katsuna.commons.utils.ToggleButtonAdjuster;

public class DrawQSUtils {


    public static Drawable createMinifiedToggleBg(Context context, UserProfile profile,
                                                  boolean leftCorners, boolean rightCorners) {
        StateListDrawable out = new StateListDrawable();

        int bgColor;
        int lineColor;
        if (profile.colorProfile == ColorProfile.CONTRAST) {
            bgColor = ContextCompat.getColor(context, R.color.common_black);
            lineColor = ContextCompat.getColor(context, R.color.common_white);
        } else {
            bgColor = ColorCalcV2.getColor(context, ColorProfileKeyV2.PRIMARY_COLOR_1,
                    profile.colorProfile);
            lineColor = ColorCalcV2.getColor(context, ColorProfileKeyV2.PRIMARY_COLOR_2,
                    profile.colorProfile);
        }
        Drawable onDrawable = getMinifiedToggleBackground(context, bgColor, lineColor, leftCorners,
            rightCorners);

        bgColor = ContextCompat.getColor(context, R.color.common_white);
        lineColor = ContextCompat.getColor(context, R.color.common_grey300);
        Drawable offDrawable = getMinifiedToggleBackground(context, bgColor, lineColor, leftCorners,
            rightCorners);

        out.addState(new int[]{android.R.attr.state_checked}, onDrawable);
        out.addState(new int[]{-android.R.attr.state_checked}, offDrawable);
        return out;
    }

    private static Drawable getMinifiedToggleBackground(Context context, int bgColor, int lineColor,
                                                        boolean leftCorners, boolean rightCorners) {
        Resources res = context.getResources();

        // create bg drawable
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        int cornerRadius = res.getDimensionPixelSize(R.dimen.common_8dp);
        int leftCornerRadius = 0;
        int rightCornerRadius = 0;
        if (leftCorners) {
            leftCornerRadius = cornerRadius;
        }
        if (rightCorners) {
            rightCornerRadius = cornerRadius;
        }

        float[] corners = new float[] {leftCornerRadius, leftCornerRadius,
            rightCornerRadius, rightCornerRadius,
            rightCornerRadius, rightCornerRadius,
            leftCornerRadius, leftCornerRadius};

        bg.setCornerRadii(corners);

        int strokeWidth = res.getDimensionPixelSize(R.dimen.common_1dp);
        int strokeColor = res.getColor(R.color.common_grey100);
        bg.setStroke(strokeWidth, strokeColor);


        // set bg
        bg.setColor(bgColor);

        // create bg drawable
        GradientDrawable line = new GradientDrawable();
        line.setShape(GradientDrawable.RECTANGLE);
        line.setColor(lineColor);

        // set size
        int width = res.getDimensionPixelSize(R.dimen.common_toggle_indicator_width);
        int height = res.getDimensionPixelSize(R.dimen.common_toggle_indicator_height);
        line.setSize(width, height);

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{bg, line});
        layerDrawable.setLayerGravity(1, Gravity.CENTER | Gravity.BOTTOM);

        int bottomPadding = res.getDimensionPixelSize(R.dimen.common_8dp);
        layerDrawable.setLayerInsetBottom(1, bottomPadding);

        return layerDrawable;
    }

    public static Drawable createExpandDrawable(Context context, UserProfile profile, boolean expand) {
        // create bg drawable
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);

        int bgColor = ColorCalcV2.getColor(context, ColorProfileKeyV2.PRIMARY_COLOR_1,
                profile.colorProfile);
        bg.setColor(bgColor);

        // choose icon
        Drawable icon;
        if (expand) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_expand_more_24dp);
        } else {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_expand_less_24dp);
        }

        int white = ContextCompat.getColor(context, R.color.common_white);
        int black54 = ContextCompat.getColor(context, R.color.common_black54);
        if (profile.colorProfile == ColorProfile.CONTRAST) {
            DrawUtils.setColor(icon, white);
        } else {
            DrawUtils.setColor(icon, black54);
        }

        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{bg, icon});
        layerDrawable.setLayerGravity(1, Gravity.CENTER);

        int padding = context.getResources().getDimensionPixelSize(R.dimen.common_4dp);
        layerDrawable.setLayerInset(1, padding, padding, padding, padding);

        return layerDrawable;
    }

    public static void adjustMinifiedToggleButton(Context context, ToggleButton toggleButton,
                                                  int iconResId, Drawable background, UserProfile profile) {
        toggleButton.setBackground(clone(background));
        ToggleButtonAdjuster.adjustToggleButtonIcon(context, toggleButton, iconResId, profile);
        ToggleButtonAdjuster.adjustToggleButtonText(context, toggleButton, profile);
    }

    private static Drawable clone(Drawable drawable) {
        return drawable.getConstantState().newDrawable();
    }

}
