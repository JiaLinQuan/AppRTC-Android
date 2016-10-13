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

import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketState;

import org.appspot.apprtc.util.LooperExecutor;
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

/*
  public class OwnSSLWebSocketClientFactory implements WebSocketClient.WebSocketClientFactory {

    protected SSLContext sslcontext;
    protected ExecutorService exec;

    public OwnSSLWebSocketClientFactory( SSLContext sslContext ) {
      this( sslContext, Executors.newSingleThreadScheduledExecutor() );
    }

    public OwnSSLWebSocketClientFactory( SSLContext sslContext , ExecutorService exec ) {
      if( sslContext == null || exec == null )
        throw new IllegalArgumentException();
      this.sslcontext = sslContext;
      this.exec = exec;
    }

    public SSLSocketFactory setSSLSocketFactory(){
      return this.sslcontext.getSocketFactory();
    }

    @Override
    public ByteChannel wrapChannel(SocketChannel channel, SelectionKey key, String host, int port ) throws IOException {
      SSLEngine e = sslcontext.createSSLEngine( host, port );
      e.setUseClientMode( true );
      return new SSLSocketChannel2( channel, e, exec, key );
    }

    @Override
    public WebSocketImpl createWebSocket(WebSocketAdapter a, Draft d, Socket c ) {
      return new WebSocketImpl( a, d, c );
    }

    @Override
    public WebSocketImpl createWebSocket(WebSocketAdapter a, List<Draft> d, Socket s ) {
      return new WebSocketImpl( a, d, s );
    }

  }
  public class TLSSocketFactory extends SSLSocketFactory  {

    private SSLSocketFactory internalSSLSocketFactory;

    public TLSSocketFactory() throws KeyManagementException, NoSuchAlgorithmException {
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(null, null, null);
      internalSSLSocketFactory = context.getSocketFactory();
    }

    @Override
    public String[] getDefaultCipherSuites() {
      return new String[]{"RC4-MD5", "DES-CBC-SHA", "DES-CBC3-SHA"};
     // return internalSSLSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
      return new String[]{"RC4-MD5", "DES-CBC-SHA", "DES-CBC3-SHA"};
     ///   return internalSSLSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
      return enableTLSOnSocket(internalSSLSocketFactory.createSocket(address, port, localAddress, localPort));
    }

    private Socket enableTLSOnSocket(Socket socket) {
      if(socket != null && (socket instanceof SSLSocket)) {
        ((SSLSocket)socket).setEnabledProtocols(new String[] {"TLSv1.1", "TLSv1.2"});
      }
      return socket;
    }
  }*/

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


  public boolean trustAllHosts() {

    // Install the all-trusting trust manager
    try {
     // SSLContext sslContext = SSLContext.get
      SSLContext sslContext = SSLContext.getDefault();

      //sslContext.getSocketFactory().
     // sslContext
      /*String ciphers[] = sslContext.getSocketFactory().getDefaultCipherSuites();
      for(int x = 0;x<ciphers.length;x++){
        Log.i(TAG, "DefaultCipherSuites: " + ciphers[x]);
      }*/


      // Create a WebSocketFactory instance.
      WebSocketFactory factory = new WebSocketFactory();
// Create a custom SSL context.
      //SSLContext context = NaiveSSLContext.getInstance("TLS"); //siehe https://gist.github.com/TakahikoKawasaki/d07de2218b4b81bf65ac

// Set the custom SSL context.
      factory.setSSLContext(sslContext);


      return true;
    } catch (Exception e) {
      Log.i(TAG, "SSLContextProblem: " + e.getMessage() );
      e.printStackTrace();
      return false;
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
          ws = new WebSocketFactory().createSocket(wsUrl);

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
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
          super.onError(websocket, cause);
          reportError("WebSocket onError : " + cause.getMessage());
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
        JSONObject json = new JSONObject();
        // json.put("cmd", "send");
        // json.put("msg", message);
        //message = json.toString();
        Log.d(TAG, "C->WSS: " + message);
        ws.sendText(message);
        break;
    }
    return;
  }

  // This call can be used to send WebSocket messages before WebSocket
  // connection is opened.
  public void sendSocketMessage(String message) {
    checkIfCalledOnValidThread();
    ws.sendText(message);
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
