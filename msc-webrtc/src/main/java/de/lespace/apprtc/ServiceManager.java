package de.lespace.apprtc;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ServiceManager extends Service {

    static Context context = null;
    public ServiceManager() {
        context = getApplicationContext();
    }
    private static final String TAG = "ServiceManager";

    public static void startService() {
        Intent intent = new Intent(SignalingService.class.getName());
        Log.i(TAG, "startService called");
        context.startService(intent);
    }
    @Override
    public IBinder onBind(Intent intent) {
        //// TODO: Return the communication channel to the service.
       return null; // throw new UnsupportedOperationException("Not yet implemented");
    }
}
