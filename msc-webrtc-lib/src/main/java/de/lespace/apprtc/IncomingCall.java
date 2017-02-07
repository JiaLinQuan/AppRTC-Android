package de.lespace.apprtc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


import static de.lespace.apprtc.RTCConnection.CONNECTION_REQUEST;
import static de.lespace.apprtc.RTCConnection.r;

public class IncomingCall extends Activity {

    private android.widget.Button btnHangup;
    private android.widget.Button btwAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        btnHangup = (Button) findViewById(R.id.btnHangup);
        btwAnswer = (Button) findViewById(R.id.btnAnswer);
        btnHangup.setOnClickListener(hangupListenr);
        btwAnswer.setOnClickListener(answerListener);
    }

    /**
     * Hangup Connection - send stop to peer
     */
    private final View.OnClickListener hangupListenr = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            r.stop();
            SignalingService.appRTCClient.sendStopToPeer();
            finish();
        }
    };

    private final View.OnClickListener answerListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            r.stop();

            Intent intent = new Intent(getApplicationContext(), ConnectActivity.class);
            intent.putExtra(RTCConnection.EXTRA_TO, RTCConnection.to);
            intent.putExtra(RTCConnection.EXTRA_INITIATOR, false);
            startActivity(intent);


            finish();
        }
    };
}
