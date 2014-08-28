
Usergrid Android Client

Experimental Android client for Usergrid. Basically uses Spring Android and
Jackson to wrap calls to Usergrid REST API. Loosely based on the Usergrid
Javascript client.

Requires the following permissions:

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

This client relies on an old copy of the Usergrid Java SDK 0.0.6, which is
included in the sdks directory, make sure you build it first.
