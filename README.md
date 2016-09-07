# AppRTCDemo for Android


##About
Android Studio project is a native prototype in order to communicate with kurentos media server. It works in conjunction with two other projects:
- AppRTC-Kurento (Kurento Signaling Server in Java (NodeJS version exists but not fully implemented here)
- AppRTC-iOS (the iOS counterpart of this project)

!!!This version was modified to work with websockets and kurento java api!!!

##Todo/Bugs
- when android hangs up stop message is not send to partner (connect is still NEW! why?)
- when stop message comes from peer android does not cancel the call
- when peer sends stop message session is removed from server too - its an error.
- incoming call decide answer or hangup
- accept/reject incoming calls - with audio/visual alarm and ringtone

##Bugs
- websocket in wss mode (secure) autobahn does work or not?
- security considerations while connecting (!)
- request runtime permission for android 6 (marshmellow) devices

##Common Mistakes
- smartphone not in same network as browser, media kurento server
- kurento server not running

##Done:
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
 25.6.2016 - change rest /join to websockets