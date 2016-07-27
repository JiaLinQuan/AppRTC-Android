# AppRTCDemo

Android Studio project for AppRTCDemo of WebRTC project. The revision number of this build is 11801.
https://chromium.googlesource.com/external/webrtc/+/250fc658c56cb3c6944bd0fd1b88973f2e6d1ced


##This version was modified to work with websockets only and is currently presentable with the java version of kurento media server at https://github.com/inspiraluna/AppRTC-Kurento-Example

##Todo/Bugs

- when android hangs up stop message is not send to partner (connect is still NEW! why?)
- incoming call decide answer or hangup
- accept/reject incoming calls - with audio/visual alarm and ringtone
- when clients disconnect users are not sent out to other clients
- read registered users from registry / and display as users in Android App
- security considerations while connecting


- request runtime permission for android 6 (marshmellow) devices 


##Common Mistakes
- smartphone not in same network as browser, media kurento server
- kurento server not running

##Done:
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