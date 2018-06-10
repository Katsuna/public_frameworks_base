package com.android.systemui.katsuna.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.FlashlightController;

public class SettingsController implements ISettingsController {

    public static final int SINGLE_SIM_DEFAULT_SUB_ID = 1;

    private Context mContext;
    private ContentResolver mContentResolver;
    private AudioManager mAudioManager;
    private WifiManager mWifiManager;
    private TelephonyManager mTelephonyManager;
    private ConnectivityManager mConnectivityManager;
    private FlashlightController mFlashlightController;

    public SettingsController(Context context) {
        mContext = context;
        mContentResolver = mContext.getContentResolver();

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mTelephonyManager = TelephonyManager.from(mContext);
        mConnectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        mFlashlightController = Dependency.get(FlashlightController.class);
    }

    @Override
    public int getBrightness() {
        int output = 0;

        try {
            Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            output = Settings.System.getInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            //Throw an error case it couldn't be retrieved
            Log.e("Error", "Cannot access system brightness");
            e.printStackTrace();
        }

        return output;
    }

    @Override
    public void setBrightness(int value) {
        Settings.System.putInt(mContentResolver, Settings.System.SCREEN_BRIGHTNESS, value);
    }

    @Override
    public int getVolume() {
        return mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
    }

    @Override
    public void setVolume(int value) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING, value, 0);
    }

    @Override
    public int getMaxVolume() {
        return mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
    }

    @Override
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    @Override
    public void setWifiEnabled(boolean enabled) {
        try {
            mWifiManager.setWifiEnabled(enabled);
        } catch (Exception ex) {
            Log.e("XXX", ex.toString());
        }
    }

    @Override
    public boolean isDataEnabled(int subId) {
        return mTelephonyManager.getDataEnabled(subId);
    }

    @Override
    public void setDataEnabled(int subId, boolean enabled) {
        mTelephonyManager.setDataEnabled(subId, enabled);
    }

    @Override
    public boolean isFlightModeEnabled() {
        return Settings.System.getInt(mContentResolver, Settings.System.AIRPLANE_MODE_ON, 0) == 1;
    }

    @Override
    public void setFlightMode(boolean enabled) {
        mConnectivityManager.setAirplaneMode(enabled);
    }

    @Override
    public boolean isFlashEnabled() {
        return mFlashlightController.isEnabled();
    }

    @Override
    public void setFlashlight(boolean enabled) {
        mFlashlightController.setFlashlight(enabled);
    }
}
