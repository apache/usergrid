package org.apache.usergrid.android.client.callbacks;

import org.usergrid.java.client.entities.Device;

public interface DeviceRegistrationCallback extends ClientCallback<Device> {

	public void onDeviceRegistration(Device device);

}
