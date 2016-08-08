package org.apache.usergrid.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;

import org.apache.usergrid.android.callbacks.UsergridResponseCallback;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridEnums.UsergridHttpMethod;
import org.apache.usergrid.java.client.UsergridRequest;
import org.apache.usergrid.java.client.model.UsergridDevice;
import org.apache.usergrid.java.client.model.UsergridEntity;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.apache.usergrid.java.client.utils.JsonUtils;
import org.apache.usergrid.java.client.utils.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

@SuppressWarnings("unused")
public final class UsergridSharedDevice {
    @Nullable
    private static UsergridDevice sharedDevice;

    @NotNull
    private static final String USERGRID_PREFS_FILE_NAME = "usergrid_prefs.xml";
    @NotNull
    private static final String USERGRID_SHARED_DEVICE_KEY = "usergridSharedDevice";

    @NotNull
    public static UsergridDevice getSharedDevice(@NotNull final Context context) {
        if (sharedDevice == null) {
            sharedDevice = UsergridSharedDevice.getStoredSharedDevice(context);
            if (sharedDevice == null) {
                String sharedDeviceId = UsergridSharedDevice.getSharedDeviceUUID(context);
                HashMap<String, JsonNode> map = new HashMap<String, JsonNode>();
                map.put("uuid", new TextNode(sharedDeviceId));
                sharedDevice = new UsergridDevice(map);
                sharedDevice.setModel(Build.MODEL);
                sharedDevice.setPlatform("android");
                sharedDevice.setOsVersion(Build.VERSION.RELEASE);
            }
        }
        return sharedDevice;
    }

    public static void applyPushToken(@NotNull final Context context, @NotNull final String notifier, @NotNull final String token, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridSharedDevice.applyPushToken(Usergrid.getInstance(), context, notifier, token, responseCallback);
    }

    public static void applyPushToken(@NotNull final UsergridClient client, @NotNull final Context context, @NotNull final String notifier, @NotNull final String token, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridSharedDevice.getSharedDevice(context).putProperty(notifier + ".notifier.id", token);
        UsergridSharedDevice.saveSharedDeviceRemotelyAndToDisk(client, context, responseCallback);
    }

    public static void save(@NotNull final Context context, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridSharedDevice.saveSharedDevice(Usergrid.getInstance(), context, responseCallback);
    }

    public static void save(@NotNull final UsergridClient client, @NotNull final Context context, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridSharedDevice.saveSharedDevice(client, context, responseCallback);
    }

    public static void saveSharedDevice(@NotNull final Context context, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridSharedDevice.saveSharedDeviceRemotelyAndToDisk(Usergrid.getInstance(), context, responseCallback);
    }

    public static void saveSharedDevice(@NotNull final UsergridClient client, @NotNull final Context context, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridSharedDevice.saveSharedDeviceRemotelyAndToDisk(client, context, responseCallback);
    }

    @Nullable
    private static UsergridDevice getStoredSharedDevice(@NotNull final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(USERGRID_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        String deviceString = prefs.getString(USERGRID_SHARED_DEVICE_KEY, null);
        UsergridDevice storedSharedDevice = null;
        if (deviceString != null) {
            try {
                storedSharedDevice = JsonUtils.mapper.readValue(deviceString, UsergridDevice.class);
            } catch (IOException ignored) {
                prefs.edit().remove(USERGRID_SHARED_DEVICE_KEY).commit();
            }
        }
        return storedSharedDevice;
    }

    private static void saveSharedDeviceToDisk(@NotNull final Context context) {
        String deviceAsString = UsergridSharedDevice.getSharedDevice(context).toString();
        SharedPreferences prefs = context.getSharedPreferences(USERGRID_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(USERGRID_SHARED_DEVICE_KEY, deviceAsString).commit();
    }

    private static void saveSharedDeviceRemotelyAndToDisk(@NotNull final UsergridClient client, @NotNull final Context context, @NotNull final UsergridResponseCallback responseCallback) {
        UsergridDevice sharedDevice = UsergridSharedDevice.getSharedDevice(context);
        String sharedDeviceUUID = sharedDevice.getUuid() != null ? sharedDevice.getUuid() : sharedDevice.getStringProperty("uuid");
        UsergridRequest request = new UsergridRequest(UsergridHttpMethod.PUT, UsergridRequest.APPLICATION_JSON_MEDIA_TYPE, client.clientAppUrl(), null, sharedDevice, client.authForRequests(), "devices", sharedDeviceUUID);
        UsergridAsync.sendRequest(client, request, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NotNull UsergridResponse response) {
                UsergridEntity responseEntity = response.entity();
                if (response.ok() && responseEntity != null && responseEntity instanceof UsergridDevice) {
                    UsergridSharedDevice.sharedDevice = (UsergridDevice) responseEntity;
                    UsergridSharedDevice.saveSharedDeviceToDisk(context);
                }
                responseCallback.onResponse(response);
            }
        });
    }

    @NotNull
    public static String getSharedDeviceUUID(@NotNull final Context context) {
        if( sharedDevice != null && sharedDevice.getUuid() != null ) {
            return sharedDevice.getUuid();
        }

        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        UUID uuid;
        try {
            if (!"9774d56d682e549c".equals(androidId)) {
                uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
            } else {
                final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : generateDeviceUuid(context);
            }
        } catch (Exception ignored) {
            uuid = UUID.randomUUID();
        }
        return uuid.toString();
    }

    private static UUID generateDeviceUuid(Context context) {
        // Get some of the hardware information
        String buildParams = Build.BOARD + Build.BRAND + Build.CPU_ABI
                + Build.DEVICE + Build.DISPLAY + Build.FINGERPRINT + Build.HOST
                + Build.ID + Build.MANUFACTURER + Build.MODEL + Build.PRODUCT
                + Build.TAGS + Build.TYPE + Build.USER;

        // Requires READ_PHONE_STATE
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);

        // gets the imei (GSM) or MEID/ESN (CDMA)
        String imei = tm.getDeviceId();

        // gets the android-assigned id
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        // requires ACCESS_WIFI_STATE
        WifiManager wm = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);

        // gets the MAC address
        String mac = wm.getConnectionInfo().getMacAddress();

        // if we've got nothing, return a random UUID
        if (ObjectUtils.isEmpty(imei) && ObjectUtils.isEmpty(androidId) && ObjectUtils.isEmpty(mac)) {
            return UUID.randomUUID();
        }

        // concatenate the string
        String fullHash = buildParams + imei + androidId + mac;
        return UUID.nameUUIDFromBytes(fullHash.getBytes());
    }
}
