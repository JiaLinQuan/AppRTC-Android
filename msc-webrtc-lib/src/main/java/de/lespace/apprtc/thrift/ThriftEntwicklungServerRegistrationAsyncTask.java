package de.lespace.apprtc.thrift;


import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.thrifty.protocol.BinaryProtocol;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.service.ClientBase;
import com.microsoft.thrifty.service.ServiceMethodCallback;
import com.microsoft.thrifty.transport.SocketTransport;
import com.microsoft.thrifty.transport.Transport;

import java.io.IOException;

import de.lespace.apprtc.firebase.MyFirebaseMessagingService;
import de.lespace.apprtc.service.SignalingService;


/**
 * Created by nico on 18.02.17.
 */

public class ThriftEntwicklungServerRegistrationAsyncTask extends AsyncTask<String, Void, RegisterUserId> {

    private static final String TAG = "TESR";
    public static String userId, firebaseToken;
    public SignalingService service;

    @Override
    protected RegisterUserId doInBackground(String... params) {
        Log.i(TAG, "executing: firebaseToken:"+firebaseToken+" userId:"+userId);
        try {
            if(firebaseToken!=null && userId!=null){
                String server = ThriftCallAsyncTask.SERVER_IP;
                int port = 9090;
                // Transports define how bytes move to and from their destination
                SocketTransport transport =  new SocketTransport.Builder(server, port).readTimeout(2000).build();

                transport.connect();
                // Protocols define the mapping between structs and bytes
                Protocol protocol = new BinaryProtocol(transport);

                ClientBase.Listener listener = new ClientBase.Listener() {
                    @Override
                    public void onTransportClosed() {
                        Log.e(TAG, "onTransportClosed:");
                    }

                    @Override
                    public void onError(Throwable error) {

                        throw new AssertionError(error);
                    }
                };

                Webrtc client = new WebrtcClient(protocol,listener);

                RegisterUserId registration = new RegisterUserId.Builder().userId(userId).firebaseToken(firebaseToken).build();

                client.registerUserId(registration, new ServiceMethodCallback<RegisterResult>() {
                    @Override
                    public void onSuccess(RegisterResult result) {
                        Log.i(TAG, "ThriftEntwicklungServerRegistrationAsyncTask: success! result:"+result);
                        service.startService(new Intent(service,MyFirebaseMessagingService.class));
                       // service.stopSelf();
                    }

                    @Override
                    public void onError(Throwable error) {
                        service.stopSelf();
                        Log.e(TAG,error.getMessage());
                    }
                });
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
