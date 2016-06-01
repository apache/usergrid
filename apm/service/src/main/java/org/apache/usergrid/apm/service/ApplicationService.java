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

import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;

import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.DeviceModel;
import org.apache.usergrid.apm.model.DevicePlatform;
import org.apache.usergrid.apm.model.NetworkCarrier;
import org.apache.usergrid.apm.model.NetworkSpeed;

/**
 * 
 * The following interface controls the creation, modification, and deletion of applications and the
 * associated URLs associated with each of the applications.
 * 
 * @author prabhat
 * @author alanho
 *
 */
public interface ApplicationService {

	public static final String DEMO_APP_ORG="Demo";
	public static final String DEMO_APP_NAME="AcmeBank";
	public static final String DEMO_APP_FULL_NAME ="Demo_AcmeBank";
	public static final String ADMIN_EMAIL_NAME="mobile@apigee.com";

	/**
	 * Saves the app in DB as well as creates necessary files in cloud
	 * @param appModel
	 * @return
	 */
	App createApplication(App appModel);


	/**
	 * Only saves app in DB and should not be used directly in most cases.
	 * @param app
	 * @return
	 */
	Long saveApplication(App app);

	/**
	 * Updates the app in DB as well as updates configuration in S3
	 * @param appModel
	 */

	void updateApplication(App appModel);

	void deleteApplication(Long instaOpsApplicatonId);

	public App getApplication(Long appId);
	
	public App getDemoApp () ;
	
	public List<App> getAllApps();
	public List<App> getApplicationsForOrg(String orgName);	
	public Long getTotalApplicationsCount();	
	public App getApp(String org, String appName);	
	public App getApp(String fullAppName);
	public List<App> getAppsAddedSince(Date date);

	public boolean isNameAvailable(String appName, String appOwner) throws HibernateException;

	public Long saveNetworkCarrier(NetworkCarrier carrier) throws HibernateException ;
	public Long saveNetworkSpeed(NetworkSpeed speed) throws HibernateException ;
	public Long saveDevicePlatform (DevicePlatform p) throws HibernateException ;
	public Long saveDeviceModel (DeviceModel m) throws HibernateException ;

	public List<NetworkCarrier> getNetworkCarriers(Long appId) throws HibernateException ;
	public List<NetworkSpeed> getNetworkSpeeds(Long appId) throws HibernateException ;
	public List<DevicePlatform> getDevicePlatforms (Long appId) throws HibernateException ;
	public List<DeviceModel>    getDeviceModels (Long appId) throws HibernateException ;

	public void prepopulateDevicePlatformLookupData () ;


}
