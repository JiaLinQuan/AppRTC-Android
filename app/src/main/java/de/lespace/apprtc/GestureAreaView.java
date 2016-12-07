package de.lespace.apprtc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class GestureAreaView extends View  {

	private GestureDetector gestures;
	private Matrix translate;
	private Bitmap droid;

	public GestureAreaView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init(context);
	}

	public GestureAreaView(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context);
	}

	public GestureAreaView(Context context) {
		super(context);

		init(context);
	}

	private void init(Context c) {

		MyGestureListener listener = new MyGestureListener(this);
		gestures = new GestureDetector(c, listener, null, true);

		//droid = BitmapFactory.decodeResource(getResources(), R.drawable.droid);
		translate = new Matrix();
	}

	// The onTouchEvent() method must be overridden to pass the MotionEvent data
	// to the gesture detector for processing.
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		boolean retVal = false;
		retVal = gestures.onTouchEvent(event);
		return retVal;
	}

	// The onDraw() method must be overridden to draw the bitmap graphic in the
	// appropriate position at any time
	/*@Override
	protected void onDraw(Canvas canvas) {

	//	canvas.drawBitmap(droid, translate, null);
	}*/

	public void move(float dx, float dy) {
		translate.postTranslate(dx, dy);
		invalidate();
	}

	public void resetLocation() {
		translate.reset();
		invalidate();
	}

}
