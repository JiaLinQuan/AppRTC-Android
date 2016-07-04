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

import org.appspot.apprtc.RoomParametersFetcher.RoomParametersFetcherEvents;
import org.appspot.apprtc.WebSocketChannelClient.WebSocketChannelEvents;
import org.appspot.apprtc.WebSocketChannelClient.WebSocketConnectionState;
import org.appspot.apprtc.util.LooperExecutor;

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
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 * <p>To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
public class WebSocketRTCClient implements AppRTCClient,
    WebSocketChannelEvents {

    private static final String TAG = "WebSocketRTCClient";
    private RoomParametersFetcher roomParametersFetcher;

  private enum ConnectionState {
    NEW, CONNECTED, CLOSED, ERROR
  };
  private enum MessageType {
    MESSAGE, LEAVE
  };
  private final LooperExecutor executor;
  private boolean initiator;
  private SignalingEvents events;
  private WebSocketChannelClient wsClient;
  private ConnectionState roomState;
  private RoomConnectionParameters connectionParameters;

  public WebSocketRTCClient(SignalingEvents events, LooperExecutor executor) {
    this.events = events;
    this.executor = executor;
    roomState = ConnectionState.NEW;
    executor.requestStart();
  }

  // --------------------------------------------------------------------
  // AppRTCClient interface implementation.
  // Asynchronously connect to an AppRTC room URL using supplied connection
  // parameters, retrieves room parameters and connect to WebSocket server.
  @Override
  public void connectToRoom(RoomConnectionParameters connectionParameters) {
    this.connectionParameters = connectionParameters;
    executor.execute(new Runnable() {
      @Override
      public void run() {
        connectToRoomInternal();
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

  // Connects to room - function runs on a local looper thread.
  private void connectToRoomInternal() {
      String connectionUrl = getConnectionUrl(connectionParameters);

      roomState = ConnectionState.NEW;
      wsClient = new WebSocketChannelClient(executor, this);
      wsClient.connect(connectionUrl);

      Log.d(TAG, "wsClient connect " + connectionUrl);

      RoomParametersFetcherEvents callbacks = new RoomParametersFetcherEvents() {
      @Override
      public void onSignalingParametersReady(
          final SignalingParameters params) {
        WebSocketRTCClient.this.executor.execute(new Runnable() {
          @Override
          public void run() {
            WebSocketRTCClient.this.signalingParametersReady(params);
          }
        });
      }

      @Override
      public void onSignalingParametersError(String description) {
        WebSocketRTCClient.this.reportError(description);
      }
    };

    this.roomParametersFetcher = new RoomParametersFetcher(connectionUrl, wsClient, callbacks);
      this.roomParametersFetcher.makeRequest();
  }

  // Disconnect from room and send bye messages - runs on a local looper thread.
  private void disconnectFromRoomInternal() {
    Log.d(TAG, "Disconnect. Room state: " + roomState);
    if (roomState == ConnectionState.CONNECTED) {
        Log.d(TAG, "Closing room.");
        JSONObject jsonMessage = new JSONObject();
        jsonPut(jsonMessage, "id" , "stop");
        wsClient.send(jsonMessage.toString());
    }
    roomState = ConnectionState.CLOSED;
    if (wsClient != null) {
      wsClient.disconnect(true);
    }
  }

  // Helper functions to get connection, post message and leave message URLs
  private String getConnectionUrl(RoomConnectionParameters connectionParameters) {
    return connectionParameters.roomUrl+"/ws";
  }

  // Callback issued when room parameters are extracted. Runs on local
  // looper thread.
  private void signalingParametersReady(
      final SignalingParameters signalingParameters) {
    Log.d(TAG, "Room connection completed.");
    if (connectionParameters.loopback
        && (!signalingParameters.initiator
            || signalingParameters.offerSdp != null)) {
      reportError("Loopback room is busy.");
      return;
    }
    if (!connectionParameters.loopback
        && !signalingParameters.initiator
        && signalingParameters.offerSdp == null) {
      Log.w(TAG, "No offer SDP in room response.");
    }
      initiator = signalingParameters.initiator;
      roomState = ConnectionState.CONNECTED;

    // Fire connection and signaling parameters events.
    events.onConnectedToRoom(signalingParameters);

    // Connect and register WebSocket client.
      wsClient.connect(signalingParameters.wssUrl);
      wsClient.register(connectionParameters.from);
  }

  public void call(final SessionDescription sdp)  {
      executor.execute(new Runnable() {
          @Override
          public void run() {
              if (roomState != ConnectionState.CONNECTED) {
                  reportError("Sending offer SDP in non connected state.");
                  return;
              }

              JSONObject json = new JSONObject();

              jsonPut(json,"id","call");
              jsonPut(json,"from",connectionParameters.from);
              jsonPut(json,"to",connectionParameters.to);
              jsonPut(json, "sdpOffer", sdp.description);
              wsClient.send(json.toString());

              if (connectionParameters.loopback) {
                  // In loopback mode rename this offer to answer and route it back.
                  SessionDescription sdpAnswer = new SessionDescription(
                          SessionDescription.Type.fromCanonicalForm("answer"),
                          sdp.description);
                  events.onRemoteDescription(sdpAnswer);
              }
          }
      });
  }

  // Send local answer SDP to the other participant.
  @Override
  public void sendAnswerSdp(final SessionDescription sdp) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (connectionParameters.loopback) {
          Log.e(TAG, "Sending answer in loopback mode.");
          return;
        }
        JSONObject json = new JSONObject();
        jsonPut(json, "sdp", sdp.description);
        jsonPut(json, "type", "answer");
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


          if (initiator) {
          // Call initiator sends ice candidates to GAE server.
              if (roomState != ConnectionState.CONNECTED) {
                reportError("Sending ICE candidate in non connected state.");
                return;
              }
             wsClient.send(json.toString());
        //  sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
          if (connectionParameters.loopback) {
                events.onRemoteIceCandidate(candidate);
          }
        } else {
          // Call receiver sends ice candidates to websocket server.
          wsClient.send(json.toString());
        }
      }
    });
  }

  // --------------------------------------------------------------------
  // WebSocketChannelEvents interface implementation.
  // All events are called by WebSocketChannelClient on a local looper thread
  // (passed to WebSocket client constructor).
  @Override
  public void onWebSocketMessage(final String msg){
      try {

          JSONObject json = new JSONObject(msg);

          if (wsClient.getState() != WebSocketConnectionState.REGISTERED) {
              Log.e(TAG, "Got WebSocket message in non registered state.");

                if (json.getString("params")!=null) {
                    Log.i(TAG, "Got appConfig"+msg+" parsing into roomParameters");
                    this.roomParametersFetcher.roomHttpResponseParse(msg);
                }
              return;
          }

          String msgText = ""; //old
          String errorText = null; //old

          String id = ""; //kurentostyle
          String response = ""; //kurentostyle

          if(json.has("msg")) msgText = json.getString("msg");
          if(json.has("error")) errorText = json.optString("error");
          if(json.has("id")) id = json.getString("id");

          if(id.equals("registerResponse")){

                response = json.getString("response"); //TODO if not accepted what todo?
                String message = json.getString("message");

                if(response.equals("rejected"))
                    reportError("register rejected: " + message);

                if(response.equals("skipped")) Log.e(TAG, "registration was skipped because: " + message);
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

              SessionDescription sdp = new SessionDescription(
                      SessionDescription.Type.OFFER,
                      json.getString("sdp"));

              events.onRemoteDescription(sdp);

              //if call state is busy reply with json reponse rejected because bussy

              //create confirm dialog (answer call from xy) if yes create webrtc connection and generateOffer for caller

              //if no reply with json response rejected because
          }

          if(id.equals("startCommunication")){
              Log.d(TAG, "startCommunication "+json.toString());

              SessionDescription sdp = new SessionDescription(
                      SessionDescription.Type.ANSWER,json.getString("sdp"));

              events.onRemoteDescription(sdp);

          }

          if(id.equals("stopCommunication")){
              Log.d(TAG, "stopCommunication "+json.toString());

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

          /*
          if (msgText.length() > 0) {

              json = new JSONObject(msgText);
              String type = json.optString("type");

          if (type.equals("candidate")) {

            IceCandidate candidate = new IceCandidate(
              json.getString("id"),
              json.getInt("label"),
              json.getString("candidate"));

              events.onRemoteIceCandidate(candidate);

          } else if (type.equals("answer")) {

              if (initiator) {
                SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type),
                json.getString("sdp"));
                events.onRemoteDescription(sdp);
          } else {
            reportError("Received answer for call initiator: " + msg);
          }
        } else if (type.equals("offer")) {
          if (!initiator) {
            SessionDescription sdp = new SessionDescription(
                SessionDescription.Type.fromCanonicalForm(type),
                json.getString("sdp"));
            events.onRemoteDescription(sdp);
          } else {
            reportError("Received offer for call receiver: " + msg);
          }
        } else if (type.equals("bye")) {
          events.onChannelClose();
        } else {
          reportError("Unexpected WebSocket message: " + msg);
        }
      } else {
        if (errorText != null && errorText.length() > 0) {
          reportError("WebSocket error message: " + errorText);
        } else {
          //reportError("Unexpected WebSocket message: " + msg);
        }
      }*/
    } catch (JSONException e) {
      reportError("WebSocket message JSON parsing error: " + e.toString());
    }
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
        if (roomState != ConnectionState.ERROR) {
          roomState = ConnectionState.ERROR;
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
