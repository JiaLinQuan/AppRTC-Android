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

import org.appspot.apprtc.AppRTCClient.SignalingParameters;
import org.appspot.apprtc.util.AsyncHttpURLConnection;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * AsyncTask that converts an AppRTC room URL into the set of signaling
 * parameters to use with that room.
 */
public class RoomParametersFetcher {
  private static final String TAG = "RoomParametersFetcher";
  private static final int TURN_HTTP_TIMEOUT_MS = 5000;
  private final RoomParametersFetcherEvents events;
  private final String websocketUrl;
  private final WebSocketChannelClient wsClient;
  private AsyncHttpURLConnection httpConnection;

  /**
   * Room parameters fetcher callbacks.
   */
  public static interface RoomParametersFetcherEvents {
    /**
     * Callback fired once the room's signaling parameters
     * SignalingParameters are extracted.
     */
    public void onSignalingParametersReady(final SignalingParameters params);

    /**
     * Callback for room parameters extraction error.
     */
    public void onSignalingParametersError(final String description);
  }

  public RoomParametersFetcher(String websocketUrl, WebSocketChannelClient wsClient,
                               final RoomParametersFetcherEvents events) {
    this.websocketUrl = websocketUrl;
    this.wsClient = wsClient;
    this.events = events;
  }

  public void makeRequest() {
      Log.d(TAG, "makeRequest to room: " + websocketUrl);

      JSONObject json = new JSONObject();
      WebSocketRTCClient.jsonPut(json, "id", "appConfig");
      //WebSocketRTCClient.jsonPut(json, "roomId", roomId);

      wsClient.post(json.toString());
      Log.d(TAG, "made json request " + json.toString()+" ws:"+wsClient.getState());
  }

  public void roomHttpResponseParse(String response) {
    Log.i(TAG, "Room response: " + response);
    try {

        LinkedList<IceCandidate> iceCandidates = null;
        SessionDescription offerSdp = null;
        JSONObject roomJson = new JSONObject(response);

        String result = roomJson.getString("result");
        Log.i(TAG, "client debug ");
        if (!result.equals("SUCCESS")) {
            events.onSignalingParametersError("Room response error: " + result);
            return;
        }

        response = roomJson.getString("params");
        roomJson = new JSONObject(response);

        boolean initiator = (roomJson.getBoolean("is_initiator"));

        if (!initiator) {
            iceCandidates = new LinkedList<IceCandidate>();
            String messagesString = roomJson.getString("messages");
            JSONArray messages = new JSONArray(messagesString);
            for (int i = 0; i < messages.length(); ++i) {
              String messageString = messages.getString(i);
              JSONObject message = new JSONObject(messageString);
              String messageType = message.getString("type");
              Log.d(TAG, "GAE->C #" + i + " : " + messageString);
              if (messageType.equals("offer")) {
                offerSdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(messageType),
                    message.getString("sdp"));
              } else if (messageType.equals("candidate")) {
                IceCandidate candidate = new IceCandidate(
                    message.getString("id"),
                    message.getInt("label"),
                    message.getString("candidate"));
                iceCandidates.add(candidate);
              } else {
                Log.e(TAG, "Unknown message: " + messageString);
              }
            }
        }

      Log.d(TAG, "Initiator: " + initiator);

      LinkedList<PeerConnection.IceServer> iceServers =
          iceServersFromPCConfigJSON(roomJson.getString("pc_config"));

    /*  boolean isTurnPresent = false;
      for (PeerConnection.IceServer server : iceServers) {
        Log.d(TAG, "IceServer: " + server);
        if (server.uri.startsWith("turn:")) {
          isTurnPresent = true;
          break;
        }
      }*/
      // Request TURN servers.
    //  if (!isTurnPresent) requestTurnServers();


        SignalingParameters params = new SignalingParameters(
              iceServers, initiator, websocketUrl,
              offerSdp, iceCandidates);
          events.onSignalingParametersReady(params);
        } catch (JSONException e) {
          events.onSignalingParametersError(
              "Room JSON parsing error: " + e.toString());
        }
  }

  // Requests & returns a TURN ICE Server based on a request URL.  Must be run
  // off the main thread!
  private void requestTurnServers()
      throws IOException, JSONException {
      Log.d(TAG, "Request TURN from websocket: ");
      JSONObject json = new JSONObject();
      WebSocketRTCClient.jsonPut(json, "id", "turn");
      wsClient.post(json.toString());
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

  // Return the contents of an InputStream as a String.
  private static String drainStream(InputStream in) {
    Scanner s = new Scanner(in).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

}
