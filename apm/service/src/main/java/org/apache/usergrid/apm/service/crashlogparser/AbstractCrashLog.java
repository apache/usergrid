package org.apache.usergrid.apm.service.crashlogparser;

/**
 * 
 * @author Paul Dardeau
 *
 */

public abstract class AbstractCrashLog
{
	private String hardwareModel;
	private String appVersion;
	private String dateTime;
	private String osVersion;


	public abstract String getCrashType();
	
	public abstract boolean crashOccurredOnSimulator();
	
	public String getHardwareModel() {
		return hardwareModel;
	}
	
	public String getAppVersion() {
		return appVersion;
	}
	
	public String getDateTime() {
		return dateTime;
	}

	public String getOsVersion() {
		return osVersion;
	}

	public void setHardwareModel(String hardwareModel) {
		this.hardwareModel = hardwareModel;
	}

	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}

	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	
	public void setOsVersion(String osVersion) {
		this.osVersion = osVersion;
	}

}
