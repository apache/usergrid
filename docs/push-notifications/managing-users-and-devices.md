# Managing users and devices
Before your app can receive notifications on a user's device, the app's code will need to register the device with both the Usergrid and the appropriate push notification service (Apple APNs or Google GCM).

By registering with the Usergrid, your app adds the device on which it is installed to your data store. The device is represented as a Device entity. This makes it possible for you to target that device when sending notifications. (For more on the Device entity, see the [API Docs](../rest-endpoints/api-docs.html).) Any devices, users, and groups that have been registered in this way may be targeted with push notifications.

By registering with the notification service, you make the device known to the service. This way, the service can forward your notifications to the device.

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
For an overview of how to set up push notifications, including troubleshooting tips, see [Adding push notifications support](adding-push-support.html).
</p></div>

## Registering devices
The following samples illustrate how to register a device with a notification service and with the Usergrid. At a high level, your code will send a registration request to the notification service, then use information in the service's response to send a separate request to the Usergrid. The two requests correlate the notification service, Usergrid, and your mobile app.

You can also create device entities separately by using the /devices endpoint. For more information on using the ``/devices`` endpoint in the Usergrid, see the [API Docs](../rest-endpoints/api-docs.html).

Registering a device with a notification service is a standard coding activity for implementing push notifications. This is not specific to the Usergrid.


### Registering for iOS

The following code illustrates how you can use the iOS SDK to register a device with both the Usergrid server and with the APNs, the Apple push notification service. This example assumes that your code has already property initialized the SDK. For more information, see [Installing the Apigee SDK for iOS](../sdks/tbd.html).

    // Register with Apple to receive notifications.

    // Invoked when the application moves from an inactive to active state. Use this
    // method to register with Apple for notifications.
    - (void)applicationDidBecomeActive:(UIApplication *)application
    {
        // Find out what notification types the user has enabled.
        UIRemoteNotificationType enabledTypes =
            [application enabledRemoteNotificationTypes];

        // If the user has enabled alert or sound notifications, then
        // register for those notification types from Apple.
        if (enabledTypes & (UIRemoteNotificationTypeAlert|UIRemoteNotificationTypeSound)) {
            
            // Register for push notifications with Apple
            NSLog(@"registering for remote notifications");
            [application registerForRemoteNotificationTypes:UIRemoteNotificationTypeAlert |
             UIRemoteNotificationTypeSound];
        }
    }

    // Invoked as a callback from calling registerForRemoteNotificationTypes. 
    // newDeviceToken is a token received from registering with Apple APNs.
    // Use this method to register with Apigee.
    - (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)newDeviceToken
    {
        // Register device token with the Usergrid (will create the Device entity if it doesn't exist)
        // Sends the name of the notifier you created with Apigee, along with the token your code
        // received from Apple.
        ApigeeClientResponse *response = [dataClient setDevicePushToken: newDeviceToken
                                                            forNotifier: notifier];
        
        if ( ! [response completedSuccessfully]) {
            [self alert: response.rawResponse title: @"Error"];
        }
    }

    // Invoked as a callback from calling registerForRemoteNotificationTypes if registration 
    // failed.
    - (void)application:(UIApplication*)application didFailToRegisterForRemoteNotificationsWithError:(NSError*)error
    {
        [self alert: error.localizedDescription title: @"Error"];
    }

Initialize the Apigee client and check for notifications that might have been sent while the app was off.

    // Invoked as a callback after the application launches.
    - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
    {
        // Connect and login
        ApigeeClient *apigeeClient =
            [[ApigeeClient alloc] initWithOrganizationId:orgName
                                           applicationId:appName
                                                 baseURL:baseURL];
        dataClient = [apigeeClient dataClient];
        [dataClient setLogging:true]; //comment out to remove debug output from the console window

        // Find out if there's a notification waiting to be handled after the
        // app launches.
        if (launchOptions != nil) {
            NSDictionary* userInfo =
                [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
            
            // If there's notification data waiting, send it to be processed.
            if (userInfo) {
                [self handlePushNotification:userInfo
                              forApplication:application];
            }
        }
        
        // It's not necessary to explicitly login if the Guest role allows access.
        // But this is how you can do it.
    //    [apigeeClient logInUser: userName password: password];

        NSLog(@"done launching");
        return YES;
    }

### Registering for Android

The following code illustrates how to register a client device with GCM, register the device with Apigee, and associate the device with the user. Methods used in this code are defined in the Apigee Android SDK. For more information on downloading and installing the SDK, see Installing the Apigee SDK for Android.

    import android.content.Context;
    import com.google.android.gcm.GCMRegistrar;
    import com.apigee.sdk.ApigeeClient;
    import com.apigee.sdk.data.client.entities.Device;

    /**
     * Registers this device with GCM, Google's messaging 
     * service.
     *
     * @param context An Android context with information specific to this 
     * application's context on the device.
     */
    static void registerPush(Context context) {
        
        //Get an instance of the Apigee DataClient class from the ApigeeClient object
        dataClient = getClient().getDataClient();
        
        // Get the registration ID (GCM API key) for this application.
        final String regId = GCMRegistrar.getRegistrationId(context);

        // If this device isn't already registered with GCM, register it
        // using the the application context and an ID for the Google account
        // authorized to send messages to this application. This is the 
        // Google Client ID from Google API Console.
        if ("".equals(regId)) {
          GCMRegistrar.register(context, gcmSenderId);
        } else {
            if (GCMRegistrar.isRegisteredOnServer(context)) {
                Log.i(TAG, "Already registered with GCM");
            } else {
            
            // Use an instance of the Client class (SDK for Android) 
            // to register this device with the Usergrid. Pass as arguments
            // the device unique identifier, the unique name of the notifier you
            // created in the Usergrid, the GCM API key, and a callback that will
            // receive an instance of a Device class representing the registered
            // device on the system.
            dataClient.registerDeviceForPushAsync(dataClient.getUniqueDeviceID(), notifierName, regId, null, 
                new DeviceRegistrationCallback() {        
                    @Override
                    public void onResponse(Device device) {                
                        AppServices.device = device;

                        // Associate the logged in user with this device.
                        if (dataClient.getLoggedInUser() != null) {
                            dataClient.connectEntitiesAsync("users", 
                                dataClient.getLoggedInUser().getUuid().toString(),
                                "devices", device.getUuid().toString(),
                                new ApiResponseCallback() {
                                    @Override
                                    public void onResponse(ApiResponse apiResponse) {
                                      Log.i(TAG, "connect response: " + apiResponse);
                                    }
                        
                                    @Override
                                    public void onException(Exception e) {
                                      displayMessage(context, "Connect Exception: " + e);
                                      Log.i(TAG, "connect exception: " + e);
                                    }
                            });
                        }
                    }
            
                });
            }
        }
    }

    /**
     * Create an instance of the SDK ApigeeClient class, setting
     * values from your Apigee registration.
     */
    static synchronized ApigeeClient getClient() {
        if (client == null) {
            client = new ApigeeClient();
            client.setApiUrl("https://api.usergrid.com");
            client.setOrganizationId("your-org");
            client.setApplicationId("your-app");
        }
        return client;
    }


### Registering for HTML5/PhoneGap

The following code illustrates how you can use the JavaScript functions included with the PhoneGap plugin to register a device with both the Apigee server and with the APNs, the Apple push notification service.

    // Declare a variable for calling push notification APIs.
    var pushNotification = window.plugins.pushNotification;
    // Collect configuration options to specify that this device accepts
    // an alert message, an application badge, and a sound.
    var appleOptions = {
        alert:true, badge:true, sound:true
    };
    // Register the device with the Usergrid, passing options for configuration 
    // along with a callback from which you can retrieve the device token
    // sent by Apigee.
    pushNotification.registerDevice(appleOptions, function(status) {
        console.log(status);
        // If a token was received, bundle options to pass when registering the device 
        // with the push notification service. The provider value must be "apigee" to
        // support push notification through Apigee. orgName and appName should be 
        // values corresponding to those used in your Apigee account.
        // notifier is the unique name you associated with the Apigee notifier you created.
        // token is the device token this code received from Apigee after registering the 
        // device.
        if(status.deviceToken) {
            var options = {
                "provider":"apigee",
                "orgName":"YOUR APIGEE.COM USERNAME",
                "appName":"sandbox",
                "notifier":"YOUR NOTIFIER",
                "token":status.deviceToken
            };

            // Use the device token and other options to register this device with the 
            // push notification provider.
            pushNotification.registerWithPushProvider(options, function(status){
                console.log(status);
            });
        }
    });
    
The functions used in this code are defined in the PhoneGap plugin. JavaScript functions invoke underlying Objective-C or Java code (depending on platform). You'll find that code in these files, included in the Apigee PhoneGap push notification plug-in.

Information about installing the plugin is available in its Readme file. For more complete examples, see [Tutorial: Push notifications sample app](tutorial.html).


## Connecting devices to users
You can associate user entities with device entities in the Usergrid. Doing so allows you to target your push notifications at users with specific characteristics. The following describes how to connect a user to a specific device in the Usergrid.

For more information on creating a device in your Usergrid data store, see "Registering Devices" above.

For more information on creating a user in your Usergrid data store, see [User](../rest-endpoints/api-docs.html#user).

The following code examples all use the same basic endpoint pattern for connecting devices with users:

    POST /users/{userUUID or name}/devices/{deviceUUID}
    
### Connecting with curl
The following call connects user "joex" with device 7a0a1cba-9a18-3bee-8ae3-4f511f12a386 (the device UUID). After this connection, you can send a push notification to joex rather than the device. Further, if joex has specific properties set--such as {"favoritecolor": "blue"}--you can send a push notification to all users whose favorite color is blue (assuming they're connected to devices in the Usergrid).

    curl -X POST "https://api.usergrid.com/my-org/sandbox/users/joex/devices/7a0a1cba-9a18-3bee-8ae3-4f511f12a386"

### Connecting with iOS

The following sample code, taken from AppDelegate.m in the native iOS push sample, uses the connectEntities method from the iOS SDK to connect a device to a user.

    ApigeeClientResponse *response = [dataClient setDevicePushToken: newDeviceToken forNotifier: notifier];

    // You could use this if you log in as an Usergrid user to associate the Device to your User
    if (response.transactionState == kUGClientResponseSuccess) {
        response = [self connectEntities: @"users" connectorID: @"me" type: @"devices" connecteeID: deviceId];
    }

### Connecting with Android

The following sample code, taken from [AppServices.java](https://github.com/apigee/appservices-android-push-example/blob/master/src/com/ganyo/pushtest/AppServices.java) in the native Android push sample, uses the connectEntitiesAsync method from the Android SDK to connect a device to an authenticated user.

    // connect Device to current User - if there is one
    if (dataClient.getLoggedInUser() != null) {
      dataClient.connectEntitiesAsync("users", dataClient.getLoggedInUser().getUuid().toString(),
                                       "devices", device.getUuid().toString(),
                                       new ApiResponseCallback() {...
                                       
### Connecting with HTML5/JavaScript

The following code illlustrates how to associate the currently logged in user with their device.

    // You'll need a client from the JavaScript SDK.
    var client = new Apigee.Client({
        // Initialize client.
    });

    // Get information about the current user so you can use
    // it to connect them with their device.
    client.getLoggedInUser(function(err, data, user) {
        if(err) {
            // Could not get the logged in user.
        } else {
            if (client.isLoggedIn()) {
                // Using a PushNotification function to get the device ID as
                // it is known to the Apigee system.
                pushNotification.getApigeeDeviceId(function(results) {
                    if (results.deviceId) {
                        // Use the JavaScript SDK connect function to register
                        // a connection between the current user and their device.
                        user.connect('devices', results.deviceId, function (err, data) {
                            if (err) {
                                // Could not make the connection.
                            } else {
                                // Call succeeded, so pull the connections back down.
                                user.getConnections('devices', function (err, data) {
                                if (err) {
                                    // Couldn't get the connections.
                                } else {
                                    // Connection exists.
                                });
                            }
                        }
                    }
                }
            }
        }
    }

You can also connect users with groups so that you can send push notifications to groups of users (and their associated devices), see [Working with group data](../user-management/group.html).

