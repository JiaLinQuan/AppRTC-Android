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

import org.appspot.apprtc.util.LooperExecutor;

import android.util.Log;

import org.appspot.apprtc.RoomParametersFetcher.RoomParametersFetcherEvents;

import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.LinkedList;
import org.java_websocket.client.WebSocketClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.X509Certificate;

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
  private WebSocketClient ws;
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
  public void trustAllHosts() {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {

      }

      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[]{};
      }

    }};

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      ws.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sc));
    } catch (Exception e) {
      e.printStackTrace();
    }
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
        ws = new WebSocketClient(new URI(wsUrl), new Draft_17()) {
        @Override
        public void onOpen(ServerHandshake handshakedata) {
          Log.d(TAG, "Status: Connected to " + wsUrl);
          Log.d(TAG, "WebSocket connection opened to: " + wsServerUrl);
          executor.execute(new Runnable() {
            @Override
            public void run() {
              state = WebSocketConnectionState.CONNECTED;
              // Check if we have pending register request.
              if(state!=WebSocketConnectionState.REGISTERED) {
                RoomParametersFetcher roomParametersFetcher = new RoomParametersFetcher(ws);
                roomParametersFetcher.makeRequest();
              }
            }
          });
        }

        @Override
        public void onMessage(final String message) {
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
        public void onClose(int code, String reason, boolean remote) {

          Log.d(TAG, (remote?"Remote(!) ":"")+"WebSocket connection closed. Code: " + code + ". Reason: " + reason + ". State: " + state);
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
        public void onError(Exception ex) {
          reportError("WebSocket connection error: " + ex.getMessage());
        }
      };
      trustAllHosts();
      ws.connect();
/*
      // load up the key store
      String STORETYPE = "JKS";
      String KEYSTORE = "keystore.jks";
      String STOREPASSWORD = "storepassword";
      String KEYPASSWORD = "keypassword";

      KeyStore ks = KeyStore.getInstance( STORETYPE );
      File kf = new File( KEYSTORE );

      try {
          ks.load( new FileInputStream( kf ), STOREPASSWORD.toCharArray() );
          KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
          kmf.init( ks, KEYPASSWORD.toCharArray() );
          TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
          tmf.init( ks );

          SSLContext sslContext = null;
          sslContext = SSLContext.getInstance( "TLS" );
          sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );

          SSLSocketFactory factory = sslContext.getSocketFactory();// (SSLSocketFactory) SSLSocketFactory.getDefault();
          ws.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));
          ws.connectBlocking();

          BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );
          while ( true ) {
            String line = reader.readLine();
            if( line.equals( "close" ) ) {
              ws.close();
            } else {
              ws.send( line );
            }
          }

      } catch (IOException e) {
        reportError("WebSocket connection error: " + e.getMessage());
      } catch (NoSuchAlgorithmException e) {
        reportError("WebSocket connection error: " + e.getMessage());
      } catch (CertificateException e) {
        reportError("WebSocket connection error: " + e.getMessage());
      } catch (InterruptedException e) {
        reportError("WebSocket connection error: " + e.getMessage());
      } catch (UnrecoverableKeyException e) {
        reportError("WebSocket connection error: " + e.getMessage());
      } catch (KeyStoreException e) {
        reportError("WebSocket connection error: " + e.getMessage());
      } catch (KeyManagementException e) {
        reportError("WebSocket connection error: " + e.getMessage());
      } */

    } catch (URISyntaxException e) {
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
        ws.send(json.toString());

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
       // wsSendQueue.add(message);
        ws.send(message);
        return;
      case ERROR:
      case CLOSED:
        Log.e(TAG, "WebSocket send() in error or closed state : " + message);
        return;
      case REGISTERED:
        JSONObject json = new JSONObject();
        // json.put("cmd", "send");
        // json.put("msg", message);
        //message = json.toString();
        Log.d(TAG, "C->WSS: " + message);
        ws.send(message);
        break;
    }
    return;
  }

  // This call can be used to send WebSocket messages before WebSocket
  // connection is opened.
  public void sendSocketMessage(String message) {
    checkIfCalledOnValidThread();
    ws.send(message);
  }

  public void disconnect(boolean waitForComplete) {
    checkIfCalledOnValidThread();
    Log.d(TAG, "Disonnect WebSocket. State: " + state);
    if (state == WebSocketConnectionState.REGISTERED) {
      // Send "bye" to WebSocket server.
      send("{\"id\": \"stop\"}");
      state = WebSocketConnectionState.CONNECTED;
      // Send http DELETE to http WebSocket server.
      //TODO sendWSSMessage("DELETE", "");
    }
    // Close WebSocket in CONNECTED or ERROR states only.
    if (state == WebSocketConnectionState.CONNECTED  || state == WebSocketConnectionState.ERROR) {
      ws.close();
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
