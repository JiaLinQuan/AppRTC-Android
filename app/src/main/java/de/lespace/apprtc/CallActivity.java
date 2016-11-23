/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package de.lespace.apprtc;




import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.RendererCommon;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;


/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
public class CallActivity extends RTCConnection implements
        CallFragment.OnCallEvents,
        PeerConnectionClient.PeerConnectionEvents,
        GestureDetector.OnDoubleTapListener,
        WebSocketChannelClient.WebSocketChannelEvents, GestureDetector.OnGestureListener {


  private static final String TAG = "CallActivity";


  // Local preview screen position before call is connected.
  private static final int LOCAL_X_CONNECTING = 0;
  private static final int LOCAL_Y_CONNECTING = 0;
  private static final int LOCAL_WIDTH_CONNECTING = 100;
  private static final int LOCAL_HEIGHT_CONNECTING = 100;

  // Local preview screen position after call is connected.
  private static final int LOCAL_X_CONNECTED = 72;
  private static final int LOCAL_Y_CONNECTED = 72;
  private static final int LOCAL_WIDTH_CONNECTED = 25;
  private static final int LOCAL_HEIGHT_CONNECTED = 25;
  // Remote video screen position
  private static final int REMOTE_X = 0;
  private static final int REMOTE_Y = 0;
  private static final int REMOTE_WIDTH = 100;
  private static final int REMOTE_HEIGHT = 100;

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 0;
    // private AppRTCClient appRtcClient;
  private ScalingType scalingType;
  private Toast logToast;
  private boolean commandLineRun;
  private boolean sendDisconnectToPeer = true;
  private long callStartedTimeMs = 0;
  // Controls
  public CallFragment callFragment;
  public HudFragment hudFragment;
  public EglBase rootEglBase;
  public PercentFrameLayout localRenderLayout;
  public PercentFrameLayout remoteRenderLayout;
  public SurfaceViewRenderer localRender;
  public SurfaceViewRenderer remoteRender;
  private GestureDetectorCompat mDetector;
    private static boolean broadcastIsRegistered;


    public void MY_PERMISSIONS_REQUEST_READ_CONTACTS(){

    }
    @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Thread.setDefaultUncaughtExceptionHandler(
        new UnhandledExceptionHandler(this));

    // Set window styles for fullscreen-window size. Needs to be done before
    // adding content.
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().addFlags(
        LayoutParams.FLAG_FULLSCREEN
        | LayoutParams.FLAG_KEEP_SCREEN_ON
        | LayoutParams.FLAG_DISMISS_KEYGUARD
        | LayoutParams.FLAG_SHOW_WHEN_LOCKED
        | LayoutParams.FLAG_TURN_SCREEN_ON);

    getWindow().getDecorView().setSystemUiVisibility(
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        | View.SYSTEM_UI_FLAG_FULLSCREEN
        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    setContentView(R.layout.activity_call);
      // Instantiate the gesture detector with the
      // application context and an implementation of
      // GestureDetector.OnGestureListener

      mDetector = new GestureDetectorCompat(this,this);
      // Set the gesture detector as the double tap
      // listener.
      mDetector.setOnDoubleTapListener(this);

    iceConnected = false;

    scalingType = ScalingType.SCALE_ASPECT_FILL;

    callFragment = new CallFragment();
    hudFragment = new HudFragment();

    // Create UI controls.
    localRender = (SurfaceViewRenderer) findViewById(R.id.local_video_view);
    remoteRender = (SurfaceViewRenderer) findViewById(R.id.remote_video_view);
    localRenderLayout = (PercentFrameLayout) findViewById(R.id.local_video_layout);
    remoteRenderLayout = (PercentFrameLayout) findViewById(R.id.remote_video_layout);


    // Show/hide call control fragment on view click.
    View.OnClickListener listener = new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        toggleCallControlFragmentVisibility();
      }
    };

    localRender.setOnClickListener(listener);
    remoteRender.setOnClickListener(listener);

    // Create video renderers.
    rootEglBase = EglBase.create();
    localRender.init(rootEglBase.getEglBaseContext(), null);
    remoteRender.init(rootEglBase.getEglBaseContext(), null);
    localRender.setZOrderMediaOverlay(true);
    updateVideoView();

      setResult(RESULT_CANCELED);
      if(!broadcastIsRegistered) {
          registerReceiver(broadcast_reciever, new IntentFilter("finish_CallActivity"));
          broadcastIsRegistered = true;
      }


    callFragment = new CallFragment();
    hudFragment = new HudFragment();
    // Send intent arguments to fragments.
    callFragment.setArguments(getIntent().getExtras());
    hudFragment.setArguments(getIntent().getExtras());

    // Activate call and HUD fragments and start the call.
    FragmentTransaction ft = getFragmentManager().beginTransaction();
    ft.add(R.id.call_fragment_container, callFragment);
    ft.add(R.id.hud_fragment_container, hudFragment);
    ft.commit();


    // For command line execution run connection for <runTimeMs> and exit.
    if (commandLineRun && runTimeMs > 0) {
      (new Handler()).postDelayed(new Runnable() {
        @Override
        public void run() {
          disconnect(false);
        }
      }, runTimeMs);
    }

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

    peerConnectionClient = PeerConnectionClient.getInstance();
    peerConnectionClient.createPeerConnectionFactory(
            CallActivity.this, peerConnectionParameters, CallActivity.this);

    peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(),
            localRender,
            remoteRender, roomConnectionParameters.initiator);

    logAndToast("Creating OFFER...");
    // Create offer. Offer SDP will be sent to answering client in
    // PeerConnectionEvents.onLocalDescription event.
    peerConnectionClient.createOffer();
  }


    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
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

  @Override
  public void onCameraSwitch() {
    if (peerConnectionClient != null) {
      //peerConnectionClient.switchCamera();
      boolean renderVideo = !peerConnectionClient.renderVideo;
      peerConnectionClient.setVideoEnabled(renderVideo);
      logAndToast(renderVideo?"video enabled":"video disabled");
    }
  }

  @Override
  public void onAudioMute() {

      boolean muted = audioManager.setMicrophoneMute(true);
      logAndToast(muted?"muted":"unmuted");
  }

  @Override
  public void onCaptureFormatChange(int width, int height, int framerate) {
    if (peerConnectionClient != null) {
      peerConnectionClient.changeCaptureFormat(width, height, framerate);
    }
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
            appRtcClient.sendOfferSdp(sdp);
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
  protected void onDestroy() {

    disconnect(sendDisconnectToPeer);
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
    disconnect(true);
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
        disconnect(false);
      }
    });
  }

  @Override
  public void onPeerConnectionError(final String description) {
    reportError(description);
  }

  @Override
  public void onVideoScalingSwitch(RendererCommon.ScalingType scalingType) {
    this.scalingType = scalingType;
    updateVideoView();
  }

  @Override
  public void onChannelClose() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        logAndToast("Remote end hung up; dropping PeerConnection");
        disconnect(false);
      }
    });
  }

  @Override
  public void onChannelError(String description) {
    reportError(description);
  }

  private void disconnectWithErrorMessage(final String errorMessage) {
    if (commandLineRun || !activityRunning) {
      Log.e(TAG, "Critical error: " + errorMessage);
      disconnect(true);
    } else {
      new AlertDialog.Builder(this)
              .setTitle(getText(R.string.channel_error_title))
              .setMessage(errorMessage)
              .setCancelable(false)
              .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                  disconnect(true);
                }
              }).create().show();
    }
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
    updateVideoView();
    // Enable statistics callback.
    peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
  }

    public void disconnect(boolean sendRemoteHangup){

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


        if (appRtcClient != null && sendRemoteHangup) {
            appRtcClient.disconnectFromWebservice(); //send bye message to peer only when initiator
            sendDisconnectToPeer = false;
            // appRtcClient = null;
        }

       //DON'T DO THAT if(appRtcClient != null) appRtcClient = null;

        if (peerConnectionClient != null) {
            peerConnectionClient.close();
            peerConnectionClient = null;
        }

        if(activityRunning){
            activityRunning=false;
            setResult(RESULT_OK); //okey means send stop to client!
            finish();
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

    BroadcastReceiver broadcast_reciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            if (action.equals("finish_CallActivity")) {
                sendDisconnectToPeer = false;
                finish();
            }
        }
    };

    @Override
    public void onWebSocketMessage(String message) {

    }

    @Override
    public void onWebSocketClose() {

    }


  @Override
  public boolean onSingleTapConfirmed(MotionEvent e) {
    logAndToast("onSingleTapConfirmed");
    return false;
  }

  @Override
  public boolean onDoubleTap(MotionEvent e) {
    logAndToast("onDoubleTap");
    return false;
  }

  @Override
  public boolean onDoubleTapEvent(MotionEvent e) {
    logAndToast("onDoubleTapEvent");
    return false;
  }

  @Override
  public boolean onDown(MotionEvent e) {
    logAndToast("onDoubleTapEvent");
    return false;
  }

  @Override
  public void onShowPress(MotionEvent e) {
    logAndToast("onDoubleTapEvent");
  }

  @Override
  public boolean onSingleTapUp(MotionEvent e) {
    logAndToast("onSingleTapUp");
    return false;
  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    return false;
  }

  @Override
  public void onLongPress(MotionEvent e) {
    logAndToast("onLongPress");

  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    return false;
  }
}
