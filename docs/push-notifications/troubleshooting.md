# Troubleshooting 

## Working with Android

### App fails to install on the emulator

* When using the emulator, be sure to note the setup steps in [Push notifications prerequisites](getting-started.html#prerequisites).

* Sometimes installation fails while the emulator is still being launched. Wait until the emulator is up and running (so that you can unlock and interact with the UI), then run the project again.

* Make sure that the order of your Java Build Path matches the order shown in [Tutorial: Push notifications sample app](tutorial.html).

### Push errors
If pressing the button to send yourself a push throws an exception or doesn't respond:

* Make sure the emulator's target matches the Google API version used in the project.

* In the IDE log, wait until you see that the device has been registered before sending yourself a push.
Sometimes waiting for the code to run and trying another click gets the push message to you.

* If you successfully send yourself a push message once but fail to again, try one of the following:

Go to the apps list on the device and launch the app from there.

OR 

Uninstall the app from the device, delete the device in the admin portal, and run the project again.

### MismatchSenderId error message from the server when it tries to send a message to your app.
The sender ID is a number you send to GCM from app code when registering with GCM for notifications. The error might be occurring because the sender ID with which your app is registering at GCM for notifications does not correlate to the API project whose API key was used to create your notifier. First, confirm the following:

* The sender ID in your app code (used when registering with GCM) is the same as your Goole API project number.

* The API key used to create your notifier is the same as the API key in your Google API project.

* The notifier name used in your app code is the same as for the notifier you created in the Usergrid.

It can be possible to make a fix (such as by correcting the sender ID in your app code) and still see this error. If you're still seeing the error, consider create a new API project and notifier, then use their new values in your code:

1. Recreate (or create anew) the Google API project. This will generate a new API key and project number. See Registering with a notification service.

2. Create a new notifier for GCM using the API key you generated with the new API project. See Creating notifiers.

3. Use the new notifier name in your code, along with a new sender ID that is that same value as the Google API project number.

### INVALID_SENDER error message
The sender ID is a number you send to GCM from app code when registering with GCM for notifications. The "sender" in this case is the Google API project you created to send notification messages. Confirm that the sender ID you're using in code is the same value as the API project number generated when you created your Google API project. See Registering with a notification service.

## Working with PhoneGap Android 

### App fails to install on the emulator

* When using the emulator, be sure to note the setup steps in [Tutorial: Push notifications sample app](tutorial.html).

* Sometimes installation fails while the emulator is still being launched. Wait until the emulator is up and running (so that you can unlock and interact with the UI), then run the project again.

* Make sure that the order of your Java Build Path matches the order shown in [Tutorial: Push notifications sample app](tutorial.html).

### Push errors
If pressing the button to send yourself a push throws an exception or doesn't respond:

* Make sure the emulator's target matches the Google API version used in the project.

* In the IDE log, wait until you see that the device has been registered before sending yourself a push.
Sometimes waiting for the code to run and trying another click gets the push message to you.

* If you successfully send yourself a push message once but fail to again, try one of the following:

Go to the apps list on the device and launch the app from there.

OR 

Uninstall the app from the device, delete the device using the admin portal, then run the project again.

### MismatchSenderId error message from the server when it tries to send a message to your app.
The sender ID is a number you send to GCM from app code when registering with GCM for notifications. The error might be occurring because the sender ID with which your app is registering at GCM for notifications does not correlate to the API project whose API key was used to create your notifier. First, confirm the following:

* The sender ID in your app code (used when registering with GCM) is the same as your Google API project number.

* The API key used to create your notifier is the same as the API key in your Google API project.

* The notifier name used in your app code is the same as for the notifier you created in the Usergrid.

It can be possible to make a fix (such as by correcting the sender ID in your app code) and still see this error. If you're still seeing the error, consider create a new API project and notifier, then use their new values in your code:

1. Recreate (or create anew) the Google API project. This will generate a new API key and project number. See [Registering with a notification service](registration.html).

2. Create a new notifier for GCM using the API key you generated with the new API project. See [Creating notifiers](creating-notifiers.html).

3. Use the new notifier name in your code, along with a new sender ID that is that same value as the Google API project number.

### INVALID_SENDER error message
The sender ID is a number you send to GCM from app code when registering with GCM for notifications. The "sender" in this case is the Google API project you created to send notification messages. Confirm that the sender ID you're using in code is the same value as the API project number generated when you created your Google API project. See [Registering with a notification service](registration.html).
