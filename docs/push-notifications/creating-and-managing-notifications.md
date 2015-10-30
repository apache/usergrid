# Creating and managing notifications
This topic provides information on setting up and sending push notifications with the Usergrid API backend as a service (BaaS). For high-level information, prerequisites, and tutorials, see [Push notifications overview](overview.html).

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
Although not shown in many of the API examples below, you need to provide a valid access token with each API call. See [Authenticating users and application clients](../security-and-auth/authenticating-users-and-application-clients.html) for details.
</p></div>

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
For an overview of how to set up push notifications, including troubleshooting tips, see [Adding push notifications support](adding-push-support.html).
</p></div>

## Creating notifications
When you create a notification, it is scheduled to be delivered to all applicable devices immediately unless the deliver property has been set. A notification may contain multiple messages (payloads), and each payload must be associated with a notifier in order to deliver that message to the associated app. This allows a single notification to be delivered to multiple apps across various services, and is useful if you want to send messages to multiple notifiers, such as development and production versions of both Apple and Google simultaneously.

Notification can be created via POST request to the BaaS API, or in the admin portal:

### Creating notificastions with cURL

#### Targeting a single device
This request will target a specific device entity.

Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/devices/<device_ID>/notifications -d '{"payloads":{<notifier>:<message>}}'
		
#### Targeting all devices
This request will target all device entities.

Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/devices/*/notifications -d '{"payloads":{<notifier>:<message>}}'
		
#### Targeting a single user
This request will target a specific user entity.

Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/users/<username>/notifications -d '{"payloads":{<notifier>:<message>}}'
		
#### Targeting a group
This request will target all users associated with a specific group entity.

Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/groups/<group_path>/notifications -d '{"payloads":{<notifier>:<message>}}'
		
#### Targeting users by location
This request will target all device entities that are within a set radius of a latitude/longitude coordinate.

Request Syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/devices;ql=location within <radius> of <lat>,<long>/notifications -d '{"payloads":{<notifier>:<message>}}'
		
#### Request Parameters

The following parameters can be specified when targeting push notifications.

Base URL

These parameters are used in forming the base URL of the request:

Parameter	Description
---------   -----------
org	        Organization UUID or organization name
app	        Application UUID or application name

Notification

These parameters are used when forming the notification portion of the request:

Parameter	Description
---------   -----------
notifier	The ``notifier`` entity you wish to associate with the notification (see [Creating notifiers](creating-notifiers.html) for information on creating notifiers)
message	    The push notification message that will be delivered to the user

__Note__: If your message contains double-quotes ("") you must escape them with a backslash

Targets

These parameters are used when specifying the notification target in the request.

Parameter	                    Description
---------                       -----------
device	                        UUID of a device entity.
user	                        UUID or username of a user entity.
phone_type	                    Specified in a appended query string. The type property of a device entity.
group	                        UUID or name of the group entity.
latitude, longitude, radius 	Specified in a appended query string. The radius in meters of a user device from a latitude/longitude coordinate.


### Creating notificastions with Admin portal

To create the new notification with the admin portal, do the following:

1. Log in to the admin portal.
2. In the left nav, click __Push > Send Notification__.
3. From the __Notifier__ drop-down, select the appropriate notifier.
4. Under the notifier name, select one of the following to specify where your notification message should go:

* __All Devices__ to have the message sent to all devices.
* __Devices__ to have the message sent to a particular subset of devices. Enter a comma-separated list of device UUIDs in the box provided.
* __Users__ to have the message sent to a particular subset of users. Enter a comma-separated list of username values in the box provided.
* __Groups__ to have the message sent to a particular subset of user groups. Enter a comma-separated list of group paths in the box provided.

5. In the __Notifier Message__ field, enter the message (payload) you want delivered.
6. If double-quotes are included in your message, you must escape them with a backslash.
7, Under __Delivery__, select one of the following to specify when the notification should be delivered:

* __Now__ to have the message delivered immediately.
* Schedule for later to choose a date and time at which the message should be delivered.

7. Click __Submit__.

To create a notification to send multiple messages or target multiple platforms, see the cURL syntax for creating notifications.

### Creating notificastions with iOS

To send notifications from iOS app code, you use the 11ApigeeAPSDestination11 and ``ApigeeAPSPayload`` classes.

With ``ApigeeAPSDestination``, you specify where the notification should go (a particular user or device or user group, for example). With ``ApigeeAPSPayload``, you specify the contents of the message, including any alert text, sound, or badge (items specified by Apple as allowable notification content types). You then use ``ApigeeDataClient`` to send the message via Apigee and Apple APNs.

The following code illustrates how to send a notification to a single user. Though this is the device on which the app itself is installed, you can imagine how you might send a notification to another device, such as one belonging to someone "following" this user. For more, see the iOS push sample application included with the [Apigee iOS SDK](../sdks/tbd.html).

    - (void)sendMyselfAPushNotification:(NSString *)message
    completionHandler:(ApigeeDataClientCompletionHandler)completionHandler
    {
        // send to a single device -- our own device
        NSString *deviceId = [ApigeeDataClient getUniqueDeviceID];
        ApigeeAPSDestination* destination =
            [ApigeeAPSDestination destinationSingleDevice:deviceId];
        
        // set our APS payload
        ApigeeAPSPayload* apsPayload = [[ApigeeAPSPayload alloc] init];
        apsPayload.sound = kBundledSoundNameWithExt;
        apsPayload.alertText = message;
        
        // Example of what a custom payload might look like -- remember that
        // APNS payloads are limited to a maximum of 256 bytes (for the entire
        // payload -- including the 'aps' part)
        NSMutableDictionary* customPayload = [[NSMutableDictionary alloc] init];
        [customPayload setValue:@"72" forKey:@"degrees"];
        [customPayload setValue:@"3" forKey:@"newOrders"];
        
        __weak AppDelegate* weakSelf = self;
        
        // send the push notification
        [dataClient pushAlert:apsPayload
                customPayload:customPayload
                  destination:destination
                usingNotifier:notifier
            completionHandler:^(ApigeeClientResponse *response) {
                if ( ! [response completedSuccessfully]) {
                    [weakSelf alert:response.rawResponse
                              title: @"Error"];
                }
                
                if (completionHandler) {
                    completionHandler(response);
                }
            }];
    }

## Scheduling notifications

### cURL

To schedule a notification for a later time, add the deliver parameter with a UNIX timestamp to the body of your request.

Request body syntax

    '{"deliver":<unix_timestamp>,"payloads":{<notifier>:<message>}}'

## Targeting multiple notifiers or messages
To send multiple messages or target multiple platforms with a single notification entity, include multiple notifier-message pairs as a comma-separated list in the payloads object of the request body:

Request body syntax

    '{"payloads":{<notifier>:<message>, <notifier>:<message>, ...}}'
		
## Setting a notification expiration
If a push service can't deliver a message to a device and needs to resend it (for example, if the device is turned off), you can set a notification to expire after a certain date/time.

To do this, adding the expire parameter with a UNIX timestamp to your request body. This specifies when the service should stop trying to send the notification.

<div class="admonition warning"> <p class="first admonition-title">Warning</p> <p class="last"> 
Please note that if the expire property is not set and Apple APNS or Google GCM are not able to immediately deliver your push notification, delivery will not be retried. This means your notification will not be delivered. As a best practice, you should always set an expire timestamp to ensure your notification is delivered in the event the delivery initially fails.
</p></div>

Request body syntax

    '{"expire":<unix_timestamp>,"payloads":{"<notifier_name>":"<message>"}}'
    
NOTE: The timestamp is a UNIX timestamp and is specified in seconds.


## Getting notifications

### cURL

The following are endpoints can be used to get notifications. For details on the notification properties you can get, see Notifier, Receipt, and Notification.

Getting one or more notifications:

    /notifications
    
Getting notifications associated with one or more receipts:

    /receipts/*/notification
    
Getting the list of devices associated with one or more notifications before the notifications are sent:

    /notifications/*/queue
    
## Canceling sent notifications

You can cancel a notification that's already in progress by doing the following:

    curl -X PUT "https://api.usergrid.com/my-org/sandbox/notifications/<notification-uuid>" -d '{"canceled": true}'

### Admin portal

You can view JSON for notifications by viewing their entity entries in the Data section of your application.

In the admin portal, click Data, then click the __/notifications__ collection.
You can also view notification data in the message history.

In the admin portal, click Push, then click Message History.
Locate the notification for which you want to view data, then click its view details link.

## Deleting unsent notifications

### Deleting unsent notifications with cURL

    curl -X DELETE "https://api.usergrid.com/<you-org>/<app>/notifications/<notification-uuid>"  

### Deleting unsent notifications with Admin Portal

1. In the admin portal, click Push, then Message History.
2. On the Message History page, click Scheduled.
3. In the list of scheduled messages, locate the message you want to delete.
4. In the top area of the message item, click the delete link.

## Getting receipts
After sending a notification, the BaaS generates a receipt entity that contains details about the notification. Receipts show details such as timestamps, payloads sent, and the receipt endpoints.

For information about what's contained in a receipt, see [Notifier](..//rest-endpoints/api-docs.html#notifier), [Receipt](..//rest-endpoints/api-docs.html#receipt), and [Notification](..//rest-endpoints/api-docs.html#notification). Use the following endpoints to get receipts.

To get one or more receipts:

    /receipts
    
To get receipts associated with one or more devices:

    /devices/*/receipts
    
To get receipts for one or more notifications. For example, get receipts for notifications that had errors.

    /notifications/*/receipts

## Notification endpoints
The ``/notifications`` endpoints let you create, schedule, cancel, and delete notifications. You can also use the following endpoints to accomplish the same actions for specific groups, users, devices, or any combination thereof.

Base URL: ``https://api.usergrid.com/my-org/my-app``

    /groups/*/notifications
    /groups/*/users/*/notifications
    /groups/*/users/*/devices/*/notifications
    /users/*/notifications
    /users/*/devices/*/notifications
    /devices/*/notifications
    