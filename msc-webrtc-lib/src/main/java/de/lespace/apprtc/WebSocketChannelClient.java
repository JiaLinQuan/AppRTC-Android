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

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketState;

import de.lespace.apprtc.util.LooperExecutor;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;


/**
 * WebSocket client implementation.
 *
 * <p>All public methods should be called from a looper executor thread
 * passed in a constructor, otherwise exception will be thrown.
 * All events are dispatched on the same thread.
 */

public class WebSocketChannelClient {

  private static final String TAG = "WSChannelRTCClient";
  private static final int CLOSE_TIMEOUT = 1000;
  private final WebSocketChannelEvents events;
  private final LooperExecutor executor;
  private WebSocket ws;
  private String wsServerUrl;
  private String postServerUrl;
  private String from;
  private WebSocketConnectionState state;
  private final Object closeEventLock = new Object();
  private boolean closeEvent;
  // WebSocket send queue. Messages are added to the queue when WebSocket
  // client is not registered and are consumed in register() call.
  private final LinkedList<String> wsSendQueue;

  /**
   * Possible WebSocket connection states.
   */
  public enum WebSocketConnectionState {
    NEW, CONNECTED, REGISTERED, CLOSED, ERROR
  };

  /**
   * Callback interface for messages delivered on WebSocket.
   * All events are dispatched from a looper executor thread.
   */
  public interface WebSocketChannelEvents {
    public void onWebSocketMessage(final String message);
    public void onWebSocketClose();
    public void onWebSocketError(final String description);
  }

  public WebSocketChannelClient(LooperExecutor executor, WebSocketChannelEvents events) {
    this.executor = executor;
    this.events = events;
    from = null;
    wsSendQueue = new LinkedList<String>();
    state = WebSocketConnectionState.NEW;
  }


  public WebSocketFactory getFactory(boolean trustAll) {

    // Install the all-trusting trust manager
    try {

        SSLContext sslContext = SSLContext.getDefault();
        // Create a WebSocketFactory instance.
        WebSocketFactory factory = new WebSocketFactory();
        // Set the custom SSL context.
          if(trustAll) factory.setSSLContext(NaiveSSLContext.getInstance("TLS"));
          else
            factory.setSSLContext(sslContext);

        return factory;
    } catch (Exception e) {
      Log.i(TAG, "SSLContextProblem: " + e.getMessage() );
      e.printStackTrace();
      return null;
    }
  }

  public void setState(WebSocketConnectionState state){
    this.state = state;
  }

  public WebSocketConnectionState getState() {
    return state;
  }

  public boolean connect(final String wsUrl) {
      Log.i(TAG, "connect called: " + wsUrl);
    checkIfCalledOnValidThread();

    if (state != WebSocketConnectionState.NEW) {
      Log.e(TAG, "WebSocket is already connected.");
      return true;
    }

    wsServerUrl = wsUrl;
    closeEvent = false;

    Log.i(TAG, "Connecting WebSocket to: " + wsUrl );

    try {

          if(wsUrl.contains("192.168"))
             ws = getFactory(true).createSocket(wsUrl);
          else
              ws = getFactory(true).createSocket(wsUrl);

          ws.addListener(new WebSocketAdapter() {
          @Override
          public void onTextMessage(WebSocket websocket, final String message) throws Exception {
            Log.d(TAG, "WSS->C: " + message);
              executor.execute(new Runnable() {
                @Override
                public void run() {
                  if (state == WebSocketConnectionState.CONNECTED
                          || state == WebSocketConnectionState.REGISTERED) {
                    events.onWebSocketMessage(message);
                  }
                }
              });
          }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
          super.onConnected(websocket, headers);
          Log.d(TAG, "Status: Connected to " + wsUrl);
          Log.d(TAG, "WebSocket connection opened to: " + wsServerUrl);
          executor.execute(new Runnable() {
            @Override
            public void run() {
              state = WebSocketConnectionState.CONNECTED;
              RTCConnection.online = true;
              // Check if we have pending register request.
              if(state!=WebSocketConnectionState.REGISTERED) {
                RoomParametersFetcher roomParametersFetcher = new RoomParametersFetcher(ws);
                roomParametersFetcher.makeRequest();
              }
            }
          });
        }

        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
          super.onStateChanged(websocket, newState);

          Log.d(TAG, ("WebSocket connection closed. Code: " + newState.name()));

          synchronized (closeEventLock) {
            closeEvent = true;
            closeEventLock.notify();
          }
          executor.execute(new Runnable() {
            @Override
            public void run() {
              state = WebSocketConnectionState.CLOSED;
            }
          });
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
          super.onConnectError(websocket, exception);
          reportError("WebSocket onError : " + exception.getMessage());
          RTCConnection.online = false;
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
          super.onError(websocket, cause);
          reportError("WebSocket onError : " + cause.getMessage());
          RTCConnection.online = false;
        }

        @Override
        public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {
          super.onSendingHandshake(websocket, requestLine, headers);
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
          super.onUnexpectedError(websocket, cause);
          reportError("WebSocket onUnexpectedError : " + cause.getMessage());
        }
      });


      } catch (IOException e) {
      e.printStackTrace();
      reportError("WebSocket connection error: " + e.getMessage());
      return false;
    } ;

      try {
          ws.connect();
          ws.setPingInterval(60 * 1000);
      } catch (WebSocketException e) {
        e.printStackTrace();
        reportError("WebSocket connection error: " + e.getMessage());
      }
    return true;
  }

  public void register(final String from) {
    Log.d(TAG, "Registering room " + from);
    checkIfCalledOnValidThread();
    this.from = from;
   // this.clientID = clientID;
    if (state != WebSocketConnectionState.CONNECTED) {
      Log.w(TAG, "WebSocket register() in state " + state);
      return;
    }

    JSONObject json = new JSONObject();
    try {
        json.put("id", "register");
        json.put("name", from);
        Log.d(TAG, "C->WSS: " + json.toString());
        ws.sendText(json.toString());

        // Send any previously accumulated messages.
        for (String sendMessage : wsSendQueue) {
          send(sendMessage);
        }

        wsSendQueue.clear();
    } catch (JSONException e) {
      reportError("WebSocket register JSON error: " + e.getMessage());
    }
  }

  public void send(String message) {
    checkIfCalledOnValidThread();
    switch (state) {
      case NEW:
      case CONNECTED:
        // Store outgoing messages and send them after websocket client
        // is registered.
        Log.d(TAG, "WS ACC: " + message);
          ws.sendText(message);
        return;
      case ERROR:
      case CLOSED:
        Log.e(TAG, "WebSocket send() in error or closed state : " + message);
        return;
      case REGISTERED:
        Log.d(TAG, "C->WSS: " + message);
        ws.sendText(message);
        break;
    }
    return;
  }


  public void disconnect(boolean waitForComplete) {
    checkIfCalledOnValidThread();
      Log.d(TAG, "Disonnect WebSocket. State: " + state);
    if (state == WebSocketConnectionState.REGISTERED) {
      // Send "bye" to WebSocket server.
      send("{\"id\": \"stop\"}");
      state = WebSocketConnectionState.CONNECTED;
    }
    // Close WebSocket ERROR states only.
    if (state == WebSocketConnectionState.ERROR) {
      ws.disconnect();
      state = WebSocketConnectionState.CLOSED;

      // Wait for websocket close event to prevent websocket library from
      // sending any pending messages to deleted looper thread.
      if (waitForComplete) {
        synchronized (closeEventLock) {
          while (!closeEvent) {
            try {
              closeEventLock.wait(CLOSE_TIMEOUT);
              break;
            } catch (InterruptedException e) {
              Log.e(TAG, "Wait error: " + e.toString());
            }
          }
        }
      }
    }
    Log.d(TAG, "Disonnecting WebSocket done.");
  }

  private void reportError(final String errorMessage) {
    Log.e(TAG, errorMessage);
    executor.execute(new Runnable() {
    @Override
    public void run() {
      if (state != WebSocketConnectionState.ERROR) {
        state = WebSocketConnectionState.ERROR;
        events.onWebSocketError(errorMessage);
      }
    }
  });
}
   // Helper method for debugging purposes. Ensures that WebSocket method is
   // called on a looper thread.
  private void checkIfCalledOnValidThread() {
    if (!executor.checkOnLooperThread()) {
      throw new IllegalStateException(
          "WebSocket method is not called on valid thread");
    }
  }


}
