/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.usergrid.apm.service;

public class Device {
	String uniqueDeviceId;
	String model;
	String platform;
	String platformVersion;
	String country;
	String networkCarrier;
	String networkType;
	String networkSubType;
	boolean inUse = false;

	public Device() {

	}	

	public Device(String uniqueDeviceId, String model, String platform,
			String platformVersion, String country, String networkCarrier,
			String networkType, boolean inUse) {

		this.uniqueDeviceId = uniqueDeviceId;
		this.model = model;
		this.platform = platform;
		this.platformVersion = platformVersion;
		this.country = country;
		this.networkCarrier = networkCarrier;
		this.networkType = networkType;
		this.inUse = inUse;
		
	}



	public String getUniqueDeviceId() {
		return uniqueDeviceId;
	}

	public void setUniqueDeviceId(String uniqueDeviceId) {
		this.uniqueDeviceId = uniqueDeviceId;
	}

	public String getModel() {
		return model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public String getPlatform() {
		return platform;
	}

	public void setPlatform(String platform) {
		this.platform = platform;
	}

	public String getPlatformVersion() {
		return platformVersion;
	}

	public void setPlatformVersion(String platformVersion) {
		this.platformVersion = platformVersion;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getNetworkCarrier() {
		return networkCarrier;
	}

	public void setNetworkCarrier(String networkCarrier) {
		this.networkCarrier = networkCarrier;
	}

	public String getNetworkType() {
		return networkType;
	}

	public void setNetworkType(String networkType) {
		this.networkType = networkType;
	}	

	public String getNetworkSubType() {
		return networkSubType;
	}

	public void setNetworkSubType(String networkSubType) {
		this.networkSubType = networkSubType;
	}

	public boolean isInUse() {
		return inUse;
	}

	public void setInUse(boolean inUse) {
		this.inUse = inUse;
	}
	
	
	



}
