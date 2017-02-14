/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package de.lespace.apprtc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.List;


/**
 * Handles the initial setup where the user selects which room to join.
 */
public class ConnectActivity extends RTCConnection {

  private static final String TAG = "ConnectActivity";

  private ImageButton connectButton;
  private String keyprefFrom;
  private String keyprefVideoCallEnabled;
  private String keyprefResolution;
  private String keyprefFps;
  private String keyprefCaptureQualitySlider;
  private String keyprefVideoBitrateType;
  private String keyprefVideoBitrateValue;
  private String keyprefVideoCodec;
  private String keyprefAudioBitrateType;
  private String keyprefAudioBitrateValue;
  private String keyprefAudioCodec;
  private String keyprefHwCodecAcceleration;
  private String keyprefCaptureToTexture;
  private String keyprefNoAudioProcessingPipeline;
  private String keyprefAecDump;
  private String keyprefOpenSLES;
  private String keyprefDisplayHud;
  private String keyprefTracing;
  private String keyprefRoomServerUrl;
  private String keyprefRoomList;
  private ArrayAdapter adapter;
  private ListView roomListView;
  private List<String> missingPermissions;
  private Intent intent = null;

  private BroadcastReceiver networkchangeBroadcastReceiver;
  private boolean isNetworkChangeReceiverRegistered;

  // List of mandatory application permissions.
  private static final String[] MANDATORY_PERMISSIONS = {
          "android.permission.MODIFY_AUDIO_SETTINGS",
          "android.permission.RECORD_AUDIO",
          "android.permission.CAMERA",
          "android.permission.INTERNET"
  };

  @Override
  public void onRequestPermissionsResult(
          int requestCode,
          String permissions[],
          int[] grantResults) {

    if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

      Log.e(TAG,  MANDATORY_PERMISSIONS[0] +" Permission Granted!");

    } else {
      Log.e(TAG, MANDATORY_PERMISSIONS[0] + " Permission Denied!");
    }
    missingPermissions.remove(0); //remove missing permission from array and request next left permission
    requestPermission();
  }


  //http://stackoverflow.com/questions/35484767/activitycompat-requestpermissions-not-showing-dialog-box
  //https://developer.android.com/training/permissions/requesting.html
  private void requestPermission() {
    if (missingPermissions.size() > 0)
      ActivityCompat.requestPermissions(this, new String[]{missingPermissions.get(0)}, missingPermissions.size());
  }



  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_connect);

    // Get setting keys.
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    keyprefFrom = getString(R.string.pref_from_key);
    keyprefVideoCallEnabled = getString(R.string.pref_videocall_key);
    keyprefResolution = getString(R.string.pref_resolution_key);
    keyprefFps = getString(R.string.pref_fps_key);
    keyprefCaptureQualitySlider = getString(R.string.pref_capturequalityslider_key);
    keyprefVideoBitrateType = getString(R.string.pref_startvideobitrate_key);
    keyprefVideoBitrateValue = getString(R.string.pref_startvideobitratevalue_key);
    keyprefVideoCodec = getString(R.string.pref_videocodec_key);
    keyprefHwCodecAcceleration = getString(R.string.pref_hwcodec_key);
    keyprefCaptureToTexture = getString(R.string.pref_capturetotexture_key);
    keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
    keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
    keyprefAudioCodec = getString(R.string.pref_audiocodec_key);
    keyprefNoAudioProcessingPipeline = getString(R.string.pref_noaudioprocessing_key);
    keyprefAecDump = getString(R.string.pref_aecdump_key);
    keyprefOpenSLES = getString(R.string.pref_opensles_key);
    keyprefDisplayHud = getString(R.string.pref_displayhud_key);
    keyprefTracing = getString(R.string.pref_tracing_key);
    keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);
    keyprefRoomList = getString(R.string.pref_room_list_key);

    // Video call enabled flag.
    boolean videoCallEnabled = sharedPref.getBoolean(keyprefVideoCallEnabled, Boolean.valueOf(getString(R.string.pref_videocall_default)));


    String wssUrl = sharedPref.getString(keyprefRoomServerUrl, getString(R.string.pref_room_server_url_default));
    from = sharedPref.getString(keyprefFrom, getString(R.string.pref_from_default));
    // Get default codecs.
    String videoCodec = sharedPref.getString(keyprefVideoCodec, getString(R.string.pref_videocodec_default));
    String audioCodec = sharedPref.getString(keyprefAudioCodec, getString(R.string.pref_audiocodec_default));

    // Check HW codec flag.
    boolean hwCodec = sharedPref.getBoolean(keyprefHwCodecAcceleration, Boolean.valueOf(getString(R.string.pref_hwcodec_default)));

    // Check Capture to texture.
    boolean captureToTexture = sharedPref.getBoolean(keyprefCaptureToTexture, Boolean.valueOf(getString(R.string.pref_capturetotexture_default)));

    // Check Disable Audio Processing flag.
    boolean noAudioProcessing = sharedPref.getBoolean(keyprefNoAudioProcessingPipeline, Boolean.valueOf(getString(R.string.pref_noaudioprocessing_default)));

    // Check Disable Audio Processing flag.
    boolean aecDump = sharedPref.getBoolean(keyprefAecDump, Boolean.valueOf(getString(R.string.pref_aecdump_default)));

    // Check OpenSL ES enabled flag.
    boolean useOpenSLES = sharedPref.getBoolean(
            keyprefOpenSLES,
            Boolean.valueOf(getString(R.string.pref_opensles_default)));

    // Check for mandatory permissions.
    missingPermissions = new ArrayList();

    for (String permission : MANDATORY_PERMISSIONS) {
      if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        missingPermissions.add(permission);
      }
    }
    requestPermission();

    networkchangeBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {

        if(intent.getFlags()==0){
          SignalingService.appRTCClient.resetWebsocket();
        }
        if(intent.getFlags()==1){
          SignalingService.appRTCClient.reconnect();
          Toast.makeText(context, "network is online:"+intent.getFlags(), Toast.LENGTH_LONG).show();
        }
      }
    };


    // Registering BroadcastReceiver
    registerNetworkChangeReceiver();

    // Get video resolution from settings.
    int videoWidth = 0;
    int videoHeight = 0;
    String resolution = sharedPref.getString(keyprefResolution,
            getString(R.string.pref_resolution_default));
    String[] dimensions = resolution.split("[ x]+");
    if (dimensions.length == 2) {
      try {
        videoWidth = Integer.parseInt(dimensions[0]);
        videoHeight = Integer.parseInt(dimensions[1]);
      } catch (NumberFormatException e) {
        videoWidth = 0;
        videoHeight = 0;
        Log.e(TAG, "Wrong video resolution setting: " + resolution);
      }
    }

    // Get camera fps from settings.
    int cameraFps = 0;
    String fps = sharedPref.getString(keyprefFps,getString(R.string.pref_fps_default));
    String[] fpsValues = fps.split("[ x]+");
    if (fpsValues.length == 2) {
      try {
        cameraFps = Integer.parseInt(fpsValues[0]);
      } catch (NumberFormatException e) {
        Log.e(TAG, "Wrong camera fps setting: " + fps);
      }
    }

    // Check capture quality slider flag.
    boolean captureQualitySlider = sharedPref.getBoolean(keyprefCaptureQualitySlider,
            Boolean.valueOf(getString(R.string.pref_capturequalityslider_default)));

    // Get video and audio start bitrate.
    int videoStartBitrate = 0;
    String bitrateTypeDefault = getString(
            R.string.pref_startvideobitrate_default);
    String bitrateType = sharedPref.getString(
            keyprefVideoBitrateType, bitrateTypeDefault);
    if (!bitrateType.equals(bitrateTypeDefault)) {
      String bitrateValue = sharedPref.getString(keyprefVideoBitrateValue, getString(R.string.pref_startvideobitratevalue_default));
      videoStartBitrate = Integer.parseInt(bitrateValue);
    }
    int audioStartBitrate = 0;
    bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default);
    bitrateType = sharedPref.getString(
            keyprefAudioBitrateType, bitrateTypeDefault);
    if (!bitrateType.equals(bitrateTypeDefault)) {
      String bitrateValue = sharedPref.getString(keyprefAudioBitrateValue,
              getString(R.string.pref_startaudiobitratevalue_default));
      audioStartBitrate = Integer.parseInt(bitrateValue);
    }

    // Check statistics display option.
    boolean displayHud = sharedPref.getBoolean(keyprefDisplayHud, Boolean.valueOf(getString(R.string.pref_displayhud_default)));

    boolean tracing = sharedPref.getBoolean(keyprefTracing, Boolean.valueOf(getString(R.string.pref_tracing_default)));

   // Log.d(TAG, "Connecting from " + from + " at URL " + wssUrl);

    if (validateUrl(wssUrl)) {
      Uri uri = Uri.parse(wssUrl);
      intent = new Intent(this, ConnectActivity.class);
      intent.setData(uri);
      intent.putExtra(CallActivity.EXTRA_VIDEO_CALL, videoCallEnabled);
      intent.putExtra(CallActivity.EXTRA_VIDEO_WIDTH, videoWidth);
      intent.putExtra(CallActivity.EXTRA_VIDEO_HEIGHT, videoHeight);
      intent.putExtra(CallActivity.EXTRA_VIDEO_FPS, cameraFps);
      intent.putExtra(CallActivity.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, captureQualitySlider);
      intent.putExtra(CallActivity.EXTRA_VIDEO_BITRATE, videoStartBitrate);
      intent.putExtra(CallActivity.EXTRA_VIDEOCODEC, videoCodec);
      intent.putExtra(CallActivity.EXTRA_HWCODEC_ENABLED, hwCodec);
      intent.putExtra(CallActivity.EXTRA_CAPTURETOTEXTURE_ENABLED, captureToTexture);
      intent.putExtra(CallActivity.EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing);
      intent.putExtra(CallActivity.EXTRA_AECDUMP_ENABLED, aecDump);
      intent.putExtra(CallActivity.EXTRA_OPENSLES_ENABLED, useOpenSLES);
      intent.putExtra(CallActivity.EXTRA_AUDIO_BITRATE, audioStartBitrate);
      intent.putExtra(CallActivity.EXTRA_AUDIOCODEC, audioCodec);
      intent.putExtra(CallActivity.EXTRA_DISPLAY_HUD, displayHud);
      intent.putExtra(CallActivity.EXTRA_TRACING, tracing);
      intent.putExtra(CallActivity.EXTRA_RUNTIME, runTimeMs);
    }

    roomListView = (ListView) findViewById(R.id.room_listview);
    roomListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

    connectButton = (ImageButton) findViewById(R.id.connect_button);
    connectButton.setOnClickListener(connectListener);

    // If an implicit VIEW intent is launching the app, go directly to that URL.
    //final Intent intent = getIntent();
    Uri wsurl = Uri.parse(wssUrl);
    //intent.getData();
   // Log.d(TAG, "connecting to:" + wsurl.toString());
    if (wsurl == null) {
      logAndToast(getString(R.string.missing_wsurl));
      Log.e(TAG, "Didn't get any URL in intent!");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    if (from == null || from.length() == 0) {
      logAndToast(getString(R.string.missing_from));
      Log.e(TAG, "Incorrect from in intent!");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    peerConnectionParameters = new PeerConnectionClient.PeerConnectionParameters(
            videoCallEnabled,
            tracing,
            videoWidth, videoHeight, cameraFps, videoStartBitrate, videoCodec, hwCodec,
            captureToTexture, audioStartBitrate, audioCodec, noAudioProcessing,
            aecDump, useOpenSLES);


    Intent intent = getIntent();
    Log.i(TAG, "created apprtc with roomUri:" + wsurl.toString() + " from:" + from);
    Log.i(TAG, "intent.EXTRA_TO:"+intent.getStringExtra(RTCConnection.EXTRA_TO));
    Log.i(TAG, "intent.EXTRA_INITIATOR:"+intent.getBooleanExtra(RTCConnection.EXTRA_INITIATOR,false));

    if(intent.getStringExtra(RTCConnection.EXTRA_TO)!=null
            && !intent.getStringExtra(RTCConnection.EXTRA_TO).equals("")){

      RTCConnection.to = intent.getStringExtra(RTCConnection.EXTRA_TO);
      RTCConnection.initiator = intent.getBooleanExtra(RTCConnection.EXTRA_INITIATOR,false);;
      connectToUser();
    }

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.connect_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items.
    if (item.getItemId() == R.id.action_settings) {
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onPause() {

    //String room = roomEditText.getText().toString();
    String roomListJson = new JSONArray(userList).toString();
    SharedPreferences.Editor editor = sharedPref.edit();
    //editor.putString(keyprefRoom, room);
    editor.putString(keyprefRoomList, roomListJson);
    editor.commit();

    super.onPause();
  }

  @Override
  public void onResume() {

    userList = new ArrayList<String>();
    String roomListJson = sharedPref.getString(keyprefRoomList, null);
    if (roomListJson != null) {
      try {
        JSONArray jsonArray = new JSONArray(roomListJson);
        for (int i = 0; i < jsonArray.length(); i++) {
          userList.add(jsonArray.get(i).toString());
        }
      } catch (JSONException e) {
        Log.e(TAG, "Failed to load room list: " + e.toString());
      }
    }
    adapter = new ArrayAdapter<String>(
            this, android.R.layout.simple_list_item_1, userList);
    roomListView.setAdapter(adapter);
    if (adapter.getCount() > 0) {
      roomListView.requestFocus();
      roomListView.setItemChecked(0, true);
    }

    super.onResume();
  }

  public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {

    if (requestCode == CONNECTION_REQUEST) {
      Log.d(TAG, "Return: " + resultCode);
      setResult(resultCode);
      //commandLineRun = false;
      finish();
    }
    if(requestCode==RESULT_OK){
      //Now start callActivityAgain!
      //if(callActive){
        connectToUser();
        //callActive=false;
     // }
    }
  }

  private final OnClickListener connectListener = new OnClickListener() {
    @Override
    public void onClick(View view) {

      String to = getSelectedItem();
      if(to !=null){
        RTCConnection.initiator = true;
        RTCConnection.to = to;

        connectToUser();
      }

    }
  };

  private void connectToUser() {

    Intent newIntent = new Intent(this, CallActivity.class);
    newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    newIntent.putExtra("keep", true);
    newIntent.putExtras(intent);
    startActivityForResult(newIntent, CONNECTION_REQUEST);

  }


  private String getSelectedItem() {
    int position = AdapterView.INVALID_POSITION;
    if (roomListView.getCheckedItemCount() > 0 && adapter.getCount() > 0) {
      position = roomListView.getCheckedItemPosition();
      if (position >= adapter.getCount()) {
        position = AdapterView.INVALID_POSITION;
      }
    }
    if (position != AdapterView.INVALID_POSITION) {
      return (String) adapter.getItem(position);
    } else {
      return null;
    }
  }

  @Override
  public void onChannelError(String description) {
    logAndToast(description);
  }

  private void registerNetworkChangeReceiver() {
    if (!isNetworkChangeReceiverRegistered) {
      LocalBroadcastManager.getInstance(this).registerReceiver(networkchangeBroadcastReceiver,
              new IntentFilter(QuickstartPreferences.NETWORK_ONLINE));

      isNetworkChangeReceiverRegistered = true;
    }
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();

  }
/*
  public static class CallDialogFragment extends DialogFragment {

    public CallDialogFragment() {

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

          Intent intent = new Intent(getActivity(), CallActivity.class);
          intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          r.stop();
          startActivityForResult(intent, CONNECTION_REQUEST);

        //  startActivity(intent);
        }
      });
      builder.setNegativeButton(R.string.calldialog_hangung, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          // User cancelled the dialog send stop message to peer.
          r.stop();
          SignalingService.appRTCClient.sendStopToPeer();
          ;
        }
      });

// 3. Get the AlertDialog from create()

      return builder.create();
    }
  }*/
}
