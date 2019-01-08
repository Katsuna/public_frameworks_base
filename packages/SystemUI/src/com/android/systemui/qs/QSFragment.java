/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.R.id;
import com.android.systemui.katsuna.utils.SettingsController;
import com.android.systemui.katsuna.utils.SoundBroadcastReceiver;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.qs.customize.QSCustomizer;
import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;
import com.android.systemui.statusbar.policy.BluetoothController;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.statusbar.stack.StackStateAnimator;
import com.katsuna.commons.entities.ColorProfileKeyV2;
import com.katsuna.commons.entities.UserProfile;
import com.katsuna.commons.utils.BackgroundGenerator;
import com.katsuna.commons.utils.ColorCalcV2;
import com.katsuna.commons.utils.DrawUtils;
import com.katsuna.commons.utils.ProfileReader;
import com.katsuna.commons.utils.SeekBarUtils;
import com.katsuna.commons.utils.ToggleButtonAdjuster;

public class QSFragment extends Fragment implements QS {
    private static final String TAG = "QS";
    private static final boolean DEBUG = false;
    private static final String EXTRA_EXPANDED = "expanded";
    private static final String EXTRA_LISTENING = "listening";

    private final Rect mQsBounds = new Rect();
    private boolean mQsExpanded;
    private boolean mHeaderAnimating;
    private boolean mKeyguardShowing;
    private boolean mStackScrollerOverscrolling;

    private long mDelay;

    private QSAnimator mQSAnimator;
    private HeightListener mPanelView;
    protected QuickStatusBarHeader mHeader;
    private QSCustomizer mQSCustomizer;
    //protected QSPanel mQSPanel;
    protected View mKatsunaQSPanel;
    private QSDetail mQSDetail;
    private boolean mListening;
    private QSContainerImpl mContainer;
    private int mLayoutDirection;
    private QSFooter mFooter;
    private LinearLayout mKatsunaSettingsContainer;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        inflater =inflater.cloneInContext(new ContextThemeWrapper(getContext(), R.style.qs_theme));
        return inflater.inflate(R.layout.qs_panel, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //mQSPanel = view.findViewById(R.id.quick_settings_panel);
        mKatsunaQSPanel = view.findViewById(R.id.quick_settings_panel);
        mQSDetail = view.findViewById(R.id.qs_detail);
        mHeader = view.findViewById(R.id.header);
        //mFooter = view.findViewById(R.id.qs_footer);
        mContainer = view.findViewById(id.quick_settings_container);

        //mQSDetail.setQsPanel(mQSPanel, mHeader, (View) mFooter);
/*        mQSAnimator = new QSAnimator(this,
                mHeader.findViewById(R.id.quick_qs_panel), mQSPanel);*/

        mQSCustomizer = view.findViewById(R.id.qs_customize);
        mQSCustomizer.setQs(this);
        if (savedInstanceState != null) {
            setExpanded(savedInstanceState.getBoolean(EXTRA_EXPANDED));
            setListening(savedInstanceState.getBoolean(EXTRA_LISTENING));
            int[] loc = new int[2];
            View edit = view.findViewById(android.R.id.edit);
            edit.getLocationInWindow(loc);
            int x = loc[0] + edit.getWidth() / 2;
            int y = loc[1] + edit.getHeight() / 2;
            mQSCustomizer.setEditLocation(x, y);
            mQSCustomizer.restoreInstanceState(savedInstanceState);
        }


        mSettingsController = new SettingsController(getContext());
        mKatsunaSettingsContainer = mKatsunaQSPanel.findViewById(R.id.katsuna_qs_container);
        setupKatsunaControls();
    }

    private SeekBar mBrigthnessSeekBar;
    private View mBrigthnessLow;
    private View mBrigthnessIconMax;

    private SoundBroadcastReceiver mSoundReceiver;
    private SeekBar mVolumeSeekBar;
    private View mVolumeLow;
    private View mVolumeMax;

    private ToggleButton mWifiSwitch;
    private Button mWifiMore;
    private ActivityStarter mActivityStarter;

    private ToggleButton mCellularToggle;
    private Button mCellucarMore;
    private ToggleButton mBluetoothToggle;
    private Button mBluetoothMore;
    private ToggleButton mFlightToggle;
    private ToggleButton mFlashToggle;
    private ToggleButton mZenToggle;
    private SettingsController mSettingsController;

    private ImageView mWifiIcon;
    private ImageView mCellularIcon;
    private ImageView mBluetoothIcon;
    private ImageView mZenIcon;
    private ImageView mFlashIcon;
    private ImageView mFlightIcon;

    private void setupKatsunaControls() {
        setupBrightnessControl();
        setupVolumeControl();
        setupZenControl();
        setupActivityStarter();
        setupWifiControl();
        setupCellularControl();
        setupBluetoothControl();
        setupFlightControl();
        setupFlashControl();
        setupListeners();
    }

    private void setupListeners() {
        if (mSoundReceiver == null) {
            mSoundReceiver = new SoundBroadcastReceiver(getContext()) {
                @Override
                public void onBroadcastReceived() {
                    readVolume();
                }
            };
        }
    }

    private void setupBrightnessControl() {
        mBrigthnessSeekBar = mKatsunaQSPanel.findViewById(R.id.brightness_seek_bar);
        mBrigthnessSeekBar.setMax(255);
        mBrigthnessSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSettingsController.setBrightness(progress);
            }
        });

        mBrigthnessLow = mKatsunaQSPanel.findViewById(R.id.brightness_label_dark);
        mBrigthnessLow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int newProgress = mBrigthnessSeekBar.getProgress() - 25;
                if (newProgress < 0) {
                    newProgress = 0;
                }
                mBrigthnessSeekBar.setProgress(newProgress, true);
                mSettingsController.setBrightness(newProgress);
            }
        });

        mBrigthnessIconMax = mKatsunaQSPanel.findViewById(R.id.brightness_label_light);
        mBrigthnessIconMax.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int newProgress = mBrigthnessSeekBar.getProgress() + 25;
                if (newProgress > 255) {
                    newProgress = 255;
                }
                mBrigthnessSeekBar.setProgress(newProgress, true);
                mSettingsController.setBrightness(newProgress);
            }
        });

    }

    private boolean mVolumeUnderChange = false;

    private void disableVolumeListeners() {
        mVolumeUnderChange = true;
    }

    private void enableVolumeListeners() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mVolumeUnderChange = false;
            }
        }, 100);
    }

    private void setupVolumeControl() {
        mVolumeSeekBar = mKatsunaQSPanel.findViewById(R.id.volume_seek_bar);
        mVolumeSeekBar.setMax(mSettingsController.getMaxVolume());
        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.progress = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                disableVolumeListeners();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mSettingsController.setVolume(progress);
                mZenToggle.setChecked(false);
                enableVolumeListeners();
            }
        });

        mVolumeLow = mKatsunaQSPanel.findViewById(R.id.common_volume_silent);
        mVolumeLow.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                disableVolumeListeners();
                int newProgress = mVolumeSeekBar.getProgress() - 7;
                mVolumeSeekBar.setProgress(newProgress, true);
                mSettingsController.setVolume(newProgress);
                mZenToggle.setChecked(false);
                enableVolumeListeners();
            }
        });

        mVolumeMax = mKatsunaQSPanel.findViewById(R.id.common_volume_loud);
        mVolumeMax.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                disableVolumeListeners();
                int newProgress = mVolumeSeekBar.getProgress() + 7;
                mVolumeSeekBar.setProgress(newProgress, true);
                mSettingsController.setVolume(newProgress);
                mZenToggle.setChecked(false);
                enableVolumeListeners();
            }
        });
    }

    private void setupZenControl() {
        mZenToggle = mKatsunaQSPanel.findViewById(R.id.zen_toggle);
        mZenToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ZenModeController controller = Dependency.get(ZenModeController.class);
            if (isChecked) {
                int zen = Prefs.getInt(getContext(), Prefs.Key.DND_FAVORITE_ZEN,
                        Settings.Global.ZEN_MODE_ALARMS);
                controller.setZen(zen, null, TAG);
            } else {
                controller.setZen(Settings.Global.ZEN_MODE_OFF, null, TAG);
            }
        });

        mZenIcon = mKatsunaQSPanel.findViewById(R.id.zen_icon);
    }

    private void setupActivityStarter() {
        mActivityStarter = Dependency.get(ActivityStarter.class);
    }

    private void setupWifiControl() {
        mWifiSwitch = mKatsunaQSPanel.findViewById(R.id.wifi_toggle);
        mWifiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mSettingsController.setWifiEnabled(isChecked);
        });
        mWifiMore = mKatsunaQSPanel.findViewById(R.id.wifi_more);
        mWifiMore.setOnClickListener(v -> {
            if (mActivityStarter != null) {
                Intent i = new Intent(Settings.ACTION_WIFI_SETTINGS);
                mActivityStarter.startActivity(i, true /* dismissShade */);
            }
        });

        mWifiIcon = mKatsunaQSPanel.findViewById(R.id.wifi_icon);
    }

    private void setupCellularControl() {
        mCellularToggle = mKatsunaQSPanel.findViewById(R.id.cellular_toggle);
        mCellularToggle.setOnCheckedChangeListener((buttonView, isChecked) ->
                setMobileDataEnabled(isChecked));
        mCellucarMore = mKatsunaQSPanel.findViewById(R.id.cellular_more);
        mCellucarMore.setOnClickListener(v -> {
            if (mActivityStarter != null) {
                Intent i = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                mActivityStarter.startActivity(i, true /* dismissShade */);
            }
        });

        mCellularIcon = mKatsunaQSPanel.findViewById(R.id.cellular_icon);
    }

    private void setMobileDataEnabled(boolean enabled) {
        mSettingsController.setDataEnabled(SettingsController.SINGLE_SIM_DEFAULT_SUB_ID, enabled);
    }

    private void setupBluetoothControl() {
        mBluetoothToggle = mKatsunaQSPanel.findViewById(R.id.bluetooth_toggle);
        mBluetoothToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (getHost() != null) {
                BluetoothController bluetoothController = Dependency.get(BluetoothController.class);
                if (bluetoothController != null) {
                    bluetoothController.setBluetoothEnabled(isChecked);
                }
            }
        });

        mBluetoothMore = mKatsunaQSPanel.findViewById(R.id.bluetooth_more);
        mBluetoothMore.setOnClickListener(v -> {
            if (mActivityStarter != null) {
                Intent i = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);

                mActivityStarter.startActivity(i, true /* dismissShade */);
            }
        });

        mBluetoothIcon = mKatsunaQSPanel.findViewById(R.id.bluetooth_icon);
    }

    private void setupFlightControl() {
        mFlightToggle = mKatsunaQSPanel.findViewById(R.id.flight_toggle);
        mFlightToggle.setOnCheckedChangeListener((buttonView, isChecked) -> setFlightModeEnabled(isChecked));
        mFlightIcon = mKatsunaQSPanel.findViewById(R.id.flight_icon);
    }

    private void setFlightModeEnabled(boolean enabled) {
        mSettingsController.setFlightMode(enabled);
        if (enabled) {
            mWifiSwitch.setChecked(false);
            mCellularToggle.setChecked(false);
        }
    }

    private void setupFlashControl() {
        mFlashToggle = mKatsunaQSPanel.findViewById(R.id.flash_toggle);
        mFlashToggle.setOnCheckedChangeListener((buttonView, isChecked) -> setFlashEnabled(isChecked));
        mFlashIcon = mKatsunaQSPanel.findViewById(R.id.flash_icon);
    }

    private void setFlashEnabled(boolean enabled) {
        mSettingsController.setFlashlight(enabled);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mListening) {
            setListening(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_EXPANDED, mQsExpanded);
        outState.putBoolean(EXTRA_LISTENING, mListening);
        mQSCustomizer.saveInstanceState(outState);
    }

    @VisibleForTesting
    boolean isListening() {
        return mListening;
    }

    @VisibleForTesting
    boolean isExpanded() {
        return mQsExpanded;
    }

    @Override
    public View getHeader() {
        return mHeader;
    }

    @Override
    public void setHasNotifications(boolean hasNotifications) {
    }

    @Override
    public void setPanelView(HeightListener panelView) {
        mPanelView = panelView;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.getLayoutDirection() != mLayoutDirection) {
            mLayoutDirection = newConfig.getLayoutDirection();

            if (mQSAnimator != null) {
                mQSAnimator.onRtlChanged();
            }
        }
    }

    @Override
    public void setContainer(ViewGroup container) {
        if (container instanceof NotificationsQuickSettingsContainer) {
            mQSCustomizer.setContainer((NotificationsQuickSettingsContainer) container);
        }
    }

    @Override
    public boolean isCustomizing() {
        return mQSCustomizer.isCustomizing();
    }

    public void setHost(QSTileHost qsh) {
        //mQSPanel.setHost(qsh, mQSCustomizer);
        mHeader.setupHost(qsh);
        //mFooter.setQSPanel(mQSPanel);
        mQSDetail.setHost(qsh);

        if (mQSAnimator != null) {
            mQSAnimator.setHost(qsh);
        }
    }

    private void readBrightness() {
        mBrigthnessSeekBar.setProgress(mSettingsController.getBrightness());
    }

    private void readVolume() {
        if (mVolumeUnderChange) return;
        mVolumeSeekBar.setProgress(mSettingsController.getVolume());
    }

    private void readWifi() {
        mWifiSwitch.setChecked(mSettingsController.isWifiEnabled());
    }

    private void readCellularData() {
        mCellularToggle.setChecked(mSettingsController.isDataEnabled(
                SettingsController.SINGLE_SIM_DEFAULT_SUB_ID));
    }

    private void readBluetooth() {
        if (getHost() != null) {
            BluetoothController bluetoothController = Dependency.get(BluetoothController.class);
            if (bluetoothController != null) {
                mBluetoothToggle.setChecked(bluetoothController.isBluetoothEnabled());
            }
        }
    }

    private void readFlightMode() {
        mFlightToggle.setChecked(mSettingsController.isFlightModeEnabled());
    }

    private void readFlash() {
        mFlashToggle.setChecked(mSettingsController.isFlashEnabled());
    }

    private void readKatsunaControls() {
        readBrightness();
        readVolume();
        readWifi();
        readCellularData();
        readBluetooth();
        readFlightMode();
        readFlash();
    }

    private void updateQsState() {
        final boolean expandVisually = mQsExpanded || mStackScrollerOverscrolling
                || mHeaderAnimating;
        //mQSPanel.setExpanded(mQsExpanded);

        if (mSoundReceiver != null) {
            mSoundReceiver.setListening(mQsExpanded);
        }
        if (mQsExpanded) {
            readKatsunaControls();
        }

        mQSDetail.setExpanded(mQsExpanded);
        mHeader.setVisibility((mQsExpanded || !mKeyguardShowing || mHeaderAnimating)
                ? View.VISIBLE
                : View.INVISIBLE);
        mHeader.setExpanded((mKeyguardShowing && !mHeaderAnimating)
                || (mQsExpanded && !mStackScrollerOverscrolling));
/*
        mFooter.setVisibility((mQsExpanded || !mKeyguardShowing || mHeaderAnimating)
                ? View.VISIBLE
                : View.INVISIBLE);
        mFooter.setExpanded((mKeyguardShowing && !mHeaderAnimating)
                || (mQsExpanded && !mStackScrollerOverscrolling));
*/

        if (expandVisually) {
            mKatsunaQSPanel.setVisibility(View.VISIBLE);
            adjustProfiles();

        } else {
            mKatsunaQSPanel.setVisibility(View.INVISIBLE);
        }
    }

    UserProfile mCurrentUserProfile;

    private void adjustProfiles() {
        Context ctx = getContext();

        UserProfile userProfile = ProfileReader.getUserProfileFromKatsunaServices(ctx);
        if (userProfile == null) return;

        // check if current profile is the same
        if (userProfile.equals(mCurrentUserProfile)) return;

        // check if hand setting needs adjustment
        if (mCurrentUserProfile == null
                || mCurrentUserProfile.isRightHanded != userProfile.isRightHanded) {
            // check for changed profile
            if (mKatsunaSettingsContainer != null) {
                mKatsunaSettingsContainer.removeAllViews();
                View mKatsunaSettingsView = LayoutInflater.from(getContext()).inflate(
                        userProfile.isRightHanded ? R.layout.katsuna_quick_settings_rh :
                                R.layout.katsuna_quick_settings_lh,
                        mKatsunaSettingsContainer, false);
                mKatsunaSettingsContainer.addView(mKatsunaSettingsView);
                setupKatsunaControls();
                readKatsunaControls();
            }
        }

        mCurrentUserProfile = userProfile;

        SeekBarUtils.adjustSeekbarV3(ctx, mBrigthnessSeekBar, userProfile,
            R.drawable.ic_brightness_7_black_24dp, R.drawable.seekbar_progress);
        SeekBarUtils.adjustSeekbarV3(ctx, mVolumeSeekBar, userProfile,
            R.drawable.ic_volume_up_black_24dp, R.drawable.seekbar_progress);

        int primaryColor2 = ColorCalcV2.getColor(ctx, ColorProfileKeyV2.PRIMARY_COLOR_2,
                userProfile.colorProfile);
        DrawUtils.setColor(mWifiIcon.getDrawable(), primaryColor2);
        DrawUtils.setColor(mCellularIcon.getDrawable(), primaryColor2);
        DrawUtils.setColor(mBluetoothIcon.getDrawable(), primaryColor2);
        DrawUtils.setColor(mZenIcon.getDrawable(), primaryColor2);
        DrawUtils.setColor(mFlashIcon.getDrawable(), primaryColor2);
        DrawUtils.setColor(mFlightIcon.getDrawable(), primaryColor2);

        Drawable toggleBg = BackgroundGenerator.createToggleBgV3(ctx, userProfile, true);
        ToggleButtonAdjuster.adjustToggleButton(ctx, mWifiSwitch, toggleBg, userProfile);
        ToggleButtonAdjuster.adjustToggleButton(ctx, mCellularToggle, toggleBg, userProfile);
        ToggleButtonAdjuster.adjustToggleButton(ctx, mBluetoothToggle, toggleBg, userProfile);
        ToggleButtonAdjuster.adjustToggleButton(ctx, mZenToggle, toggleBg, userProfile);
        ToggleButtonAdjuster.adjustToggleButton(ctx, mFlightToggle, toggleBg, userProfile);
        ToggleButtonAdjuster.adjustToggleButton(ctx, mFlashToggle, toggleBg, userProfile);
    }

    public QSPanel getQsPanel() {
        return null;
    }

    public QSCustomizer getCustomizer() {
        return mQSCustomizer;
    }

    @Override
    public boolean isShowingDetail() {
        return false;
    }

    @Override
    public void setHeaderClickable(boolean clickable) {
        if (DEBUG) Log.d(TAG, "setHeaderClickable " + clickable);

/*        View expandView = mFooter.getExpandView();
        if (expandView != null) {
            expandView.setClickable(clickable);
        }*/
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (DEBUG) Log.d(TAG, "setExpanded " + expanded);
        mQsExpanded = expanded;
        //mQSPanel.setListening(mListening && mQsExpanded);
        updateQsState();
    }

    @Override
    public void setKeyguardShowing(boolean keyguardShowing) {
        if (DEBUG) Log.d(TAG, "setKeyguardShowing " + keyguardShowing);
        mKeyguardShowing = keyguardShowing;

        if (mQSAnimator != null) {
            mQSAnimator.setOnKeyguard(keyguardShowing);
        }

        //mFooter.setKeyguardShowing(keyguardShowing);
        updateQsState();
    }

    @Override
    public void setOverscrolling(boolean stackScrollerOverscrolling) {
        if (DEBUG) Log.d(TAG, "setOverscrolling " + stackScrollerOverscrolling);
        mStackScrollerOverscrolling = stackScrollerOverscrolling;
        updateQsState();
    }

    @Override
    public void setListening(boolean listening) {
        if (DEBUG) Log.d(TAG, "setListening " + listening);
        mListening = listening;
        mHeader.setListening(listening);
        //mFooter.setListening(listening);
        //mQSPanel.setListening(mListening && mQsExpanded);
    }

    @Override
    public void setHeaderListening(boolean listening) {
        mHeader.setListening(listening);
        //mFooter.setListening(listening);
    }

    @Override
    public void setQsExpansion(float expansion, float headerTranslation) {
        if (DEBUG) Log.d(TAG, "setQSExpansion " + expansion + " " + headerTranslation);
        mContainer.setExpansion(expansion);
        final float translationScaleY = expansion - 1;
        if (!mHeaderAnimating) {
            int height = mHeader.getHeight();
            getView().setTranslationY(mKeyguardShowing ? (translationScaleY * height)
                    : headerTranslation);
        }
        mHeader.setExpansion(mKeyguardShowing ? 1 : expansion);
        //mFooter.setExpansion(mKeyguardShowing ? 1 : expansion);
        int heightDiff = mKatsunaQSPanel.getBottom() - mHeader.getBottom() + mHeader.getPaddingBottom();
                //+ mFooter.getHeight();
        mKatsunaQSPanel.setTranslationY(translationScaleY * heightDiff);
        mQSDetail.setFullyExpanded(expansion == 1);

        if (mQSAnimator != null) {
            mQSAnimator.setPosition(expansion);
        }

        // Set bounds on the QS panel so it doesn't run over the header.
        mQsBounds.top = (int) -mKatsunaQSPanel.getTranslationY();
        mQsBounds.right = mKatsunaQSPanel.getWidth();
        mQsBounds.bottom = mKatsunaQSPanel.getHeight();
        mKatsunaQSPanel.setClipBounds(mQsBounds);
    }

    @Override
    public void animateHeaderSlidingIn(long delay) {
        if (DEBUG) Log.d(TAG, "animateHeaderSlidingIn");
        // If the QS is already expanded we don't need to slide in the header as it's already
        // visible.
        if (!mQsExpanded) {
            mHeaderAnimating = true;
            mDelay = delay;
            getView().getViewTreeObserver().addOnPreDrawListener(mStartHeaderSlidingIn);
        }
    }

    @Override
    public void animateHeaderSlidingOut() {
        if (DEBUG) Log.d(TAG, "animateHeaderSlidingOut");
        mHeaderAnimating = true;
        getView().animate().y(-mHeader.getHeight())
                .setStartDelay(0)
                .setDuration(StackStateAnimator.ANIMATION_DURATION_STANDARD)
                .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        getView().animate().setListener(null);
                        mHeaderAnimating = false;
                        updateQsState();
                    }
                })
                .start();
    }

    @Override
    public void setExpandClickListener(OnClickListener onClickListener) {
        /*View expandView = mFooter.getExpandView();

        if (expandView != null) {
            expandView.setOnClickListener(onClickListener);
        }*/
    }

    @Override
    public void closeDetail() {
        //mQSPanel.closeDetail();
    }

    public void notifyCustomizeChanged() {
        // The customize state changed, so our height changed.
        mContainer.updateExpansion();
        //mQSPanel.setVisibility(!mQSCustomizer.isCustomizing() ? View.VISIBLE : View.INVISIBLE);
        mHeader.setVisibility(!mQSCustomizer.isCustomizing() ? View.VISIBLE : View.INVISIBLE);
        //mFooter.setVisibility(!mQSCustomizer.isCustomizing() ? View.VISIBLE : View.INVISIBLE);
        // Let the panel know the position changed and it needs to update where notifications
        // and whatnot are.
        mPanelView.onQsHeightChanged();
    }

    /**
     * The height this view wants to be. This is different from {@link #getMeasuredHeight} such that
     * during closing the detail panel, this already returns the smaller height.
     */
    @Override
    public int getDesiredHeight() {
        if (mQSCustomizer.isCustomizing()) {
            return getView().getHeight();
        }
        if (mQSDetail.isClosingDetail()) {
            LayoutParams layoutParams = (LayoutParams) mKatsunaQSPanel.getLayoutParams();
            int panelHeight = layoutParams.topMargin + layoutParams.bottomMargin +
                    + mKatsunaQSPanel.getMeasuredHeight();
            return panelHeight + getView().getPaddingBottom();
        } else {
            return getView().getMeasuredHeight();
        }
    }

    @Override
    public void setHeightOverride(int desiredHeight) {
        mContainer.setHeightOverride(desiredHeight);
    }

    @Override
    public int getQsMinExpansionHeight() {
        return mHeader.getHeight();
    }

    @Override
    public void hideImmediately() {
        getView().animate().cancel();
        getView().setY(-mHeader.getHeight());
    }

    private final ViewTreeObserver.OnPreDrawListener mStartHeaderSlidingIn
            = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            getView().getViewTreeObserver().removeOnPreDrawListener(this);
            getView().animate()
                    .translationY(0f)
                    .setStartDelay(mDelay)
                    .setDuration(StackStateAnimator.ANIMATION_DURATION_GO_TO_FULL_SHADE)
                    .setInterpolator(Interpolators.FAST_OUT_SLOW_IN)
                    .setListener(mAnimateHeaderSlidingInListener)
                    .start();
            getView().setY(-mHeader.getHeight());
            return true;
        }
    };

    private final Animator.AnimatorListener mAnimateHeaderSlidingInListener
            = new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
            mHeaderAnimating = false;
            updateQsState();
        }
    };
}
