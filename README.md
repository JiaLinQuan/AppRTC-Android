# AppRTCDemo - Android

##About
This project is a open source Android webrtc video chat which can be easily integrated in any website. E.g. you can add improve your customer support by adding this widget to your website.

##Documentation
This WebRTC Android App simply connects to a Java Webrtc Signaling Server via Websocket Protokoll and lists connected users which can be called via Videophone. It is a WebRTC ready prototype for integration into other apps which want to implement WebRTC Videocalls e.g. for their Sales and Support team. 

##Installation
1. git clone this repository and open it in Android Studio
2. AppRTC-Kurento (Signaling Server) must be running already
3. STUN-TURN Server should be running, if you run the project outside your local LAN.
4. connect your Android phone via USB and deploy and start android app from Android Studio 
5. in the app settings (top right corner of the running app) 
	- change the Websocket-URL according to the URL of your signaling server 
		e.g. wss://webrtcsignaling-server/jWebrtc (secure websocket - wss:// insecure with ws:// )
		 you can change the default values in  strings.xml  ```<string name="pref_room_server_url_default" translatable="false">wss://nicokrause.com/jWebrtc</string>``
	- enter your favourite username which should register on signaling server and should be visible and reachable by the peers
6. open chrome and/or android browser to https://webrtcsignaling-server/jWebrtc and register another user
7. choose a registered user on phone or browser and call.

##Code-Instructions
- the apps main activities are:
	ConnectActivity - the main screen - displays connected users
	CallActivity - the screen which has the video and their controls

- all websocket communication is done in 
	WebsocketChannelClient - creates, connects, registeres and closes the websocket to SignalingServer 
	WebsocketRTCClient - receives WebRTC - signaling messages and handles them accordingly

- WebRTC peerconnection is done in PeerConnectionClient

##Common Mistakes
- (production) wrong url for webservice.
- (development) smartphone not in same network as browser, media kurento server 
- (development) kurento server not running?

##Todo/Bugs
- when receiving "rejected" do not send channelclose to other party and display reject message"
- display my userId
- display status for connection (connecting,online,error)
- generate code for blog ```<iframe /> or <script>```

#Improvements and Research
- (improvement) reconnect websocket in case of websocket error (every 10 seconds if not offline)
- (improvement) try to reconnect to server if no ping is received
- (improvement-3) disable signaling wake-lock when offline - enable signaling wake-lock when online
- compile own webrtc lib for android https://github.com/pristineio/webrtc-build-scripts
- Custom-Dialog for "back to front" enhancement 
	http://stackoverflow.com/questions/13341560/how-to-create-a-custom-dialog-box-in-android
	http://stackoverflow.com/questions/7569937/unable-to-add-window-android-view-viewrootw44da9bc0-permission-denied-for-t#answer-34061521
	http://stackoverflow.com/questions/32224452/android-unable-to-add-window-permission-denied-for-this-window-type
- try timeout of half a second ofter bringing app back to foreground for display call dialog
- (enhancement) bring app to forground during incoming call (doesn't work)
	http://stackoverflow.com/questions/29766270/how-to-resume-android-activity-programmatically-from-background/29769255#29769255
	http://stackoverflow.com/questions/12074980/bring-application-to-front-after-user-clicks-on-home-button
- re-register when coming back online
- send heartbeat message to websocket 
- (feature) add wake up feature - if (user is offline - allow wake up trough gcm)
- save gcm registration token with websocket registration on signaling server
- wake up certain user when offline (if app was started can be seen if use is online now) 

- security check BEAST attack on production server (TEST SSL) 
	https://www.ssllabs.com/ssltest/analyze.html?d=webrtc.a-fk.de&latest

- Test necessary: don't let android go into idle mode during a call (who can test this in Android 5.0)
	- http://stackoverflow.com/questions/3723634/how-do-i-prevent-an-android-device-from-going-to-sleep-programmatically
	- https://developer.android.com/reference/android/os/PowerManager.html
	- https://github.com/commonsguy/cw-advandroid/tree/master/SystemServices/Alarm/	

##possible tweaks 
- produces echo while communicating with chrome
	- test with non-opus codec
	- Echo Cancellation: Android-AECM https://github.com/lhc180/webrtc-based-android-aecm
	Pakistani Echo in WebRTC https://www.webrtc-experiment.com/pdf/Echo-in-WebRTC-Why.pdf
	http://stackoverflow.com/questions/12818721/webrtc-aec-on-android
	http://gingertech.net/2014/03/19/apprtc-googles-webrtc-test-app-and-its-parameters/
	https://groups.google.com/forum/#!topic/easyrtc/zCUurD4tA2E

##Nice2Have
- (p3) add "audio call" and "video call" button
- (p3) add "answer with audio" and answer "answer with video" button during incoming call 


##Tests
- Handsfree speaker test switch with earpiece 
- (not tested yet) does app go in stand by mode during video broadcast
- (not tested yet) test reconnect when app goes offline or wifi off (see also: https://github.com/palmerc/SecureWebSockets/issues/13)
- (ok) test socket stays connected in background mode. 

##Done:
- 03.02.2017 - added library to github / jitpack.io
			 - creating and sharing an own android library (blog)
			 	https://mayojava.github.io/android/library/creating-and-distributing-your-own-android-library/
			 - JitPack.Io
			 	https://jitpack.io
			 	https://jitpack.io/#inspiraluna/AppRTC-Android/2.01
			 - android-gradle-maven-plugin
			 	https://github.com/dcendents/android-maven-gradle-plugin
			 - some stackoverflow answers
			 	http://stackoverflow.com/questions/33058358/jitpack-io-failed-to-resolve-github-repo/33059275#33059275
			 - Git-SubModules - https://git-scm.com/book/en/v2/Git-Tools-Submodules

- 13.01.2017 - add msc-webrtc library to msc-android project
- 13.01.2017 - convert app project into a library
	https://developer.android.com/studio/projects/android-library.html
- 13.01.2017	- (bug) Signaling finishes when app goes to sleep
				- https://developer.android.com/training/scheduling/wakelock.html
- (bug) service or activity gets destroyed - send message to start each other.
	http://stackoverflow.com/questions/9696861/how-can-we-prevent-a-service-from-being-killed-by-os
- 13.01.2017 - make a foreground service Sample Foreground Service
		https://github.com/commonsguy/cw-android/tree/master/Notifications/FakePlayer
		http://www.truiton.com/2014/10/android-foreground-service-example/
		http://stackoverflow.com/questions/9696861/how-can-we-prevent-a-service-from-being-killed-by-os
- 12.01.2017 - (improvement) created le-space launch icon
	https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html#foreground.type=image&foreground.space.trim=1&foreground.space.pad=0&foreColor=607d8b%2C0&crop=0&backgroundShape=bevel&backColor=ffffff%2C100&effects=none
- 11.01.2017 - (bug) after wifi reconnect app crashes
- 10.01.2017 - (bug) android or status widget seems to disconnect/go offline after a couple of minutes
	- implement websocket service which runs in background
- 04.01.2017 - re-register when coming back online
- 04.01.2017 - send heartbeat message to websocket
- 04.01.2017 - (bug) if android goes offline server doesn't recognize it - send ping-pong
- 04.01.2017 - (bug) if a user is already registered when coming back online websocket must be overwritten
- 02.01.2017 - (bug) when first time registering over facbook old default user does not de-register
	- don't use default - user
	- don't connect with default user
- 02.01.2017 - login with facebook and setUserId
				https://code.tutsplus.com/tutorials/quick-tip-add-facebook-login-to-your-android-app--cms-23837
				https://www.tutorialspoint.com/android/android_shared_preferences.htm
- 01.12.2016 - (screensharing) if android calls browser - browse cannot start screensharing (initiater - offer problem)

- 01.12.2016 - (screensharing) remote hangup does not switches back to video screen
- 30.11.2016 - remote hangup stops communication very late. stop must react immediately
- 29.11.2016 - stop connection from browser does not end properly on android
- 29.11.2016 - (gestures) for camera switch - double tab is not working 
- 29.11.2016 - (2 MT) added second PeerConnection logic for screensharing over additional stream  
- 25.11.2016 - add double touch on display for camera switch
- 23.11.2016 - add video on/off button 
- 23.11.2016 - add audio on/off button  
			   http://stackoverflow.com/questions/35208029/switch-between-audio-and-video-call-in-apprtc-android-code
- 20.11.2016 - (bug) iterate through permissions requests to next permission only grant or deny (when app starts first time)
- 19.11.2016 - now app comes back to forground while ringing. but "answer"/"hangup" button is not shown	
- 19.11.2016 - app now also rings in background 
- 19.11.2016 - added new permission handling for audio and video
- 18.11.2016 - Added GCM-Message to Android-App which wakes it up (message can be send to firebase gcm service)
			 - Notification is send 
- 17.11.2016 - Evaluation of Google Cloud Messaging on Android (6h)
	- worked through android gcm example on https://developers.google.com/cloud-messaging/android/start
		(in https://developers.google.com/cloud-messaging/samples)
	- created Firebox Account on https://console.firebase.google.com/ for GCM (Google Cloud Messaging)
	- reviewed but not yet used gcm playground https://github.com/googlesamples/gcm-playground
- 17.11.2016 - Evaluation of Android Services for Background Tasks
	- https://developer.android.com/reference/android/app/Service.html#ProcessLifecycle
- 17.11.2016 - RingTone during call (2h)
- 25.10.2016 - user registers but does not unregister when closing application
- 25.10.2016 - removed websocket disconnect after hangup
- 20.10.2016 - camera switch works (no implementation needed)	
- 14.10.2016 - incoming call: decision: answer or hangup?
- 14.10.2016 - better error handling while switching connection parameters
	- wrong url
	- no internet
	- already registered user
	- other error
- 14.10.2016 -reload userlist after reconnection.
- 14.10.2016 -crashes when changing protocoll between ws to wss and back on 5.0
- 13.10.2016 - change websocket library to https://github.com/TakahikoKawasaki/nv-websocket-client in order to fix ssl bug in android 5.02
- 13.10.2016 - ssl handshake exception on android lollipop (5.02)
				analyze ssl server 
					https://www.ssllabs.com/ssltest/analyze.html?d=webrtc.a-fk.de
				(solution 0) https://github.com/TakahikoKawasaki/nv-websocket-client
				(solution 1) http://stackoverflow.com/questions/27112082/httpclient-fails-with-handshake-failed-in-android-5-0-lollipop
				(solution 2) http://stackoverflow.com/questions/33003017/ssl-handshake-failed-android-5-1
				(solution 3) update googles security provider 
					http://appfoundry.be/blog/2014/11/18/Google-Play-Services-Dynamic-Security-Provider/
					https://developer.android.com/training/articles/security-gms-provider.html#patching
					http://stackoverflow.com/questions/24357863/making-sslengine-use-tlsv1-2-on-android-4-4-2/26586324#26586324
					http://stackoverflow.com/questions/29916962/javax-net-ssl-sslhandshakeexception-javax-net-ssl-sslprotocolexception-ssl-han
			    websocket java lib 
			    	https://github.com/elabs/mobile-websocket-example/issues/6
			    	https://github.com/TooTallNate/Java-WebSocket/issues/293
			    	https://github.com/TooTallNate/Java-WebSocket/issues/141
			    	https://github.com/TooTallNate/Java-WebSocket/pull/101
			    other
			    	https://qnalist.com/questions/5822188/android-5-0-ssl-handshake-failure
- 04.10.2016 - BUG-27.9.2016 Websocket does not connect on Android 5.0 (Android 5.1 does)
			 - reading https://www.varvet.com/blog/using-websockets-in-native-ios-and-android-apps/
			 - possible problem Android 5.0 Lollipop with wss: 
					https://github.com/TooTallNate/Java-WebSocket/issues/293
					https://github.com/andrepew/Java-WebSocket/tree/1.3.0-Android-SSL-Fix
- 04.10.2016 - when android hangs up stop message is not send to partner 
- 04.10.2016 - when stop message comes from peer android does not cancel the call
- 03.10.2016 - fixed problem with missing libjingle through an emulator related inclusion of x86-libs, which didn't turn out to be that good for android-native .apk
- 03.10.2016 - fixed problem with current-user list 
- 27.09.2016 - ws and wss now possible in case of a not working wss in lollipopp.
- 27.09.2016 - test socket stays connected in background mode. ok
- 23.09.2016 - fixed  "settings change" issue: https://github.com/Le-Space/mscrtc-android/issues/1
- 20.09.2016 - fixed bug: new secure websocket crashes / disconnects / error on tomcat but works on glassfish 
				- tomcat problem? Check if server is working correctly: 
				- https://cryptoreport.thawte.com/checker/views/certCheck.jsp ok
				- http://www.websocket.org/echo.html
	- android problem? see: - https://github.com/palmerc/SecureWebSockets

- 16.9.2016 - websocket in wss mode (secure) autobahn does work or not?
				tried: https://github.com/palmerc/SecureWebSockets  (from: https://github.com/crossbario/autobahn-android/pull/14)
					- tomcat crashes
				https://github.com/TooTallNate/Java-WebSocket
				https://github.com/TooTallNate/Java-WebSocket/issues/141
				http://www.juliankrone.com/connect-and-transfer-data-with-secure-websockets-in-android/
- 16.9.2016 - if websocket url is wrong android crashes and url cannot be changed anymore
- 27.7.2016 - when clients disconnects users are not sent out to other clients
- 27.7.2016 - user list gets updated
- 26.7.2016 - when android gets called video does not appear
- 26.7.2016 - handle call from browser to android
- 26.7.2016 - on app start call "appConfig"  and "register username" from setttings
- 26.6.2016 - handle onIceCandidate stuffs
- 26.6.2016 - handle WSS->C: {"id":"callResponse","response":"rejected: user 'nico' is not registered"} by android
- 26.6.2016 - moved turn configuration in to room config
- 26.6.2016 - on call transmit from string from settings and to string from user to call 
- 26.6.2016 -	add "from" User to Setting of Android App
- 26.6.2016 - use room names as to (change label)
- 25.6.2016 - add appConfig to java websocket server
- 25.6.2016 - change rest /join to websockets
