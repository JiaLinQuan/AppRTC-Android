package de.lespace.apprtc.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class ServiceDestroyReceiver extends BroadcastReceiver {

    private static final String TAG = "ServiceDestroyReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive...");
        Log.d(TAG, "action:" + intent.getAction());
        Log.d(TAG, "ServeiceDestroy auto start service...");
      //  ServiceManager.startService();
    }
}
