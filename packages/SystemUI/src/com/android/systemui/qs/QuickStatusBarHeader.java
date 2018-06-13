/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextClock;
import android.widget.ToggleButton;

import com.android.settingslib.Utils;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.R.id;
import com.android.systemui.katsuna.utils.DrawQSUtils;
import com.android.systemui.katsuna.utils.SettingsController;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.QSDetail.Callback;
import com.android.systemui.statusbar.SignalClusterView;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;
import com.katsuna.commons.entities.UserProfile;
import com.katsuna.commons.utils.ProfileReader;


public class QuickStatusBarHeader extends RelativeLayout {

    private static final String TAG = QuickStatusBarHeader.class.getSimpleName();
    private static final float EXPAND_INDICATOR_THRESHOLD = .93f;
    private ActivityStarter mActivityStarter;

    private QSPanel mQsPanel;

    private boolean mExpanded;
    private boolean mListening;

    //protected QuickQSPanel mHeaderQsPanel;
    protected QSTileHost mHost;

    protected View mKatsunaQuickPanel;
    private SettingsController mSettingsController;
    private ToggleButton mWifiToggle;
    private ToggleButton mCellularToggle;
    private ToggleButton mBluetoothToggle;
    private boolean mWifiToggleInProgress;
    private boolean mCellularToggleInProgress;
    private boolean mBluetoothToggleInProgress;
    private ImageView mExpandIndicator;
    private TextClock mKatsunaDate;


    public QuickStatusBarHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        mSettingsController = new SettingsController(getContext());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();

        mKatsunaQuickPanel = findViewById(R.id.katsuna_quick_panel);
        //mHeaderQsPanel = findViewById(R.id.quick_qs_panel);

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view

        updateResources();

        // Set the light/dark theming on the header status UI to match the current theme.
        int colorForeground = Utils.getColorAttr(getContext(), android.R.attr.colorForeground);
        float intensity = colorForeground == Color.WHITE ? 0 : 1;
        Rect tintArea = new Rect(0, 0, 0, 0);

        applyDarkness(R.id.battery, tintArea, intensity, colorForeground);
        applyDarkness(R.id.clock, tintArea, intensity, colorForeground);

        mActivityStarter = Dependency.get(ActivityStarter.class);
        setupControls();
        mExpandIndicator = findViewById(R.id.expand_indicator);
        mExpandIndicator.setOnClickListener(v -> {
            if (mHost != null) {
                if (mExpanded) {
                    mHost.collapsePanels();
                } else {
                    mHost.openPanels();
                }
            }
        });
        mKatsunaDate = findViewById(R.id.katsuna_date);
    }

    private void applyDarkness(int id, Rect tintArea, float intensity, int color) {
        View v = findViewById(id);
        if (v instanceof DarkReceiver) {
            ((DarkReceiver) v).onDarkChanged(tintArea, intensity, color);
        }
    }

    private void setupControls() {
        // setup wifi
        mWifiToggle = findViewById(R.id.wifi_minified_toggle);
        mWifiToggle.setOnClickListener(v -> {

            if (mWifiToggleInProgress) return;

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    mWifiToggleInProgress = true;
                }

                @Override
                protected Void doInBackground(Void... params) {
                    mSettingsController.setWifiEnabled(mWifiToggle.isChecked());
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    mWifiToggleInProgress = false;
                }
            }.execute();

        });

        // setup data
        mCellularToggle = findViewById(R.id.cellular_minified_toggle);
        mCellularToggle.setOnClickListener(v -> {

            if (mCellularToggleInProgress) return;

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    mCellularToggleInProgress = true;
                }

                @Override
                protected Void doInBackground(Void... params) {
                    mSettingsController.setDataEnabled(SettingsController.SINGLE_SIM_DEFAULT_SUB_ID,
                            mCellularToggle.isChecked());
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    mCellularToggleInProgress = false;
                }
            }.execute();
        });

        // setup bluetooth
        mBluetoothToggle = findViewById(R.id.bluetooth_minified_toggle);
        mBluetoothToggle.setOnClickListener(v -> {

            if (mBluetoothToggleInProgress) return;

            new AsyncTask<Void, Void, Void>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    mBluetoothToggleInProgress = true;
                }

                @Override
                protected Void doInBackground(Void... params) {
                    BluetoothController bluetoothController = Dependency.get(BluetoothController.class);
                    if (bluetoothController != null) {
                        bluetoothController.setBluetoothEnabled(mBluetoothToggle.isChecked());
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    mBluetoothToggleInProgress = false;
                }
            }.execute();

        });
    }

    private void readSettings() {
        mWifiToggle.setChecked(mSettingsController.isWifiEnabled());
        mCellularToggle.setChecked(mSettingsController.isDataEnabled(
                SettingsController.SINGLE_SIM_DEFAULT_SUB_ID));
        //read bluetooth
        BluetoothController bluetoothController = Dependency.get(BluetoothController.class);
        if (bluetoothController != null) {
            mBluetoothToggle.setChecked(bluetoothController.isBluetoothEnabled());
        }
    }


    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateResources();
    }

    private void updateResources() {
    }

    public int getCollapsedHeight() {
        return getHeight();
    }

    public int getExpandedHeight() {
        return getHeight();
    }

    public void setExpanded(boolean expanded) {
        //Log.e(TAG, "setExpanded" + expanded);
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        //mHeaderQsPanel.setExpanded(expanded);

        if (expanded) {
            mKatsunaQuickPanel.setVisibility(View.GONE);
            mKatsunaDate.setVisibility(View.VISIBLE);
        } else {
            mKatsunaQuickPanel.setVisibility(View.VISIBLE);
            mKatsunaDate.setVisibility(View.GONE);
        }

        adjustProfile();

        updateEverything();
    }

    private UserProfile mUserProfile;

    private void adjustProfile() {
        //Log.e(TAG, "adjustProfile");
        Context context = getContext();
        mUserProfile = ProfileReader.getUserProfileFromKatsunaServices(context);

        if (mUserProfile == null)  {
            Log.w(TAG, "profile not found");
            return;
        }

        if (mUserProfile.isRightHanded) {
            alignParentStart(mKatsunaQuickPanel);
            setMarginEnd(mKatsunaQuickPanel, R.dimen.katsuna_qs_mini_margin);
            alignParentStart(mKatsunaDate);
            alignParentEnd(mExpandIndicator);
        } else {
            alignParentEnd(mKatsunaQuickPanel);
            setMarginStart(mKatsunaQuickPanel, R.dimen.katsuna_qs_mini_margin);
            alignParentEnd(mKatsunaDate);
            alignParentStart(mExpandIndicator);
        }

        Drawable toggleBg = DrawQSUtils.createMinifiedToggleBg(context, mUserProfile);

        DrawQSUtils.adjustMinifiedToggleButton(context, mWifiToggle, R.drawable.ic_wifi_black_28dp,
                toggleBg, mUserProfile);
        DrawQSUtils.adjustMinifiedToggleButton(context, mCellularToggle,
                R.drawable.ic_signal_cellular_4_bar_28dp, toggleBg, mUserProfile);
        DrawQSUtils.adjustMinifiedToggleButton(context, mBluetoothToggle, R.drawable.ic_bluetooth_28dp,
                toggleBg, mUserProfile);

        mExpandDrawable = DrawQSUtils.createExpandDrawable(context, mUserProfile, true);
        mCollapseDrawable = DrawQSUtils.createExpandDrawable(context, mUserProfile, false);
    }

    private void setMarginStart(View view, int dimenId) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
        int margin = getContext().getResources().getDimensionPixelSize(dimenId);
        params.setMarginStart(margin);
        params.setMarginEnd(0);
        view.setLayoutParams(params);
    }

    private void setMarginEnd(View view, int dimenId) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
        int margin = getContext().getResources().getDimensionPixelSize(dimenId);
        params.setMarginStart(0);
        params.setMarginEnd(margin);
        view.setLayoutParams(params);
    }

    private void alignParentStart(View view) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
        params.removeRule(RelativeLayout.ALIGN_PARENT_END);
        params.addRule(RelativeLayout.ALIGN_PARENT_START);
        view.setLayoutParams(params);
    }

    private void alignParentEnd(View view) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)view.getLayoutParams();
        params.removeRule(RelativeLayout.ALIGN_PARENT_START);
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        view.setLayoutParams(params);
    }

    private Drawable mExpandDrawable;
    private Drawable mCollapseDrawable;

    public void setExpansion(float headerExpansionFraction) {
        //Log.e(TAG, "setExpansion" + headerExpansionFraction);

        if (mUserProfile == null) {
            adjustProfile();
        }

        // refresh profile
        if (headerExpansionFraction == 0 && mWifiToggle.isShown()) {
            //Log.e(TAG, "setExpansion adjust profile needed");
            adjustProfile();
        }

        boolean expanded = headerExpansionFraction > EXPAND_INDICATOR_THRESHOLD;
        if (expanded) {
            mExpandIndicator.setImageDrawable(mCollapseDrawable);
        } else {
            mExpandIndicator.setImageDrawable(mExpandDrawable);
        }
    }

    @Override
    @VisibleForTesting
    public void onDetachedFromWindow() {
        setListening(false);
        super.onDetachedFromWindow();
    }

    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        //mHeaderQsPanel.setListening(listening);
        mListening = listening;
    }

    public void updateEverything() {
        post(() -> {
            setClickable(false);
            readSettings();
        });
    }

    public void setQSPanel(final QSPanel qsPanel) {
        mQsPanel = qsPanel;
        setupHost(qsPanel.getHost());
    }

    public void setupHost(final QSTileHost host) {
        mHost = host;
        //host.setHeaderView(mExpandIndicator);
        //mHeaderQsPanel.setQSPanelAndHeader(mQsPanel, this);
        //mHeaderQsPanel.setHost(host, null /* No customization in header */);
    }

    public void setCallback(Callback qsPanelCallback) {
        //mHeaderQsPanel.setCallback(qsPanelCallback);
    }
}
