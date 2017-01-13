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


import de.lespace.apprtc.WebSocketChannelClient.WebSocketChannelEvents;
import de.lespace.apprtc.WebSocketChannelClient.WebSocketConnectionState;
import de.lespace.apprtc.util.LooperExecutor;

import android.os.Handler;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class WebSocketRTCClient implements AppRTCClient {

    private static final String TAG = "WebSocketRTCClient";

    private final LooperExecutor executor;
    private WebSocketChannelClient wsClient;
    private WebSocketChannelEvents wsEvents;
    private SignalingEvents signalingEvents;
    private boolean queuing = false;

    public WebSocketRTCClient( WebSocketChannelEvents wsEvents, LooperExecutor executor) {
        this.executor = executor;
        this.wsEvents = wsEvents;
         executor.requestStart();
    }
    private List<JSONObject> signalingQueue = new ArrayList<JSONObject>();
    public void processSignalingQueue(){
        try {
        while(getSignalingQueue().size()>0){
            setQueuing(true);
            JSONObject json= getSignalingQueue().remove(0);
            String msg = json.toString();
            if (json.has("params")) {
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

                        wsClient.register(RTCConnection.roomConnectionParameters.from);
                    } catch (JSONException e) {
                      getSignalingEvents().onChannelError("app config JSON parsing error: " + e.toString());
                    }
                    setQueuing(false);
                 return;
            }

            if (wsClient.getState() != WebSocketConnectionState.REGISTERED && wsClient.getState() != WebSocketConnectionState.CONNECTED){
                Log.e(TAG, "websocket still in non registered state.");
                setQueuing(false);
                return;
            }


            String id = "";
            String response = "";

            if(json.has("id")) id = json.getString("id");

            if(id.equals("registerResponse")){

                response = json.getString("response"); //TODO if not accepted what todo?
                String message = json.getString("message");

                if(response.equals("accepted"))      {
                    wsClient.setState(WebSocketConnectionState.REGISTERED);
                }

                else if(response.equals("rejected"))      {
                    getSignalingEvents().onChannelError("register rejected: " + message);
                }

                else if(response.equals("skipped")) {
                    getSignalingEvents().onChannelError("register rejected: " + message);                                                                       // Log.e(TAG, "registration was skipped because: "+message);
                }
            }

            if(id.equals("ping")){
                if(getSignalingEvents()!=null)  getSignalingEvents().onPing();
            }

            if(id.equals("registeredUsers")){
                response = json.getString("response");
                getSignalingEvents().onUserListUpdate(response);
            }

            if(id.equals("incomingCall")){
                Log.d(TAG, "incomingCall "+json.toString());
                getSignalingEvents().onIncomingCall(json.getString("from"),json.has("screensharing"));
            }

            if(id.equals("incomingScreenCall")){
                Log.d(TAG, "incomingScreenCall "+json.toString());
                getSignalingEvents().onIncomingScreenCall(json);
            }

            if(id.equals("callResponse")){
                response = json.getString("response");

                if(response.startsWith("rejected")) {
                    Log.d(TAG, "call got rejected: "+response);
                    getSignalingEvents().onChannelClose();
                }else{
                    Log.d(TAG, "sending sdpAnswer: "+response);
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.ANSWER,json.getString("sdpAnswer"));

                    getSignalingEvents().onRemoteDescription(sdp);
                }
            }

            if(id.equals("callScreenResponse")){
                response = json.getString("response");

                if(response.startsWith("rejected")) {
                    Log.d(TAG, "call got rejected: "+response);
                    getSignalingEvents().onChannelScreenClose();
                }else{
                    Log.d(TAG, "sending sdpAnswer: "+response);
                    SessionDescription sdp = new SessionDescription(
                            SessionDescription.Type.ANSWER,json.getString("sdpAnswer"));

                    getSignalingEvents().onRemoteScreenDescription(sdp);
                }
            }

            if(id.equals("startCommunication")){
                Log.d(TAG, "startCommunication "+json.toString());
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,json.getString("sdpAnswer"));
                getSignalingEvents().onStartCommunication(sdp);
            }

            if(id.equals("startScreenCommunication")){
                Log.d(TAG, "startScreenCommunication "+json.toString());
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,json.getString("sdpAnswer"));
                   // signalingEvents.onStartScreenCommunication(sdp); //remove if not needed!
                getSignalingEvents().onStartScreenCommunication(sdp);
            }

            if(id.equals("stopCommunication")){
                Log.d(TAG, "stopCommunication "+json.toString());

                getSignalingEvents().onChannelClose();
                if(json.has("callback")){
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 3000ms
                            getSignalingEvents().onCallback();
                        }
                    }, 3000);

                }
            }

            if(id.equals("stopScreenCommunication")){
                Log.d(TAG, "stopCommunication "+json.toString());
                getSignalingEvents().onChannelScreenClose();
            }

            if(id.equals("iceCandidateScreen")){

                JSONObject candidateJson = json.getJSONObject("candidate");

                IceCandidate candidate = new IceCandidate(
                        candidateJson.getString("sdpMid"),
                        candidateJson.getInt("sdpMLineIndex"),
                        candidateJson.getString("candidate"));

                getSignalingEvents().onRemoteScreenIceCandidate(candidate);

            }

            if(id.equals("iceCandidate")){
                Log.d(TAG, "iceCandidate "+json.toString());

                JSONObject candidateJson = json.getJSONObject("candidate");

                IceCandidate candidate = new IceCandidate(
                        candidateJson.getString("sdpMid"),
                        candidateJson.getInt("sdpMLineIndex"),
                        candidateJson.getString("candidate"));

                getSignalingEvents().onRemoteIceCandidate(candidate);
            }

            if (id.equals("stop")) {
                getSignalingEvents().onChannelClose();
            }

            if (id.equals("callback")) {
                getSignalingEvents().onChannelClose();
            }

            if (id.equals("stopScreen")) {
                getSignalingEvents().onChannelScreenClose();
            }
        }
        setQueuing(false);
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
      RTCConnection.roomConnectionParameters = connectionParameters;
    executor.execute(new Runnable() {
      @Override
      public void run() {
          try {
              connectToWebsocketInternal();
          }catch(Exception e){
              reportError("WebSocketerror: " + e.toString());
          }
      }
    });
  }

    public void sendStopToPeer(){
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonMessage = new JSONObject();
                    jsonPut(jsonMessage, "id" , "stop");

                    wsClient.send(jsonMessage.toString());
                }catch(Exception e){
                    reportError("WebSocketerror: " + e.toString());
                }
            }
        });
    }

    @Override
  public void sendDisconnectToPeer() {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            disconnectFromRoomInternal();
          }
        });
  }

    public void sendPong() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonMessage = new JSONObject();
                    jsonPut(jsonMessage, "id" , "pong");

                    wsClient.send(jsonMessage.toString());
                }catch(Exception e){
                    reportError("WebSocketerror: " + e.toString());
                }
            }
        });
    }

    public void sendCallback() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonMessage = new JSONObject();
                    jsonPut(jsonMessage, "id" , "callback");

                    wsClient.send(jsonMessage.toString());
                }catch(Exception e){
                    reportError("WebSocketerror: " + e.toString());
                }
            }
        });
    }

    public void resetWebsocket(){
            wsClient = null;
    }

    @Override
    public void reconnect() {
        executor.execute(new Runnable() {
            @Override
            public void run() {

                disconnectFromRoomInternal();

                try {
                    connectToWebsocketInternal();
                }catch(Exception e){
                    reportError("WebSocketerror: " + e.toString());
                }
            }
        });
    }

      // Connects to websocket - function runs on a local looper thread.
      private void connectToWebsocketInternal() {

          String connectionUrl = getConnectionUrl(RTCConnection.roomConnectionParameters);
         // wsClient.setState(WebSocketConnectionState.NEW);
          wsClient = new WebSocketChannelClient(executor,wsEvents);
          wsClient.connect(connectionUrl);
          wsClient.setState(WebSocketConnectionState.CONNECTED);
          Log.d(TAG, "wsClient connect " + connectionUrl);

      }

      // Disconnect from room and send bye messages - runs on a local looper thread.
      private void disconnectFromRoomInternal() {
          executor.execute(new Runnable() {
              @Override
              public void run() {
                     if (wsClient!=null && wsClient.getState() !=null && (wsClient.getState() == WebSocketConnectionState.CONNECTED
                            || wsClient.getState() == WebSocketConnectionState.NEW
                            || wsClient.getState() == WebSocketConnectionState.REGISTERED)) {
                        Log.d(TAG, "Closing room.");
                        JSONObject jsonMessage = new JSONObject();
                        jsonPut(jsonMessage, "id" , "stop");
                        if(wsClient!=null) { //in case of reconnect in the beginning wsClient is null
                            wsClient.send(jsonMessage.toString());
                            wsClient.disconnect(true);
                        }
                    }
              }
          });
      }

      // Helper functions to get connection, sendSocketMessage message and leave message URLs
      private String getConnectionUrl(RoomConnectionParameters connectionParameters) {
            return connectionParameters.wssUrl +"/ws";
      }

    // Return the list of ICE servers described by a WebRTCPeerConnection
    // configuration string.
    private LinkedList<PeerConnection.IceServer> iceServersFromPCConfigJSON(String pcConfig) throws JSONException {
        JSONObject json = new JSONObject(pcConfig);
        Log.d(TAG, "current pcConfig: " + pcConfig);
        LinkedList<PeerConnection.IceServer> iceServers = new LinkedList<PeerConnection.IceServer>();
        JSONArray iceServersArray = json.getJSONArray("iceServers");
        for (int i = 0; i < iceServersArray.length(); i++) {
            JSONObject iceJson  = iceServersArray.getJSONObject(i);

            String username = iceJson.getString("username");
            String password = iceJson.getString("password");
            JSONArray iceUris = iceJson.getJSONArray("urls");

            for (int j = 0; j < iceUris.length(); j++) {
                String uri = iceUris.getString(j);
                Log.d(TAG, "adding ice server: " + uri + " username:" + username + " password:" + password);
                iceServers.add(new PeerConnection.IceServer(uri, username, password));
            }
        }
        return iceServers;
    }

      public void call(final SessionDescription sdp)  {
          executor.execute(new Runnable() {
              @Override
              public void run() {
                  if (wsClient.getState() != WebSocketConnectionState.REGISTERED) {
                      reportError("Sending offer SDP in non registered state.");
                      return;
                  }

                  JSONObject json = new JSONObject();

                  jsonPut(json,"id","call");
                  jsonPut(json,"from",RTCConnection.roomConnectionParameters.from);
                  jsonPut(json,"to",RTCConnection.roomConnectionParameters.to);
                  jsonPut(json, "sdpOffer", sdp.description);
                  wsClient.send(json.toString());

              }
          });
      }

      // Send local answer SDP to the other participant.
      @Override
      public void sendOfferSdp(final SessionDescription sdp, final boolean isScreenSharing) {
        executor.execute(new Runnable() {
          @Override
          public void run() {

              JSONObject json = new JSONObject();
              if(!isScreenSharing) jsonPut(json, "id","incomingCallResponse");
              else jsonPut(json, "id","incomingScreenCallResponse");
              jsonPut(json, "from", RTCConnection.roomConnectionParameters.to);
              jsonPut(json, "callResponse",  "accept");
              jsonPut(json, "sdpOffer", sdp.description);
              wsClient.send(json.toString());
          }
        });
      }

      // Send Ice candidate to the other participant.
      @Override
      public void sendLocalIceCandidate(final IceCandidate candidate,  final boolean isScreenSharing) {
        executor.execute(new Runnable() {
          @Override
          public void run() {

            JSONObject json = new JSONObject();

              if(!isScreenSharing) jsonPut(json, "id", "onIceCandidate");
              else  jsonPut(json, "id","onIceCandidateScreen");
              jsonPut(json, "candidate", candidate.sdp);
              jsonPut(json, "sdpMid", candidate.sdpMid);
              jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);
              // Call receiver sends ice candidates to websocket server.
              wsClient.send(json.toString());
          }
        });
      }


  // --------------------------------------------------------------------
  // Helper functions.
  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (wsClient!=null && wsClient.getState()!=null && wsClient.getState() != WebSocketConnectionState.ERROR) {
                wsClient.setState(WebSocketConnectionState.ERROR);
                getSignalingEvents().onChannelError(errorMessage);
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

    public boolean isQueuing() {
        return queuing;
    }

    public void setQueuing(boolean queuing) {
        this.queuing = queuing;
    }

    public List<JSONObject> getSignalingQueue() {
        return signalingQueue;
    }

    public void setSignalingQueue(List<JSONObject> signalingQueue) {
        this.signalingQueue = signalingQueue;
    }

    public SignalingEvents getSignalingEvents() {
        return signalingEvents;
    }

    public void setSignalingEvents(SignalingEvents signalingEvents) {
        this.signalingEvents = signalingEvents;
    }
}
