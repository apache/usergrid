# Creating notifiers
When you request that a push notification be sent to your app on devices, an Usergrid notifier carries the request to the notification service (Google GCM or Apple APNs).

A notifier is represented by an entity in your Usergrid application (see the [API Docs](../rest-endpoints/api-docs.html) for reference information). It carries the credentials that authorize your request. Once a notification service has verified that your notifier contains valid credentials, it will forward your push notification to your app on devices.

You can create a notifier in two ways: using the admin portal and programmatically.

<div class="admonition note"> <p class="first admonition-title">Note</p> <p class="last"> 
For an overview of how to set up push notifications, including troubleshooting tips, see [Adding push notifications support](adding-push-support.html).
</p></div>

## Requirements
To create a notifier, you must first register your app with the appropriate notification service, as described in [Registering with a notification service](registration.html).

## Creating notifiers with the admin portal
To create a notifier with the admin portal, do the following:

1. Log in to the admin portal.
2. In the left nav, select __Push > Configuration__.
3. Click the __Apple__ or __Android__ tab.
4. If you have not already done so, retrieve your .p12 certificate (iOS apps) or API key (Android apps) by following the steps in the [Registering with a notification service](registration.html).
5. In the admin portal's Configuration page, enter values for the platform on which your mobile app will be installed.

The fields are different depending on whether you are on the Apple or Android tab:

__Fields for Apple__

<table class="usergrid-table">
<tr><td>Name this notifier</td>	<td>Enter a unique name that can be used to identify this notifiers.</td></tr>
<tr><td>Certificate</td>	<td>Click __Choose File__ to select the .p12 certificate you generated and saved to your desktop earlier in this tutorial.</td></tr>
<tr><td>Environment</td>	<td>Select the environment appropriate to your app. You may select development or production. Note that for the environment you select, you should have a separate .p12 certificate -- different certificates for development and production.</td></tr>
<tr><td>Certificate Password</td>	
<td>Enter a certificate password if one was specified when you created your .p12 certificate.</td></tr>
</table>

__Fields for Android__  

<table class="usergrid-table">
<tr><td>Name this notifier</td>	<td>Enter a unique name that can be used to identify this notifiers.</td></tr>
<tr><td>API Key</td>	
<td>Enter the API key that was generated when you registered your app with GCM. To retrieve your API key, go to the [Google API developer web site](https://code.google.com/apis/console/), then select __APIs & Auth > Credentials__.</td></tr>
</table>

6. Click __Create Notifier__. The Usergrid will create a notifier entity in the /notifiers collection. The notifier will also appear in the list of notifiers in the notifications console. 

## Creating notifiers programmatically
You can create an App BaaS notifier programmatically by sending requests to the Usergrid API.

### For Apple

    curl -X POST -i -H "Accept: application/json" -H "Accept-Encoding: gzip, deflate" -H "Authorization: Bearer YWMtFeeWEMyNEeKtbNX3o4PU0QAAAT8vzK3xz3utVZat0CosiYm75C2qpiGT79c" -F "name=applenotifier" -F "provider=apple" -F "environment=development" -F "p12Certificate=@/Users/me/dev/pushtest_dev.p12" 'https://api.usergrid.com/my-org/my-app/notifiers'

### For Google

    curl -X POST "https://api.usergrid.com/my-org/my-app/notifiers" -d '{"name":"androiddev", "provider":"google", "apiKey":"AIzaSyCkXOtBQ7A9GoJsSLqZlod_YjEfxxxxxxx"}'

## Notifier endpoints

The following are the available notifier endpoints. For details on notifier properties, see the [API Docs](../rest-endpoints/api-docs.html). 

Base URL: ``https://api.usergrid.com/my-org/my-app/``

Working with one or more notifiers:

    /notifiers
    
Working with notifiers associated with specific devices:

    /devices/{device-id}/notifier
