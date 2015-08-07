# Android SDK 

Installing the SDK
--------------------

To initialize the Usergrid SDK, do the following:

1. Add 'usergrid-android-<version>.jar' to the build path for your project.
2. Add the following to your source code to import commonly used SDK classes:
<pre>
    import org.apache.usergrid.android.sdk.UGClient;
    import org.apache.usergrid.android.sdk.callbacks.ApiResponseCallback;
    import org.apache.usergrid.android.sdk.response.ApiResponse;  
</pre>
3. Add the following to your 'AndroidManifest.xml':
<pre>
    &lt;uses-permission android:name="android.permission.INTERNET" /&gt;    
</pre>
4. Instantiate the 'UGClient' class to initialize the Usergrid SDK:
<pre>
    //Usergrid app credentials, available in the admin portal
    String ORGNAME = "your-org";
    String APPNAME = "your-app";
    UGClient client = new UGClient(ORGNAME,APPNAME);    
</pre>

Building From Source
--------------------
To build from source, do the following:

1. Update the path in <android.libs>
2. Run <code>sh build_release_zip.sh &lt;version&gt;</code> - <version> is the SDK version number specified in UGClient.java.
