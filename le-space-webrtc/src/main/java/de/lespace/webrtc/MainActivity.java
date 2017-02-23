package de.lespace.webrtc;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import de.lespace.apprtc.activity.ConnectActivity;
import de.lespace.apprtc.RTCConnection;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    public Toast logToast;
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private String keyprefFrom;
    private String keyprefRoomServerUrl;
    String from = "";
    String wssUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        logAndToast("I have a client token:"+FacebookSdk.getClientToken());

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        keyprefFrom = getString(de.lespace.apprtc.R.string.pref_from_key);
        from = sharedPref.getString(keyprefFrom, getString(de.lespace.apprtc.R.string.pref_from_default));
        logAndToast("from:"+from);
        if(from != null && !from.equals("nandi")){
           // logAndToast("I have a client token:"+FacebookSdk.getClientToken());
            Intent i = new Intent(MainActivity.this, ConnectActivity.class);
            startActivity(i);
            finish();
        }

        setContentView(de.lespace.apprtc.R.layout.activity_main);


        final String wssUrl = sharedPref.getString(keyprefRoomServerUrl, getString(de.lespace.apprtc.R.string.pref_room_server_url_default));
        RTCConnection.wssUrl = wssUrl;
        RTCConnection.from = from;

        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton)findViewById(de.lespace.apprtc.R.id.login_button);
        callbackManager = CallbackManager.Factory.create();
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {

                from = loginResult.getAccessToken().getUserId();
                // Log.i(TAG,  "facebook login successful:"+from);
                logAndToast("facebook login successful:"+from);
                FacebookSdk.setClientToken(loginResult.getAccessToken().getToken());

               /* if(FacebookSdk.getClientToken()!=null){
                    logAndToast("I have a client token:"+FacebookSdk.getClientToken());
                }*/
                //use facebook user id as login name

                SharedPreferences saved_values = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor=saved_values.edit();
                editor.putString(keyprefFrom, from);

                boolean okey = editor.commit();

                RTCConnection.from = from;
                RTCConnection.wssUrl = wssUrl;
               // SignalingService.appRTCClient.reconnect();

                Intent i = new Intent(MainActivity.this, ConnectActivity.class);
                startActivity(i);
                finish();

            }

            @Override
            public void onCancel() {
                //  logAndToast( "facebook login canceled");

            }

            @Override
            public void onError(FacebookException e) {
                // logAndToast( "facebook login error:"+e.getMessage());
            }

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    // Log |msg| and Toast about it.
    public void logAndToast(String msg) {
        Log.d(TAG, msg);
        if(RTCConnection.doToast){
            if (logToast != null) {
                logToast.cancel();
            }
            logToast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
            logToast.show();
        }

    }
}
