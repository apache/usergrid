package org.usergrid.android.client.callbacks;

import org.usergrid.android.client.entities.Device;

public interface DeviceRegistrationCallback extends ClientCallback<Device> {

	public void onDeviceRegistration(Device device);

	public void onException(Exception e);

}
