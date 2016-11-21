package de.lespace.apprtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;

import java.util.ArrayList;


public abstract class RTCConnection extends Activity implements
        AppRTCClient.SignalingEvents,
        WebSocketChannelClient.WebSocketChannelEvents {

    public static final String EXTRA_FROM = "de.lespace.mscwebrtc.FROM";
    public static final String EXTRA_TO = "de.lespace.mscwebrtc.TO";
    public static final String EXTRA_LOOPBACK = "de.lespace.mscwebrtc.LOOPBACK";
    public static final String EXTRA_VIDEO_CALL = "de.lespace.mscwebrtc.VIDEO_CALL";
    public static final String EXTRA_VIDEO_WIDTH = "de.lespace.mscwebrtc.VIDEO_WIDTH";
    public static final String EXTRA_VIDEO_HEIGHT = "de.lespace.mscwebrtc.VIDEO_HEIGHT";
    public static final String EXTRA_VIDEO_FPS = "de.lespace.mscwebrtc.VIDEO_FPS";
    public static final String EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED = "org.appsopt.apprtc.VIDEO_CAPTUREQUALITYSLIDER";
    public static final String EXTRA_VIDEO_BITRATE = "de.lespace.mscwebrtc.VIDEO_BITRATE";
    public static final String EXTRA_VIDEOCODEC = "de.lespace.mscwebrtc.VIDEOCODEC";
    public static final String EXTRA_HWCODEC_ENABLED = "de.lespace.mscwebrtc.HWCODEC";
    public static final String EXTRA_CAPTURETOTEXTURE_ENABLED = "de.lespace.mscwebrtc.CAPTURETOTEXTURE";
    public static final String EXTRA_AUDIO_BITRATE = "de.lespace.mscwebrtc.AUDIO_BITRATE";
    public static final String EXTRA_AUDIOCODEC = "de.lespace.mscwebrtc.AUDIOCODEC";
    public static final String EXTRA_NOAUDIOPROCESSING_ENABLED = "de.lespace.mscwebrtc.NOAUDIOPROCESSING";
    public static final String EXTRA_AECDUMP_ENABLED = "de.lespace.mscwebrtc.AECDUMP";
    public static final String EXTRA_OPENSLES_ENABLED = "de.lespace.mscwebrtc.OPENSLES";
    public static final String EXTRA_DISPLAY_HUD = "de.lespace.mscwebrtc.DISPLAY_HUD";
    public static final String EXTRA_TRACING = "de.lespace.mscwebrtc.TRACING";
    public static final String EXTRA_CMDLINE = "de.lespace.mscwebrtc.CMDLINE";
    public static final String EXTRA_RUNTIME = "de.lespace.mscwebrtc.RUNTIME";
    public static final int CONNECTION_REQUEST = 1;
    public static PeerConnectionClient peerConnectionClient = null;
    public String from = "";
    public Toast logToast;
    public long callStartedTimeMs = 0;
    public static AppRTCClient appRtcClient;
    private static final String TAG = "RTCConnection";
    public boolean iceConnected;
    public boolean isError;
    public static SharedPreferences sharedPref;
    private boolean commandLineRun;
    public int runTimeMs;
    public boolean activityRunning;

    public AppRTCAudioManager audioManager = null;
    public RendererCommon.ScalingType scalingType;
    public boolean callControlFragmentVisible = true;

    public ArrayList<String> roomList;
    public ArrayAdapter adapter;
    public static Ringtone r;

    // Peer connection statistics callback period in ms.
    public static final int STAT_CALLBACK_PERIOD = 1000;

    // Remote video screen position
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;

    public static AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    public static PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    public static AppRTCClient.SignalingParameters signalingParam;

    public RTCConnection(){


    }



    @Override
    public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //onConnectedToRoomInternal(params);
            }
        });
    }

    @Override
    public void onChannelClose() {

    }

    @Override
    public void onUserListUpdate(final String response) {

        runOnUiThread(new Runnable() {


            @Override
            public void run() {
                try {
                JSONArray mJSONArray = new JSONArray(response);
                roomList = new ArrayList();
                    adapter.clear();
                adapter.notifyDataSetChanged();


                for(int i = 0; i < mJSONArray.length();i++){
                    String username = mJSONArray.getString(i);
                    if (username.length() > 0
                            && !roomList.contains(username)
                            && !username.equals(roomConnectionParameters.from)) {
                    roomList.add(username);
                    adapter.add(username);
                    }
                }
                    adapter.notifyDataSetChanged();
                }catch (JSONException e) {
                e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onIncomingCall(final String from) {


        // Notify UI that registration has completed, so the progress indicator can be hidden.
/*
        //Send Broadcast message to Service
        Intent registrationComplete = new Intent(QuickstartPreferences.INCOMING_CALL);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);

        startActivity(intent);*/

       /* Intent intent = new Intent(this, ConnectActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Intent intent = new Intent(this,CallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);*/
       // r.stop();
        //startActivity(intent);


        roomConnectionParameters.to = from;
        roomConnectionParameters.initiator = false;
        DialogFragment newFragment = new CallDialogFragment();


        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        if(alert == null){
            // alert is null, using backup
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // I can't see this ever being null (as always have a default notification)
            // but just incase
            if(alert == null) {
                // alert backup is null, using 2nd backup
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL);
            }
        }
        r = RingtoneManager.getRingtone(getApplicationContext(), alert);
        r.play();

        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.add(newFragment, "loading");
        transaction.commitAllowingStateLoss();

       // newFragment.show(getFragmentManager(), "incomingcall");
    }

        @Override
        public void onStartCommunication(final SessionDescription sdp) {
            final long delta = System.currentTimeMillis() - callStartedTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                        return;
                    }
                    logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                    peerConnectionClient.setRemoteDescription(sdp);
                }
            });
        }

        @Override
        public void onRemoteDescription(final SessionDescription sdp) {
            final long delta = System.currentTimeMillis() - callStartedTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (peerConnectionClient == null) {
                        Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
                        return;
                    }
                    logAndToast("Received remote " + sdp.type + ", delay=" + delta + "ms");
                    peerConnectionClient.setRemoteDescription(sdp);
                }
            });
        }

        // Log |msg| and Toast about it.
        public void logAndToast(String msg) {
            Log.d(TAG, msg);
            if (logToast != null) {
                logToast.cancel();
            }
            logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
            logToast.show();
        }

        @Override
        public void onRemoteIceCandidate(final IceCandidate candidate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (peerConnectionClient == null) {
                        Log.e(TAG,
                                "Received ICE candidate for non-initilized peer connection.");
                        return;
                    }
                    peerConnectionClient.addRemoteIceCandidate(candidate);
                }
            });
        }

    @Override
    public void onWebSocketError(String description) {
        logAndToast(description);
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
    // All callbacks are invoked from websocket signaling looper thread and
    // are routed to UI thread.

    private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;
        signalingParam = params;
    }

    public void onChannelError(final String description){
        logAndToast(description);
    }

    // Activity interfaces
    @Override
    public void onPause() {
        super.onPause();
        activityRunning = false;
        if (peerConnectionClient != null) {
            peerConnectionClient.stopVideoSource();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
        if (peerConnectionClient != null) {
            peerConnectionClient.startVideoSource();
        }
    }

    public void connectToWebsocket() {
        if (appRtcClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.");
            return;
        }
        callStartedTimeMs = System.currentTimeMillis();

        // Start room connection.
        appRtcClient.connectToWebsocket(roomConnectionParameters);
    }

    // Disconnect from remote resources, dispose of local resources, and exit.
    public void disconnect(boolean sendRemoteHangup) {

    }

    public boolean validateUrl(String url) {
        //if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
        if (isWSUrl(url) || isWSSUrl(url)) {
            return true;
        }

        new AlertDialog.Builder(this)
                .setTitle(getText(R.string.invalid_url_title))
                .setMessage(getString(R.string.invalid_url_text, url))
                .setCancelable(false)
                .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).create().show();
        return false;
    }


    public static boolean isWSUrl(String url) {
        return (null != url) &&
                (url.length() > 4) &&
                url.substring(0, 5).equalsIgnoreCase("ws://");
    }

    /**
     * @return True iff the url is an https: url.
     */
    public static boolean isWSSUrl(String url) {
        return (null != url) &&
                (url.length() > 5) &&
                url.substring(0, 6).equalsIgnoreCase("wss://");
    }

    public static class CallDialogFragment extends DialogFragment {

        public CallDialogFragment(){

        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Build the dialog and set up the button click handlers
            // 1. Instantiate an AlertDialog.Builder with its constructor
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

// 2. Chain together various setter methods to set the dialog characteristics
            builder.setMessage(R.string.calldialog_question).setTitle(R.string.calldialog_title);
            // Add the buttons
            builder.setPositiveButton(R.string.calldialog_answer, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Intent intent = new Intent(getActivity(),CallActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    r.stop();
                    startActivity(intent);
                }
            });
            builder.setNegativeButton(R.string.calldialog_hangung, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog send stop message to peer.
                    r.stop();
                    appRtcClient.sendStopToPeer();;
                }
            });

// 3. Get the AlertDialog from create()
            AlertDialog dialog = builder.create();

            return builder.create();
        }
    }
}
