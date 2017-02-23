package de.lespace.apprtc.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


import de.lespace.apprtc.R;
import de.lespace.apprtc.RTCConnection;
import de.lespace.apprtc.service.SignalingService;

import static de.lespace.apprtc.RTCConnection.r;

public class IncomingCall extends Activity {

    private android.widget.Button btnHangup;
    private android.widget.Button btwAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_call);

        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

        if(alert == null){
            // alert is null, using backup
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // I can't see this ever being null (as always have a default notification)
            // but just incase
            if(alert == null) {
                // alert backup is null, using 2nd backup
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL);
            }
        }
        r = RingtoneManager.getRingtone(getApplicationContext(), alert);
        r.play();

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
            intent.putExtra(RTCConnection.EXTRA_FROM, RTCConnection.from);
            intent.putExtra(RTCConnection.EXTRA_INITIATOR, true);
            startActivity(intent);

            finish();
        }
    };
}
