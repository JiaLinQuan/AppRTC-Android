package de.lespace.apprtc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DebugUtils;
import android.util.Log;

/**
 * Created by nico on 03.01.17.
 */

public class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {


        final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        Log.d("isDefaultNetworkActive ", connMgr.isDefaultNetworkActive()?"TRUE":"FALSE");
        connMgr.isDefaultNetworkActive();

        if (wifi.isAvailable() || mobile.isAvailable()) {

            // Do something
            if(wifi.isConnected() || mobile.isConnected()){
                Intent networkOnline = new Intent(QuickstartPreferences.NETWORK_ONLINE);
                networkOnline.setFlags(1);
                LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(networkOnline);
                Log.d("wifi connected", "flag No 1");
            }
            else{
                Intent networkOffline = new Intent(QuickstartPreferences.NETWORK_ONLINE);
                networkOffline.setFlags(0);
                LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(networkOffline);
                Log.d("wifi not connected", "flag No 0");
            }

        }
    }
}
