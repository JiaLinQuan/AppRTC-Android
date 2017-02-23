package de.lespace.apprtc.thrift;


import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.thrifty.protocol.BinaryProtocol;
import com.microsoft.thrifty.protocol.Protocol;
import com.microsoft.thrifty.service.ClientBase;
import com.microsoft.thrifty.service.ServiceMethodCallback;
import com.microsoft.thrifty.transport.SocketTransport;

import java.io.IOException;

/**
 * Created by nico on 18.02.17.
 */

public class ThriftCallAsyncTask extends AsyncTask<String, Void, CallResult> {

    private static final String TAG = "TCAT";
    public static final String SERVER_IP = "192.168.43.151"; //"192.168.43.151"
    @Override
    protected CallResult doInBackground(String... params) {
        Log.i(TAG, "calling from:"+params[0]+" to:"+params[1]+" over MSC");
        try {

            // Transports define how bytes move to and from their destination
           // String server = "192.168.43.151";//"172.20.10.6"; //"192.168.43.151"
            int port = 9090;
            // Transports define how bytes move to and from their destination
            SocketTransport transport =  new SocketTransport.Builder(SERVER_IP, port).readTimeout(2000).build();
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
                Call call = new Call.Builder().fromName(params[0]).toName(params[1]).fromUUID(params[2]).toUUID(params[3]).build();
                client.call(call, new ServiceMethodCallback<CallResult>() {
                    @Override
                    public void onSuccess(CallResult result) {
                        Log.i(TAG, "success! result:"+result.response);
                    }

                    @Override
                    public void onError(Throwable error) {
                        Log.e(TAG, "success! result:"+error.getMessage());
                    }
                });


        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
