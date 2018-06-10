package com.android.systemui.katsuna.utils;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;

public abstract class SoundBroadcastReceiver extends BroadcastReceiver {

    private final Context mContext;

    public SoundBroadcastReceiver(Context context) {
        mContext = context;
    }

    public void setListening(boolean listening) {
        if (listening) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(AudioManager.VOLUME_CHANGED_ACTION);
            filter.addAction(AudioManager.STREAM_DEVICES_CHANGED_ACTION);
            filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
            filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
            filter.addAction(AudioManager.STREAM_MUTE_CHANGED_ACTION);
            filter.addAction(NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED);
            mContext.registerReceiver(this, filter);
        } else {
            try {
                mContext.unregisterReceiver(this);
            } catch (Exception ex) {

            }

        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onBroadcastReceived();
    }

    public abstract void onBroadcastReceived();

}