/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.appspot.apprtc;


import org.appspot.apprtc.WebSocketChannelClient.WebSocketChannelEvents;
import org.appspot.apprtc.WebSocketChannelClient.WebSocketConnectionState;
import org.appspot.apprtc.util.AppRTCUtils;
import org.appspot.apprtc.util.LooperExecutor;

import android.content.Intent;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.LinkedList;

/**
 * Negotiates signaling for chatting with apprtc.appspot.com "rooms".
 *
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToWebsocket().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
public class WebSocketRTCClient implements AppRTCClient, WebSocketChannelEvents {

    private static final String TAG = "WebSocketRTCClient";


  private enum ConnectionState {
    NEW, CONNECTED, CLOSED, ERROR
  };
  private enum MessageType {
    MESSAGE, LEAVE
  };
  private final LooperExecutor executor;
  private boolean initiator;

    private WebSocketChannelClient wsClient;
    private WebSocketConnectionState socketState;
    private RoomConnectionParameters connectionParameters;


    private SignalingEvents events;
    public WebSocketRTCClient(SignalingEvents events, LooperExecutor executor) {
        this.executor = executor;
        this.socketState = WebSocketConnectionState.NEW;
        this.events = events;

     executor.requestStart();
  }
    // --------------------------------------------------------------------
    // WebSocketChannelEvents interface implementation.
    // All events are called by WebSocketChannelClient on a local looper thread
    // (passed to WebSocket client constructor).
    @Override
    public void onWebSocketMessage(final String msg){
        try {

            JSONObject json = new JSONObject(msg);

            if (socketState != WebSocketConnectionState.REGISTERED) {
                Log.e(TAG, "websocket still in non registered state.");

                if (json.getString("params")!=null) {
                    Log.i(TAG, "Got appConfig"+msg+" parsing into roomParameters");
                    //this.roomParametersFetcher.parseAppConfig(msg);

                        Log.i(TAG, "app config: " + msg);
                        try {


                            JSONObject appConfig = new JSONObject(msg);

                            String result = appConfig.getString("result");
                            Log.i(TAG, "client debug ");
                            if (!result.equals("SUCCESS")) {
                                return;
                            }

                            String params = appConfig.getString("params");
                            appConfig = new JSONObject(params);


                            LinkedList<PeerConnection.IceServer> iceServers = iceServersFromPCConfigJSON(appConfig.getString("pc_config"));

                            AppRTCClient.SignalingParameters signalingParameters = new SignalingParameters(iceServers);


                            wsClient.register(connectionParameters.from);
                        } catch (JSONException e) {
                         //   events.onSignalingParametersError("Room JSON parsing error: " + e.toString());
                        }
                    }
                    socketState = WebSocketConnectionState.REGISTERED;
                return;
            }



            String id = "";
            String response = "";

            if(json.has("id")) id = json.getString("id");

            if(id.equals("registerResponse")){

                response = json.getString("response"); //TODO if not accepted what todo?
                String message = json.getString("message");

                if(response.equals("rejected"))
                    reportError("register rejected: " + message);

                if(response.equals("skipped")) Log.e(TAG, "registration was skipped because: "+message);
            }

            if(id.equals("registeredUsers")){
                response = json.getString("response");
                events.onUserListUpdate(response);
            }


            if(id.equals("callResponse")){
                response = json.getString("response");

                if(response.startsWith("rejected")) {
                    reportError("call rejected:" + response);
                    events.onChannelClose();
                }else{
                    Log.d(TAG, "sending sdpAnswer: "+response);
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.ANSWER,json.getString("sdpAnswer"));

                    events.onRemoteDescription(sdp);
                }
            }

            if(id.equals("incomingCall")){ //looks like some reject messages! other wise create offer for
                Log.d(TAG, "incomingCall "+json.toString());
                events.onIncomingCall(json.getString("from"));
            }


            if(id.equals("startCommunication")){
                Log.d(TAG, "startCommunication "+json.toString());
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,json.getString("sdpAnswer"));
                events.onStartCommunication(sdp);
            }

            if(id.equals("stopCommunication")){
                Log.d(TAG, "stopCommunication "+json.toString());
                events.onChannelClose();
            }

            if(id.equals("iceCandidate")){
                Log.d(TAG, "iceCandidate "+json.toString());

                JSONObject candidateJson = json.getJSONObject("candidate");


                IceCandidate candidate = new IceCandidate(
                        candidateJson.getString("sdpMid"),
                        candidateJson.getInt("sdpMLineIndex"),
                        candidateJson.getString("candidate"));

                events.onRemoteIceCandidate(candidate);
            }
            if (id.equals("stop")) {
                events.onChannelClose();
            }

        } catch (JSONException e) {
            reportError("WebSocket message JSON parsing error: " + e.toString());
        }
    }
  // --------------------------------------------------------------------
  // AppRTCClient interface implementation.
  // Asynchronously connect to an AppRTC room URL using supplied connection
  // parameters, retrieves room parameters and connect to WebSocket server.
  @Override
  public void connectToWebsocket(RoomConnectionParameters connectionParameters) {
    this.connectionParameters = connectionParameters;
    executor.execute(new Runnable() {
      @Override
      public void run() {
        connectToWebsocketInternal();
      }
    });
  }

  @Override
  public void disconnectFromRoom() {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        disconnectFromRoomInternal();
      }
    });
    executor.requestStop();
  }

  // Connects to websocket - function runs on a local looper thread.
  private void connectToWebsocketInternal() {

      String connectionUrl = getConnectionUrl(connectionParameters);

      socketState = WebSocketConnectionState.NEW;
      wsClient = new WebSocketChannelClient(executor, this);
      wsClient.connect(connectionUrl);
      socketState = WebSocketConnectionState.CONNECTED;

      Log.d(TAG, "wsClient connect " + connectionUrl);

  }

  // Disconnect from room and send bye messages - runs on a local looper thread.
  private void disconnectFromRoomInternal() {
    Log.d(TAG, "Disconnect. Room state: " + socketState);
    if (socketState == WebSocketConnectionState.CONNECTED
            || socketState == WebSocketConnectionState.NEW
            || socketState == WebSocketConnectionState.REGISTERED) {
        Log.d(TAG, "Closing room.");
        JSONObject jsonMessage = new JSONObject();
        jsonPut(jsonMessage, "id" , "stop");
        wsClient.send(jsonMessage.toString());
    }
    socketState = WebSocketConnectionState.CLOSED;
    if (wsClient != null) {
      wsClient.disconnect(true);
    }
  }

  // Helper functions to get connection, sendSocketMessage message and leave message URLs
  private String getConnectionUrl(RoomConnectionParameters connectionParameters) {
        return connectionParameters.roomUrl+"/ws";
  }

    // Return the list of ICE servers described by a WebRTCPeerConnection
    // configuration string.
    private LinkedList<PeerConnection.IceServer> iceServersFromPCConfigJSON(String pcConfig) throws JSONException {
        JSONObject json = new JSONObject(pcConfig);
        Log.d(TAG, "current pcConfig: " + pcConfig);
        JSONObject iceJson  = json.getJSONObject("iceServers");

        LinkedList<PeerConnection.IceServer> turnServers =  new LinkedList<PeerConnection.IceServer>();
        String username = iceJson.getString("username");
        String password = iceJson.getString("password");
        JSONArray turnUris = iceJson.getJSONArray("uris");

        for (int i = 0; i < turnUris.length(); i++) {
            String uri = turnUris.getString(i);
            Log.d(TAG, "adding ice server: " + uri+" username:"+username+" password:"+password);
            turnServers.add(new PeerConnection.IceServer(uri, username, password));
        }

        return turnServers;
    }

  public void call(final SessionDescription sdp)  {
      executor.execute(new Runnable() {
          @Override
          public void run() {
              if (socketState != WebSocketConnectionState.REGISTERED) {
                  reportError("Sending offer SDP in non registered state.");
                  return;
              }

              JSONObject json = new JSONObject();

              jsonPut(json,"id","call");
              jsonPut(json,"from",connectionParameters.from);
              jsonPut(json,"to",connectionParameters.to);
              jsonPut(json, "sdpOffer", sdp.description);
              wsClient.send(json.toString());

          }
      });
  }

  // Send local answer SDP to the other participant.
  @Override
  public void sendOfferSdp(final SessionDescription sdp) {
    executor.execute(new Runnable() {
      @Override
      public void run() {

        if (connectionParameters.loopback) {
          Log.e(TAG, "Sending answer in loopback mode.");
          return;
        }

          JSONObject json = new JSONObject();
          jsonPut(json, "id","incomingCallResponse");
          jsonPut(json, "from", connectionParameters.to);
          jsonPut(json, "callResponse",  "accept");
          jsonPut(json, "sdpOffer", sdp.description);


        wsClient.send(json.toString());
      }
    });
  }

  // Send Ice candidate to the other participant.
  @Override
  public void sendLocalIceCandidate(final IceCandidate candidate) {
    executor.execute(new Runnable() {
      @Override
      public void run() {

        JSONObject json = new JSONObject();
          jsonPut(json, "id", "onIceCandidate");
          jsonPut(json, "candidate", candidate.sdp);
          jsonPut(json, "sdpMid", candidate.sdpMid);
          jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);


          // Call receiver sends ice candidates to websocket server.
          wsClient.send(json.toString());
      }
    });
  }



  @Override
  public void onWebSocketClose() {
    events.onChannelClose();
  }

  @Override
  public void onWebSocketError(String description) {
    reportError("WebSocket error: " + description);
  }

  // --------------------------------------------------------------------
  // Helper functions.
  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (socketState != WebSocketConnectionState.ERROR) {
          socketState = WebSocketConnectionState.ERROR;
          events.onChannelError(errorMessage);
        }
      }
    });
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
