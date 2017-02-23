package de.lespace.apprtc;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import de.lespace.apprtc.service.SignalingService;

public class WakeFulReceiver extends WakefulBroadcastReceiver {
    public WakeFulReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // Start the service, keeping the device awake while the service is
        // launching. This is the Intent to deliver to the service.
        Intent service = new Intent(context, SignalingService.class);
        startWakefulService(context, service);
    }
}
