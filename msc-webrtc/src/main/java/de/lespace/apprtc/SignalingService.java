package de.lespace.apprtc;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import de.lespace.apprtc.util.LooperExecutor;

public class SignalingService extends Service  implements WebSocketChannelClient.WebSocketChannelEvents {

    private static final String TAG = "SignalingService";

    public SignalingService() {
        Log.i(TAG, "SignalingService started");
    }

    public static AppRTCClient appRTCClient;
    private LooperExecutor executor;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "SignalingService onStartCommand");
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        final String keyprefFrom = getString(R.string.pref_from_key);
        final String keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);

        Log.i(TAG, "WebsocketService started: ");

        executor = new LooperExecutor();
        appRTCClient = new WebSocketRTCClient(this,executor);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                if(RTCConnection.roomConnectionParameters.from==null){
                    RTCConnection.roomConnectionParameters.from = sharedPref.getString(keyprefFrom, getString(R.string.pref_from_default));
                }

                if(RTCConnection.roomConnectionParameters.wssUrl==null)
                    RTCConnection.roomConnectionParameters.wssUrl = sharedPref.getString(keyprefRoomServerUrl, getString(R.string.pref_room_server_url_default));

                appRTCClient.connectToWebsocket(RTCConnection.roomConnectionParameters);
            }
        });

        sendNotification("signaling running now...");


    return(START_NOT_STICKY);
    }

    @Override
    public void onDestroy() {
        Intent in = new Intent();
        in.setAction("YouWillNeverKillMe");
        sendBroadcast(in);
        Log.d(TAG, "onDestroy()...");
    }

    public void sendNotification(String message){

        Intent i=new Intent(this, ConnectActivity.class);

        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi=PendingIntent.getActivity(this, 0,
                i, 0);

        Notification note= new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setDeleteIntent(pi)
                .build();

        note.flags|=Notification.FLAG_NO_CLEAR;

        startForeground(1337, note);

       /* Intent notificationIntent = new Intent(getApplicationContext(),ConnectActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification  = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build();
        startForeground(1,notification);*/
    }
    @Override
    public IBinder onBind(Intent intent) {

        return(null);
        //throw new UnsupportedOperationException("Not yet implemented");
    }


    // --------------------------------------------------------------------
    // WebSocketChannelEvents interface implementation.
    // All events are called by WebSocketChannelClient on a local looper thread
    // (passed to WebSocket client constructor).
    @Override
    public void onWebSocketMessage(final String msg) {
        executor.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    JSONObject json = new JSONObject(msg);
                    appRTCClient.getSignalingQueue().add(json);

                    if(!appRTCClient.isQueuing() && appRTCClient.getSignalingEvents()!=null) appRTCClient.processSignalingQueue();
                } catch (JSONException e) {
                    reportError("WebSocket message JSON parsing error: " + e.toString());
                }

            }
        });


    }

    @Override
    public void onWebSocketClose() {
        appRTCClient.getSignalingEvents().onChannelClose();
    }

    private void reportError(final String errorMessage) {
        Log.e(TAG, errorMessage);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //if (wsClient.getState() != WebSocketChannelClient.WebSocketConnectionState.ERROR) {
                // wsClient.setState(WebSocketChannelClient.WebSocketConnectionState.ERROR);
                appRTCClient.getSignalingEvents().onChannelError(errorMessage);
                Log.e(TAG, errorMessage);
                //}
            }
        });
    }

    @Override
    public void onWebSocketError(String description) {
        Log.e(TAG, description);
    }


    // Put a |key|->|value| mapping in |json|.
    public static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
