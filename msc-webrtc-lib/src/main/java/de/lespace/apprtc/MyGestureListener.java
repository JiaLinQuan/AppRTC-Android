package de.lespace.apprtc;

import android.view.GestureDetector;
import android.view.MotionEvent;

import de.lespace.apprtc.activity.GestureAreaView;


/**
 * Created by nico on 25.11.16.
 */

public class MyGestureListener  extends
        GestureDetector.SimpleOnGestureListener {

    GestureAreaView view;
    public PeerConnectionClient peerConnectionClient;

    public MyGestureListener(GestureAreaView view) {
        this.view = view;
        this.peerConnectionClient = PeerConnectionClient.getInstance(0); //get standard instance in case we have two
    }

    // onDown: Called when the user first presses on the touch screen.
    @Override
    public boolean onDown(MotionEvent e) {

        return true;
    }

    // onShowPress: Called after the user first presses the touch screen but
    // before he lifts his finger or moves it around on the screen;
    // used to visually or audibly indicate that the press has been detected
    @Override
    public void onShowPress(MotionEvent e) {
        // TODO Auto-generated method stub
        super.onShowPress(e);
    }

    // onSingleTapUp: Called when the user lifts up (using the up
    // MotionEvent) from the touch screen as part of a single-tap event.
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // TODO Auto-generated method stub
        return super.onSingleTapUp(e);
    }

    // onSingleTapConfirmed: Called when a single-tap event occurs.
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // TODO Auto-generated method stub
        return super.onSingleTapConfirmed(e);
    }

    // onDoubleTap: Called when a double-tap event occurs.
    @Override
    public boolean onDoubleTap(MotionEvent e) {

       // Toast.makeText(view.getContext(), "onDoubleTapEvent",
          //      Toast.LENGTH_SHORT).show();

        this.peerConnectionClient.switchCamera();

        view.resetLocation();

        return true;
    }

    // onDoubleTapEvent: Called when an event within a double-tap gesture
    // occurs,
    // including any down, move, or up MotionEvent.
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {

        // TODO Auto-generated method stub
        return super.onDoubleTapEvent(e);
    }

    // onLongPress: Similar to onSingleTapUp, but called if the user holds
    // down his
    // finger long enough to not be a standard click but also without any
    // movement
    @Override
    public void onLongPress(MotionEvent e) {
      //  Toast.makeText(view.getContext(), "onLongPress", Toast.LENGTH_SHORT).show();

    }

    // onScroll: Called after the user presses and then moves his finger in
    // a steady
    // motion before lifting his finger.This is commonly called dragging.
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2,
                            float distanceX, float distanceY) {

        view.move(-distanceX, -distanceY);

        return true;
    }

    // onFling: Called after the user presses and then moves his finger in
    // an accelerating
    // motion before lifting it.This is commonly called a flick gesture and
    // usually results in
    // some motion continuing after the user lifts his finger
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {



      //  myGestureEvents.onFling();
        return true;
    }



}
