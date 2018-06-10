package com.android.systemui.katsuna.utils;

/**
 * Created by alkis on 23/3/2017.
 */

public interface ISettingsController {
    int getBrightness();

    void setBrightness(int value);

    int getVolume();

    void setVolume(int value);

    int getMaxVolume();

    boolean isWifiEnabled();

    void setWifiEnabled(boolean enabled);

    boolean isDataEnabled(int subId);

    void setDataEnabled(int subId, boolean enabled);

    boolean isFlightModeEnabled();

    void setFlightMode(boolean enabled);

    boolean isFlashEnabled();

    void setFlashlight(boolean enabled);
}
