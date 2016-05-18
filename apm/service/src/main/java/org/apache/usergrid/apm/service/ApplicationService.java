package org.apache.usergrid.apm.service;

import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;

import com.ideawheel.common.model.App;
import com.ideawheel.portal.model.DeviceModel;
import com.ideawheel.portal.model.DevicePlatform;
import com.ideawheel.portal.model.NetworkCarrier;
import com.ideawheel.portal.model.NetworkSpeed;

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
