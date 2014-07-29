/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Though it is enabled here for one platform, this code can be
 * easily modified to to support both iOS and Android. See the comments 
 * for platform-specific code.
 *
 * In order to use this sample, you must first have:
 *  
 * - Created an Apple App ID that supports 
 * push notifications.
 * - Created an Apigee push notifier. 
 */

// IMPORTANT! Update these with your own values -- the org name,
// app name, and notifier you created in the portal.
var orgName = "YOUR ORGNAME";
var appName = "YOUR APPNAME";
var notifier = "YOUR NOTIFIER";

/*
 * Called when a notification is received from Apple.
 * Here, handle notifications as they should be handled on 
 * an iOS device.
 */
function onNotificationAPN(event) {
    console.log(JSON.stringify(event, undefined, 2));
    if (event.alert) {
        navigator.notification.alert(event.alert);
    }
    
    if (event.sound) {
        var snd = new Media(event.sound);
        snd.play();
    }
    
    if (event.badge) {
        pushNotification.setApplicationIconBadgeNumber(successHandler, errorHandler, event.badge);
    }
}

/*
 * Called by Google with notification-related events. Can be
 * called to confirm device registration and when events
 * are sent.
 */
function onNotificationGCM(e) {
    $("#app-status-ul").append('<li>EVENT -> RECEIVED:' + e.event + '</li>');

    switch( e.event )
    {
        case 'registered':
        if ( e.regid.length > 0 )
        {
            $("#app-status-ul").append('<li>REGISTERED -> REGID:' + e.regid + "</li>");
            // Your GCM push server needs to know the regID before it can push to this device
            // here is where you might want to send it the regID for later use.
            console.log("regID = " + e.regID);
        }
        break;

        case 'message':
            // if this flag is set, this notification happened while we were in the foreground.
            // you might want to play a sound to get the user's attention, throw up a dialog, etc.
            if (e.foreground)
            {
                $("#app-status-ul").append('<li>--INLINE NOTIFICATION--' + '</li>');

                // if the notification contains a soundname, play it.
                var my_media = new Media("/android_asset/www/"+e.soundname);
                my_media.play();
            }
            else
            {   // otherwise we were launched because the user touched a notification in the notification tray.
                if (e.coldstart)
                    $("#app-status-ul").append('<li>--COLDSTART NOTIFICATION--' + '</li>');
                else
                $("#app-status-ul").append('<li>--BACKGROUND NOTIFICATION--' + '</li>');
            }

            $("#app-status-ul").append('<li>MESSAGE -> MSG: ' + e.payload.message + '</li>');
            $("#app-status-ul").append('<li>MESSAGE -> MSGCNT: ' + e.payload.msgcnt + '</li>');
        break;

        case 'error':
            $("#app-status-ul").append('<li>ERROR -> MSG:' + e.msg + '</li>');
        break;

        default:
            $("#app-status-ul").append('<li>EVENT -> Unknown, an event was received and we do not know what it is</li>');
        break;
    }
}

var app = {
  // Application Constructor
  initialize: function() {
      this.bindEvents();
  },
  // Bind event listeners
  //
  // Bind any events that are required on startup. Common events are:
  // 'load', 'deviceready', 'offline', and 'online'.
  bindEvents: function() {
      document.addEventListener('deviceready', this.onDeviceReady, false);
  },
  // deviceready event handler
  //
  // The scope of 'this' is the event. In order to call the 'receivedEvent'
  // function, we must explicity call 'app.receivedEvent(...);'
  onDeviceReady: function() {
      var client = new Apigee.Client({
        orgName:orgName,
        appName:appName,
        logging: true, //optional - turn on logging, off by default
		buildCurl: true //optional - logs all network calls to the console, off by default
      });

      // A variable to refer to the PhoneGap push notification plugin.
      var pushNotification = window.plugins.pushNotification;

      // A callback function used by Apple APNs when notification 
      // registration is successful.
      function tokenHandler(status) {
        if(status) {
          var options = {
            notifier:notifier,
            deviceToken:status
          };

          // Now register with Apigee so that you can target this device 
          // for notifications.
          client.registerDevice(options, function(error, result){
            if(error) {
                console.log(error);
            } else {
                console.log(result);
            }
          });
        }
      }

      // A callback function used by Google GCM when notification registration
      // is successful. Not used on iOS.
      function successHandler(result) {console.log(result);}

      // A callback function used by Apple APNs and Google GCM when 
      // notification registration fails.
      function errorHandler(error){ console.log(error);}

      // Detect the device platform this app is deployed on and register
      // accordingly for notifications.
      if (device.platform == 'android' || device.platform == 'Android') {
          // If this is an Android device, register with Google GCM to receive notifications.
          // On Android, the senderID value is the project number for Google API project
          // that supports Google Cloud Messaging.
          pushNotification.register(successHandler, errorHandler, 
            {"senderID":"replace_with_sender_id", "ecb":"onNotificationGCM"});
      } else {
          // If this is an iOS device, register with Apple APNs to receive notifications.
          pushNotification.register(tokenHandler, errorHandler, 
            {"badge":"true", "sound":"true", "alert":"true", "ecb":"onNotificationAPN"});
      }

      // Handle the app UI button's click event to send a notification
      // to this device.
      $("#push").on("click", function(e){

        // Build the request URL that will create a notification in app services.
        // Use this device's ID as the recipient.
        var devicePath = "devices/"+client.getDeviceUUID()+"/notifications";
        var options = {
          notifier:notifier,
          path:devicePath,
          message:"Hello world from JavaScript!"
        };
        // Send a notification to this device.
        client.sendPushToDevice(options, function(error, data){
          if(error) {
            console.log(data);
          } else {
            console.log("Push sent");
          }
        });
      });
      
      app.receivedEvent('deviceready');
  },
  // Update DOM on a Received Event
  receivedEvent: function(id) {
      var parentElement = document.getElementById(id);
      var listeningElement = parentElement.querySelector('.listening');
      var receivedElement = parentElement.querySelector('.received');
      
      listeningElement.setAttribute('style', 'display:none;');
      receivedElement.setAttribute('style', 'display:block;');
      
      console.log('Received Event: ' + id);
  }
};
