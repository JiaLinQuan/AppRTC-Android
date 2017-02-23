package de.lespace.apprtc.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

import java.util.ArrayList;

import de.lespace.apprtc.AppRTCClient;
import de.lespace.apprtc.R;
import de.lespace.apprtc.RTCConnection;
import de.lespace.apprtc.WebSocketChannelClient;
import de.lespace.apprtc.WebSocketRTCClient;
import de.lespace.apprtc.activity.ConnectActivity;
import de.lespace.apprtc.activity.IncomingCall;
import de.lespace.apprtc.thrift.ThriftEntwicklungServerRegistrationAsyncTask;
import de.lespace.apprtc.util.LooperExecutor;

import static de.lespace.apprtc.RTCConnection.r;

public class SignalingService extends Service  implements WebSocketChannelClient.WebSocketChannelEvents, AppRTCClient.SignalingEvents {

    private static final String TAG = "SignalingService";

    public SignalingService() {
        Log.i(TAG, "SignalingService started");

    }

    public static AppRTCClient appRTCClient;
    private LooperExecutor executor;

    String from;
    String wssUrl;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "SignalingService starten: with " + intent.getStringExtra(RTCConnection.EXTRA_MSC_REGISTRATION));

        if (intent.getStringExtra(RTCConnection.EXTRA_FROM) != null)
            from = intent.getStringExtra(RTCConnection.EXTRA_FROM);

       /* if(intent.getStringExtra(RTCConnection.EXTRA_)!=null)
            wssUrl = intent.getStringExtra(RTCConnection.EXTRA_FROM);*/

        //during development only! register current userId on testThriftServer
        //not necessary after integration into msc system
        if (intent.getStringExtra(RTCConnection.EXTRA_MSC_REGISTRATION) != null) {
            String token = FirebaseInstanceId.getInstance().getToken();
            Log.i(TAG, "TOKEN:" + token);

            ThriftEntwicklungServerRegistrationAsyncTask th = new ThriftEntwicklungServerRegistrationAsyncTask();
            th.userId = from;
            th.firebaseToken = token;
            th.service = this;
            th.execute();
        }

        /**
         * Preparation for a call receiving generated fromUUID and
         * registering this name on WebRTC-Signaling-Server
         */
        if (intent.getStringExtra(RTCConnection.EXTRA_SIGNALING_REGISTRATION) != null) {
            PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
            final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

            final String keyprefFrom = getString(R.string.pref_from_key);
            final String keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);

            Log.i(TAG, "starting WebsocketSignaling Service with UUID:"+from);

            executor = new LooperExecutor();
            appRTCClient = new WebSocketRTCClient(this, this, executor);

            AppRTCClient.SignalingEvents signalingEvents = this;

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    if (from == null)
                        from = sharedPref.getString(keyprefFrom, getString(R.string.pref_from_default));
                    else
                        editor.putString(keyprefFrom, from);
                    if (wssUrl == null)
                        wssUrl = sharedPref.getString(keyprefRoomServerUrl, getString(R.string.pref_room_server_url_default));
                    else
                        editor.putString(keyprefRoomServerUrl, wssUrl);
                    editor.commit();

                    Log.i(TAG, "starting SignalingService and registering:" + from + " connecting to url: " + wssUrl);

                    appRTCClient.connectToWebsocket(wssUrl, from);
                }
            });

        }
        return (START_NOT_STICKY);
        //  sendNotification("signaling running now..."); //the foreground service with le space icon
    }

    @Override
    public void onDestroy() {
      /*  Intent in = new Intent();
        in.setAction("YouWillNeverKillMe");
        sendBroadcast(in);
        Log.d(TAG, "onDestroy()...");*/
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
                    Log.d(TAG, "json message from server isQueing()"+appRTCClient.isQueuing()+ "size:"+appRTCClient.getSignalingQueue().size());
                    JSONObject json = new JSONObject(msg);
                    appRTCClient.getSignalingQueue().add(json);

                    if(!appRTCClient.isQueuing()){
                        Log.d(TAG, "processing...");
                        appRTCClient.processSignalingQueue();
                    }
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

    @Override
    public void onChannelError(String description) {

    }

    @Override
    public void onUserListUpdate(final String response) {

                try {
                    JSONArray mJSONArray = new JSONArray(response);
                    RTCConnection.userList = new ArrayList();

                   //     RTCConnection.adapter = new ArrayAdapter(mJSONArray);
                  //  RTCConnection.adapter.clear();
                  //  RTCConnection.adapter.notifyDataSetChanged();

                    for(int i = 0; i < mJSONArray.length();i++){
                        String username = mJSONArray.getString(i);
                        if (username.length() > 0
                                && !RTCConnection.userList.contains(username)
                                && !username.equals(RTCConnection.from)) {
                 //           RTCConnection.userList.add(username);
                 //           RTCConnection.adapter.add(username);
                        }
                    }

               //     RTCConnection.adapter.notifyDataSetChanged();
                }catch (JSONException e) {
                    e.printStackTrace();
                }
    }

    @Override
    public void onIncomingCall(final String from, final boolean screensharing) {

        RTCConnection.to = from;
        RTCConnection.initiator = false;

        if(screensharing){ //if its screensharing jsut re-connect without asking user!
            RTCConnection.callActive=true;

            Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
            intent.putExtra(RTCConnection.EXTRA_TO,  RTCConnection.to);
            intent.putExtra(RTCConnection.EXTRA_INITIATOR,  RTCConnection.initiator);
            startActivity(intent);
        }
        else{

            Intent intent = new Intent(this, IncomingCall.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }

    }

    @Override
    public void onIncomingScreenCall(JSONObject from) {
/*
        disabled here.
        RTCConnection.peerConnectionClient2 = PeerConnectionClient.getInstance(true);
        RTCConnection.peerConnectionClient2.createPeerConnectionFactoryScreen(this);
        RTCConnection.peerConnectionClient2.createPeerConnectionScreen(RTCConnection.peerConnectionClient.getRenderEGLContext(),
                RTCConnection.peerConnectionClient.getScreenRender());
        // Create offer. Offer SDP will be sent to answering client in
        // PeerConnectionEvents.onLocalDescription event.
        RTCConnection.peerConnectionClient2.createOffer();*/

    }


    @Override
    public void onStartCommunication(final SessionDescription sdp) {
        RTCConnection.peerConnectionClient.setRemoteDescription(sdp);
    }


    @Override
    public void onStartScreenCommunication(final SessionDescription sdp) {

        if (RTCConnection.peerConnectionClient2 == null) {
            Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
            return;
        }
        RTCConnection.peerConnectionClient2.setRemoteDescription(sdp);

    }

    @Override
    public void onRemoteDescription(final SessionDescription sdp) {

                if (RTCConnection.peerConnectionClient == null) {
                    Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                    return;
                }

                RTCConnection.peerConnectionClient.setRemoteDescription(sdp);
    }

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
                if (RTCConnection.peerConnectionClient == null) {
                    Log.e(TAG,
                            "Received ICE candidate for non-initilized peer connection.");
                    return;
                }
            RTCConnection.peerConnectionClient.addRemoteIceCandidate(candidate);
    }

    @Override
    public void onRemoteScreenDescription(final SessionDescription sdp) {
        if (RTCConnection.peerConnectionClient2 == null) {
            Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
            return;
        }

        RTCConnection.peerConnectionClient2.setRemoteDescription(sdp);

    }

    @Override
    public void onRemoteScreenIceCandidate(final IceCandidate candidate) {

                if (RTCConnection.peerConnectionClient2 == null) {
                    Log.e(TAG,
                            "Received ICE candidate for non-initilized peer connection.");
                    return;
                }
                RTCConnection.peerConnectionClient2.addRemoteIceCandidate(candidate);

    }

    @Override
    public void onPing() { //A stop was received by the peer - now answering please send new call (e.g. with screensharing)
        SignalingService.appRTCClient.sendPong();
    }

    @Override
    public void onCallback() { //A stop was received by the peer - now answering please send new call (e.g. with screensharing)
        SignalingService.appRTCClient.sendCallback();
    }

    @Override
    public void onChannelClose() {
        Intent intent = new Intent("finish_CallActivity");
        sendBroadcast(intent);
    }

    @Override
    public void onChannelScreenClose() {
        Intent intent = new Intent("finish_screensharing");
        sendBroadcast(intent);
    }
}
