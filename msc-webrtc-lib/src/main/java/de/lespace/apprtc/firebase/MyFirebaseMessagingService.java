package de.lespace.apprtc.firebase;

/**
 * Created by nico on 15.02.17.
 */

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


    import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import de.lespace.apprtc.RTCConnection;
import de.lespace.apprtc.activity.IncomingCall;
import de.lespace.apprtc.service.SignalingService;


public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        JSONObject jsonData = new JSONObject(remoteMessage.getData());
        Log.d(TAG, "calldata: " + jsonData.toString());

        Intent intent = new Intent(this, SignalingService.class);
        intent.putExtra(RTCConnection.EXTRA_SIGNALING_REGISTRATION,"true");
        intent.putExtra(RTCConnection.EXTRA_FROM, remoteMessage.getData().get("toUUID"));
        startService(intent);

        //Start Registration
        Log.d(TAG, "we should call to:" + remoteMessage.getData().get("fromUUID")+" but callto 'chrome' now!");
        Intent connectIntent = new Intent(getApplicationContext(), IncomingCall.class);
        String to = "chrome";
      /*  RTCConnection.from = remoteMessage.getData().get("toUUID");
        RTCConnection.to = to;
        RTCConnection.initiator = true;*/
        connectIntent.putExtra(RTCConnection.EXTRA_TO, to);
        connectIntent.putExtra(RTCConnection.EXTRA_FROM, remoteMessage.getData().get("toUUID"));
        connectIntent.putExtra(RTCConnection.EXTRA_INITIATOR, true);
        connectIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(connectIntent);


    }
    // [END receive_message]
}