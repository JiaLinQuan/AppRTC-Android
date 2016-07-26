package org.appspot.apprtc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.appspot.apprtc.util.LooperExecutor;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;


public class RTCConnection extends Activity implements
            AppRTCClient.SignalingEvents,
            PeerConnectionClient.PeerConnectionEvents,
            CallFragment.OnCallEvents{

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
    public String keyprefRoom;
    public String keyprefRoomList;

    public String from = "";
    public PeerConnectionClient peerConnectionClient = null;
    public Toast logToast;
    public long callStartedTimeMs = 0;
    public AppRTCClient appRtcClient;
    private static final String TAG = "RTCConnection";
    public boolean iceConnected;
    public boolean isError;
    public SharedPreferences sharedPref;
    public SurfaceViewRenderer localRender;
    public SurfaceViewRenderer remoteRender;
    private boolean commandLineRun;
    public int runTimeMs;
    public boolean activityRunning;
    public EglBase rootEglBase;
    public AppRTCAudioManager audioManager = null;
    public RendererCommon.ScalingType scalingType;
    public boolean callControlFragmentVisible = true;

    // Controls
    public CallFragment callFragment;
    public HudFragment hudFragment;
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

    // Local preview screen position before call is connected.
    private static final int LOCAL_X_CONNECTING = 0;
    private static final int LOCAL_Y_CONNECTING = 0;
    private static final int LOCAL_WIDTH_CONNECTING = 100;
    private static final int LOCAL_HEIGHT_CONNECTING = 100;

    public static AppRTCClient.RoomConnectionParameters roomConnectionParameters;
    public PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
    public PercentFrameLayout localRenderLayout;
    public PercentFrameLayout remoteRenderLayout;

    public static AppRTCClient.SignalingParameters signalingParam;

    public RTCConnection(){
            peerConnectionClient = PeerConnectionClient.getInstance();
            appRtcClient = new WebSocketRTCClient(this,new LooperExecutor());
            rootEglBase = EglBase.create();
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
        public void onUserListUpdate() {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  //  onConnectedToRoomInternal(params);//TODO update array with users!
                }
            });
        }

        @Override
        public void onIncomingCall(final String from) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    roomConnectionParameters.to = from;
                    roomConnectionParameters.initiator = false;
                    Intent intent = new Intent(RTCConnection.this, CallActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
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
                    if (!roomConnectionParameters.initiator) {
                        logAndToast("Creating ANSWER...");
                        // Create answer. Answer SDP will be sent to offering client in
                        // PeerConnectionEvents.onLocalDescription event.
                        peerConnectionClient.createAnswer();
                    }
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

        public void reportError(final String description) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isError) {
                        isError = true;
                        disconnectWithErrorMessage(description);
                    }
                }
            });
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
        public void onChannelClose() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAndToast("Remote end hung up; dropping PeerConnection");
                    disconnect();
                }
            });
        }

    @Override
    public void onChannelError(String description) {
        reportError(description);
    }

    // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
        // All callbacks are invoked from websocket signaling looper thread and
        // are routed to UI thread.
    private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
        final long delta = System.currentTimeMillis() - callStartedTimeMs;

        signalingParam = params;
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

    @Override
    protected void onDestroy() {
        disconnect();
        if (logToast != null) {
            logToast.cancel();
        }
        activityRunning = false;
        rootEglBase.release();
        super.onDestroy();
    }

    // CallFragment.OnCallEvents interface implementation.
    @Override
    public void onCallHangUp() {
        disconnect();
    }

    @Override
    public void onCameraSwitch() {
        if (peerConnectionClient != null) {
            peerConnectionClient.switchCamera();
        }
    }

    @Override
    public void onVideoScalingSwitch(RendererCommon.ScalingType scalingType) {
        this.scalingType = scalingType;
        updateVideoView();
    }

    @Override
    public void onCaptureFormatChange(int width, int height, int framerate) {
        if (peerConnectionClient != null) {
            peerConnectionClient.changeCaptureFormat(width, height, framerate);
        }
    }

    // Helper functions.
    public void toggleCallControlFragmentVisibility() {
        if (!iceConnected || !callFragment.isAdded()) {
            return;
        }
        // Show/hide call control fragment
        callControlFragmentVisible = !callControlFragmentVisible;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        if (callControlFragmentVisible) {
            ft.show(callFragment);
            ft.show(hudFragment);
        } else {
            ft.hide(callFragment);
            ft.hide(hudFragment);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

        // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
        // Send local peer connection SDP and ICE candidates to remote party.
        // All callbacks are invoked from peer connection client looper thread and
        // are routed to UI thread.
        @Override
        public void onLocalDescription(final SessionDescription sdp) {
            final long delta = System.currentTimeMillis() - callStartedTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (appRtcClient != null) {
                        logAndToast("Sending " + sdp.type + ", delay=" + delta + "ms");
                        if (roomConnectionParameters.initiator) {
                            appRtcClient.call(sdp);
                        } else {
                            appRtcClient.sendAnswerSdp(sdp);
                        }
                    }
                }
            });
        }

        @Override
        public void onIceCandidate(final IceCandidate candidate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (appRtcClient != null) {
                        appRtcClient.sendLocalIceCandidate(candidate);
                    }
                }
            });
        }

        @Override
        public void onIceConnected() {
            final long delta = System.currentTimeMillis() - callStartedTimeMs;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAndToast("ICE connected, delay=" + delta + "ms");
                    iceConnected = true;
                    callConnected();
                }
            });
        }

        @Override
        public void onIceDisconnected() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logAndToast("ICE disconnected");
                    iceConnected = false;
                    disconnect();
                }
            });
        }

        @Override
        public void onPeerConnectionClosed() {
        }

        @Override
        public void onPeerConnectionStatsReady(final StatsReport[] reports) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isError && iceConnected) {
                        hudFragment.updateEncoderStatistics(reports);
                    }
                }
            });
        }

        @Override
        public void onPeerConnectionError(final String description) {
            reportError(description);
        }

        private void disconnectWithErrorMessage(final String errorMessage) {
        if (commandLineRun || !activityRunning) {
              Log.e(TAG, "Critical error: " + errorMessage);
              disconnect();
        } else {
          new AlertDialog.Builder(this)
              .setTitle(getText(R.string.channel_error_title))
              .setMessage(errorMessage)
              .setCancelable(false)
              .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                  disconnect();
                }
              }).create().show();
        }
        }

        // Disconnect from remote resources, dispose of local resources, and exit.
        public void disconnect() {
            activityRunning = false;
            if (appRtcClient != null) {
                appRtcClient.disconnectFromRoom();
                appRtcClient = null;
            }
            if (peerConnectionClient != null) {
                peerConnectionClient.close();
                peerConnectionClient = null;
            }
            if (localRender != null) {
                localRender.release();
                localRender = null;
            }
            if (remoteRender != null) {
                remoteRender.release();
                remoteRender = null;
            }
            if (audioManager != null) {
                audioManager.close();
                audioManager = null;
            }
            if (iceConnected && !isError) {
                setResult(RESULT_OK);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        }

        // Should be called from UI thread
       private void callConnected() {
            final long delta = System.currentTimeMillis() - callStartedTimeMs;
            Log.i(TAG, "Call connected: delay=" + delta + "ms");
            if (peerConnectionClient == null || isError) {
                Log.w(TAG, "Call is connected in closed or error state");
                return;
            }
            // Update video view.
            //TODO updateVideoView();
            // Enable statistics callback.
            peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
       }

        public void connectToWebsocket() {
            if (appRtcClient == null) {
                Log.e(TAG, "AppRTC client is not allocated for a call.");
                return;
            }
            callStartedTimeMs = System.currentTimeMillis();

            // Start room connection.
            appRtcClient.connectToWebsocket(roomConnectionParameters);

            // Create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
            audioManager = AppRTCAudioManager.create(this, new Runnable() {
                        // This method will be called each time the audio state (number and
                        // type of devices) has been changed.
                        @Override
                        public void run() {
                            onAudioManagerChangedState();
                        }
                    }
            );
            // Store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.d(TAG, "Initializing the audio manager...");
            audioManager.init();
        }

    public void updateVideoView() {
        remoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        remoteRender.setScalingType(scalingType);
        remoteRender.setMirror(false);

        if (iceConnected) {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            localRender.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        } else {
            localRenderLayout.setPosition(
                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING, LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING);
            localRender.setScalingType(scalingType);
        }
        localRender.setMirror(true);

        localRender.requestLayout();
        remoteRender.requestLayout();
    }

    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
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

}
