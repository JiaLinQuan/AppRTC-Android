# AppRTCDemo - Android

##About
This project is a native prototype in order to communicate with kurentos media server. It works in conjunction with two other projects:

There are also:
- a pure websocket AppRTC for Kurento: AppRTC-Kurento and
- a pure websocket AppRTC for Android: AppRTC-iOS 

##Common Mistakes
- (production) wrong url for webservice.
- (development environment) smartphone not in same network as browser, media kurento server 
- (development environment) kurento server not running?

##Todo/Bugs
- incoming call: Decision: answer or hangup?
- when android hangs up stop message is not send to partner (connect is still NEW! why?)
- when stop message comes from peer android does not cancel the call
- mic on/off
- video on/off
- camera switch	

##Nice2Have
- ringtone / alarm for incoming call

##Tests
- (not tested yet) does app goes in stand by mode during video broadcast
- (not tested yet) test reconnect when app goes offline or wifi off (see also: https://github.com/palmerc/SecureWebSockets/issues/13)
- (ok) test socket stays connected in background mode. 

##Bugs
- BUG-27.9.2016 Websocket does not connect on Android 5.1 
	- reading https://www.varvet.com/blog/using-websockets-in-native-ios-and-android-apps/
	- possible problem Android 5.0 Lollipop with wss: 
		https://github.com/TooTallNate/Java-WebSocket/issues/293
		https://github.com/andrepew/Java-WebSocket/tree/1.3.0-Android-SSL-Fix
- security check BEAST attack on production server

##Improvements
- security considerations while connecting (!)
- request runtime permission for android 6 (marshmellow) devices

##Done:
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