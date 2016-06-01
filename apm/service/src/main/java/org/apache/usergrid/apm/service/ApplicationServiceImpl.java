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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserResult;
import com.amazonaws.services.identitymanagement.model.EntityAlreadyExistsException;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.util.AsyncMailer;
import org.apache.usergrid.apm.service.util.Email;
import org.apache.usergrid.apm.model.DeviceModel;
import org.apache.usergrid.apm.model.DevicePlatform;
import org.apache.usergrid.apm.model.NetworkCarrier;
import org.apache.usergrid.apm.model.NetworkSpeed;
import com.thoughtworks.xstream.XStream;


public class ApplicationServiceImpl implements ApplicationService {

	private static final Log log = LogFactory.getLog(ApplicationServiceImpl.class);

	public static final String APP_PRINCIPLE_USER_PREFIX = "ApigeeAPM";
	public static final String POLICY_NAME = "MobileAgentPolicy";
	//   public static final String POLICY_DOCUMENT_TEMPLATE = "{  \"Statement\": [      ]}";   
	public static final String POLICY_DOCUMENT_TEMPLATE = "{  \"Statement\": [{ \"Action\": [ \"sqs:SendMessage\" ],  \"Effect\": \"Allow\", \"Resource\": [\"%s\" ] } ]}";



	private XStream xStream;

	ObjectMapper objectMapper = new ObjectMapper();	
	
	App demoApp = null;

	AmazonSQSClient sqsClient;
	public AmazonSQSClient getSqsClient() {
		return sqsClient;
	}

	public void setSqsClient(AmazonSQSClient sqsClient) {
		this.sqsClient = sqsClient;
	}


	AmazonS3 s3Client; 

	public AmazonS3 getS3Client() {
		return s3Client;
	}

	public void setS3Client(AmazonS3 s3Client) {
		this.s3Client = s3Client;
	}

	AWSCredentials awsCredentials;

	public AWSCredentials getAwsCredentials() {
		return awsCredentials;
	}

	public void setAwsCredentials(AWSCredentials awsCredentials) {
		this.awsCredentials = awsCredentials;
	}

	AmazonIdentityManagementClient identityManagementClient;


	public AmazonIdentityManagementClient getIdentityManagementClient() {
		return identityManagementClient;
	}

	public void setIdentityManagementClient(
			AmazonIdentityManagementClient identityManagementClient) {
		this.identityManagementClient = identityManagementClient;
	}
	
	public App getDemoApp () {
		if (demoApp == null)
		 demoApp =  getApp(DEMO_APP_FULL_NAME);
		if (demoApp == null)
			log.fatal("Demo App with name " + DEMO_APP_FULL_NAME + " does not exist.");
		return demoApp;
	}

	@Override
	public App createApplication(App appModel)
	{
		appModel.setEnvironment(DeploymentConfig.geDeploymentConfig().getEnvironment());
		Long applicationId = saveApplication(appModel);
		String fullAppName=appModel.getFullAppName();
		
		ServiceFactory.getNetworkMetricsChartCriteriaService().saveDefaultChartCriteriaForApp(applicationId);
		ServiceFactory.getSessionChartCriteriaService().saveDefaultChartCriteriaForApp(applicationId);
		ServiceFactory.getLogChartCriteriaService().saveDefaultChartCriteriaForApp(applicationId);
		createSQSQueue(fullAppName);
		/* following also adds security constraints on sqs queue.
		AccessKey appCustomKey = createAuthorizedAppPrinciple(applicationId, fullAppName);
		if (appCustomKey == null)			
			log.error("New app created in DB but queue and s3 buckets are not protected");
		*/

		log.info("Sending email notification in asynch way to mobile analytics dudes");
		AsyncMailer.send(new Email (
				" User " + appModel.getAppOwner() + " new app : " + appModel.getAppName(),
				" User : " + appModel.getAppOwner() + " created new app for env: " + DeploymentConfig.geDeploymentConfig().getEnvironment() + " " +  fullAppName +
				" org uuid " + appModel.getOrganizationUUID() + " app uuid " + appModel.getApplicationUUID() ,
				AsyncMailer.getMobileAnalyticsAdminEmail()));		

		return appModel;    

	}

	public Long saveApplication(App application) throws HibernateException {
		Session session = null;
		Transaction transaction = null;
		try {
			Date d = new Date();
			application.setCreatedDate(d);
			application.setLastModifiedDate(d);
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			Long generatedId = (Long)session.save(application);  
			transaction.commit();
			log.info("Application " + generatedId + " saved");
			return generatedId;
		} catch (HibernateException e) {
			log.error(e.getCause());
			transaction.rollback();
			throw new HibernateException("Cannot save application due to: " + e.getCause(), e);
		}
	}

	/**
	 *
	 * @param appModel
	 */
	protected void createS3Configuration(App appModel)
	{
		/*//temporarily unset cloudid, access key and secret key before pushing to s3
		String cloudId = appModel.getCloudAppId();
		String aKey = appModel.getAccessKey();
		String sKey = appModel.getSecretKey();
		appModel.setCloudAppId(null);
		appModel.setAccessKey(null);
		appModel.setSecretKey(null);

		//String serializedConfig = getXStream().toXML(appModel);

		writeJSONConfigurationToS3(appModel);
		//writeXMLConfigurationToS3(appModel);

		appModel.setCloudAppId(cloudId);
		appModel.setAccessKey(aKey);
		appModel.setSecretKey(sKey);
		 */

	}

	/* private void writeJSONConfigurationToS3(App appModel)
	{
		String serializedConfig = "";
		try {
			serializedConfig = objectMapper.writeValueAsString(appModel);
			writeConfigurationToS3(appModel.getOrgName()+"_"+appModel.getAppName(), serializedConfig,"json");
		} catch (JsonGenerationException e1) {
			log.error("Cannot write JSON Configuration to S3" + e1.getMessage());
		} catch (JsonMappingException e1) {
			log.error("Cannot write JSON Configuration to S3" + e1.getMessage());
		} catch (IOException e1) {
			log.error("Cannot write JSON Configuration to S3" + e1.getMessage());
		}
	}

	private void writeXMLConfigurationToS3(App appModel)
	{
		String serializedConfig = "";
		serializedConfig = getXStream().toXML(appModel);
		writeConfigurationToS3(appModel.getOrgName()+"_"+appModel.getAppName(),serializedConfig,"xml");
	}

private void writeConfigurationToS3(String appName, String serializedConfig,String formatType)
	{
		log.info("Attempting to save the following configuration to S3 : " + serializedConfig);
		PutObjectRequest putObjectRequest;

		try {

			ObjectMetadata metaData = new ObjectMetadata();

			metaData.setHeader(Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString());

			putObjectRequest = new PutObjectRequest(AWSUtil.S3_CONFIG_BUCKET_NAME, 
					awsUtil.formS3ConfigurationObjectName(appName,formatType), 
					new ByteArrayInputStream(serializedConfig.getBytes("UTF-8")),null);
			PutObjectResult result = s3Client.putObject(putObjectRequest);
			log.info("Successfully saved configuration : " + putObjectRequest.getBucketName() + ". Verision ID " + result.getVersionId());

		} catch (UnsupportedEncodingException e) {
			log.error("Error Saving Configuration to S3", e);
		}	   
	} */


	/**
	 * 
	 * @param applicationId
	 */
	public void createSQSQueue(String orgAppName)
	{
		log.info("Creating Queue for App : " + orgAppName);
		CreateQueueRequest createQueueRequest = new CreateQueueRequest();
		createQueueRequest.setQueueName(AWSUtil.formQueueName(orgAppName));		

		try{	      
			sqsClient.createQueue(createQueueRequest);
			
			//Need to do this to get QueueArn to apply right policy on that
			
			GetQueueAttributesRequest attributesRequest = new GetQueueAttributesRequest()
    		.withQueueUrl(AWSUtil.formFullQueueUrl(orgAppName))
    		.withAttributeNames("QueueArn"); 
			
			GetQueueAttributesResult attributesResult = sqsClient.getQueueAttributes(attributesRequest);
			String queueArn = attributesResult.getAttributes().get("QueueArn");

			SetQueueAttributesRequest setQueueAttributesRequest = new SetQueueAttributesRequest();			
			Map<String, String> queueAttributes = new HashMap<String,String>();
			//Increasing the max size of SQS messages.
			queueAttributes.put("MaximumMessageSize", "65536");					
			//Apply IP address white list
			String sqsPolicy = AWSUtil.getSQSIPAddressWhiteListPolicy(queueArn);
			log.info("For  queue " + queueArn + " with policy  json " + sqsPolicy);
	        queueAttributes.put("Policy", sqsPolicy);
			setQueueAttributesRequest.setAttributes(queueAttributes);
			setQueueAttributesRequest.setQueueUrl(AWSUtil.formFullQueueUrl(orgAppName));
			sqsClient.setQueueAttributes(setQueueAttributesRequest);

		} catch (AmazonServiceException ase)
		{	
			log.error ("Problem creating queue in sqs");
			log.error(ase);
		} catch (AmazonClientException ace)
		{
			log.error(ace);
		}
	}

	/*
	 * Creates an AWS user account and assigns policy allowing the AWS user to send 
	 * messages to AWS
	 */
	public AccessKey createAuthorizedAppPrinciple(Long applicationId, String orgAppName)
	{
		CreateUserRequest createUserRequest = new CreateUserRequest();

		createUserRequest.setUserName(APP_PRINCIPLE_USER_PREFIX + "_"+ orgAppName);

		createUserRequest.setRequestCredentials(awsCredentials);

		try {
			CreateUserResult createUserResult = identityManagementClient.createUser(createUserRequest);
			log.info ("cloud user id for app with " + orgAppName + " created with " + createUserResult.getUser().getUserName());
			CreateAccessKeyRequest accessKeyRequest = new CreateAccessKeyRequest();

			accessKeyRequest.setUserName(createUserResult.getUser().getUserName());

			CreateAccessKeyResult accessKeyResult= identityManagementClient.createAccessKey(accessKeyRequest);    


			//Create policy of queue

			GetQueueAttributesRequest attributesRequest = new GetQueueAttributesRequest();
			
			log.info("Going to secure sqs queue : " + AWSUtil.formFullQueueUrl(orgAppName));

			attributesRequest.setQueueUrl(AWSUtil.formFullQueueUrl(orgAppName));

			List<String> attributeNames = new ArrayList<String>();
			attributeNames.add("QueueArn");
			attributesRequest.setAttributeNames(attributeNames);

			GetQueueAttributesResult attributesResult =     
					sqsClient.getQueueAttributes(attributesRequest);

			String queueArn = attributesResult.getAttributes().get("QueueArn");

			String policy = POLICY_DOCUMENT_TEMPLATE.replace("QUEUE_ARN", queueArn);

			String formattedPolicy = String.format(POLICY_DOCUMENT_TEMPLATE, queueArn);
			log.info("Applying authorization for following AWS resources"  + formattedPolicy);

			PutUserPolicyRequest policyRequest = new PutUserPolicyRequest();

			policyRequest.setPolicyName(POLICY_NAME);

			policyRequest.setPolicyDocument(formattedPolicy);


			policyRequest.setUserName(createUserResult.getUser().getUserName());

			identityManagementClient.putUserPolicy(policyRequest);
			log.info ("User policy for queue " + queueArn + " was set");

			return accessKeyResult.getAccessKey();
		} catch (EntityAlreadyExistsException e) {

			log.error ("This should not happen in production. Swallowing the error fow now " + e.getMessage());
			log.error(e);
			return null;
		}
	}

	boolean deleteSQSQueue(String appName)
	{
		log.info("Deleting Queue for App : " + appName.toString());   
		DeleteQueueRequest deleteQueueRequest = new DeleteQueueRequest();
		deleteQueueRequest.setQueueUrl(AWSUtil.formFullQueueUrl(appName.toString()));
		try{	      
			sqsClient.deleteQueue(deleteQueueRequest);
			return true;
		} catch (AmazonServiceException ase)
		{
			if (ase.getErrorCode().equals("AWS.SimpleQueueService.NonExistentQueue"))
			{
				log.info("Queue for app : " + appName.toString() + " was probably already deleted. " + ase.getMessage());
			} else
			{
				log.error(ase);
			}
		} catch (AmazonClientException ace)
		{
			log.error(ace);
		}
		return false;
	}

	public void resetSQSQueue(String appName)
	{
		log.info("Resetting Queue : " + appName);
		boolean successfullyDeleted = deleteSQSQueue( appName);
		if(successfullyDeleted){
			try {
				Thread.sleep(70*1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		createSQSQueue(appName);
	}

	@Override
	public void updateApplication(App appModel) {
		updateApplicationInDB(appModel);
		App updatedModel = getApplication(appModel.getInstaOpsApplicationId());


		//createS3Configuration(updatedModel);


		//no need to change the SQS since application ID has not changed
		log.info("App " + appModel.getAppName() + " updated in DB");
	}

	public void updateApplicationInDB(App appModel) {
		Session session = null;
		Transaction transaction = null;
		try {
			appModel.setLastModifiedDate(new Date());
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			session.saveOrUpdate(appModel);
			transaction.commit();

			log.info("Application record " + appModel.getAppName() + " updated");
		} catch (HibernateException e) {
			transaction.rollback();

			log.error(e);
			throw new HibernateException("Cannot update Application call record.", e);
		}        
	}

	private void updateApplicationInDBSimple(App appModel) {
		Session session = null;
		Transaction transaction = null;
		try {         
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();         
			session.saveOrUpdate(appModel);
			transaction.commit();

			log.info("Application record " + appModel.getAppName() + " updated");
		} catch (HibernateException e) {
			transaction.rollback();

			log.error(e);
			throw new HibernateException("Cannot update Application call record.", e);
		}        
	}


	@Override
	/**
	 * Not really deleting it ..just marking it for deletion
	 */
	public void deleteApplication(Long applicationId) {

		log.info("Deleting Application with id: " + applicationId);
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();

			App result = (App)session.get(App.class, applicationId);
			result.setDeleted(true);
			session.update(result);
			transaction.commit();                  
		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot delete Application record : ", e);
		}  

	}

	@Override
	public App getApplication(Long instaOpsApplicationId)
	{
		if (instaOpsApplicationId == null) {
			log.error ("Can not get an applicaiton with null application Id");
			return null;
		}


		log.info("Getting Application with id: " + instaOpsApplicationId);
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			App result = (App)session.get(App.class, instaOpsApplicationId);
			transaction.commit();
			return result;

		} catch (Exception e) {       
			e.printStackTrace();       
			transaction.rollback();
			throw new HibernateException("Cannot get Application record : ", e);
		} 
	}




	@SuppressWarnings("unchecked")
	@Override
	public List<App> getAllApps()
	{

		log.info("Getting all applications");

		List<App> apps = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from App as m where m.deleted = false order by m.appName asc");			
			apps = (List<App>) query.list();
			transaction.commit();
		} catch (HibernateException e) {

			throw new HibernateException("Cannot get all applications ", e);
		} 

		return apps;
	}



	@SuppressWarnings("unchecked")
	@Override
	public Long getTotalApplicationsCount()
	{
		log.info("Getting total applications count !!");
		List<App> apps = null;
		Session session = null;
		Transaction transaction = null;
		Long count = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("Select max(instaOpsApplicationId) from App");			
			count = (Long) query.uniqueResult();
			transaction.commit();
		} catch (HibernateException e) {
			log.error("problem getting total applications count");
			throw new HibernateException("Cannot get applications. ", e);
		} 

		return count;
	}


	@SuppressWarnings("unchecked")
	@Override
	public App getApp(String org, String appName) {
		log.info("Getting app with org " + org + " app named " + appName);
		if (org != null && appName != null)
			return getApp(org+"_"+appName);
		else
			return null;
	}

	public App getApp(String fullAppName) {
		App app = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from App as m where m.fullAppName=:fullName and m.deleted = false");	
			query.setParameter("fullName", fullAppName);		
			app = (App) query.uniqueResult();
			transaction.commit();
		} catch (HibernateException e) {

			throw new HibernateException("Cannot get all applications ", e);
		} 

		return app;


	}



	@SuppressWarnings("unchecked")
	@Override
	public List<App> getApplicationsForOrg(String orgName) {
		log.info("Getting all application for org " + orgName);		

		List<App> apps = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from App as m where m.orgName= :orgName" +
					" and m.deleted = false order by m.appName asc");

			query.setParameter("appOwnerName", orgName);
			apps = (List<App>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			throw new HibernateException("Cannot get applications ", e);
		} 

		return apps;


	}

	public void prepopulateDevicePlatformLookupData () {
		ServiceFactory.prepoulateDB();
	}

	@Override
	public boolean isNameAvailable(String appName, String appOwner) throws HibernateException
	{
		if (appName == null || appName.length() == 0 || appOwner == null || appOwner.length() == 0) {
			log.error ("Empty or Null name can not be checked for availability");
			return false;
		}

		log.info("Checking for App with name : " + appName + " for owner " + appOwner);
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from App as m where m.appName = :name and m.appOwner = :owner");
			query.setParameter("name", appName);
			query.setParameter("owner", appOwner);
			App result = (App) query.uniqueResult(); 
			transaction.commit();
			if (result != null)
				return false;
			else
				return true;

		} catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Cannot check for name availability ", e);
		}


	}



	public Long saveNetworkCarrier(NetworkCarrier carrier) throws HibernateException {
		Session session = null;
		Transaction transaction = null;
		try {
			Date d = new Date();
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			Long generatedId = (Long)session.save(carrier);      
			transaction.commit();
			log.info("Network Carrier " + generatedId + " saved");
			return generatedId;
		} catch (HibernateException e) {
			log.error(e);
			transaction.rollback();
			throw new HibernateException("Cannot save Network carrier.", e);
		}
	}  


	public Long saveNetworkSpeed(NetworkSpeed speed) throws HibernateException {
		Session session = null;
		Transaction transaction = null;
		try {
			Date d = new Date();
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			Long generatedId = (Long)session.save(speed); 
			transaction.commit();
			log.info("Network Speed " + generatedId + " saved");
			return generatedId;
		} catch (HibernateException e) {
			log.error(e);
			transaction.rollback();
			throw new HibernateException("Cannot save Network Speed.", e);
		}   
	}


	public Long saveDeviceModel(DeviceModel model) throws HibernateException {
		Session session = null;
		Transaction transaction = null;
		try {
			Date d = new Date();
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			Long generatedId = (Long)session.save(model);
			transaction.commit();
			log.info("Device Model " + generatedId + " saved");
			return generatedId;
		} catch (HibernateException e) {
			log.error(e);
			throw new HibernateException("Cannot save Device model.", e);
		} 
	}   

	public Long saveDevicePlatform(DevicePlatform platform) throws HibernateException {
		Session session = null;
		Transaction transaction = null;
		try {
			Date d = new Date();
			session = ServiceFactory.getHibernateSession();       
			transaction = session.beginTransaction();
			Long generatedId = (Long)session.save(platform);   
			transaction.commit();
			log.info("Device Model " + generatedId + " saved");
			return generatedId;
		} catch (HibernateException e) {
			log.error(e);
			transaction.rollback();
			throw new HibernateException("Cannot save Device platform.", e);
		} 
	} 

	@SuppressWarnings("unchecked")
	@Override
	public List<NetworkCarrier> getNetworkCarriers(Long appId) throws HibernateException
	{
		log.info("Getting all network carriers");
		List<NetworkCarrier> cs = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from NetworkCarrier");
			cs = (List<NetworkCarrier>) query.list();
			transaction.commit();

		} catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Cannot get NetworkCarriers. ", e);
		} 

		return cs;

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<NetworkSpeed> getNetworkSpeeds(Long appId) throws HibernateException
	{
		log.info("Getting all network speeds");
		List<NetworkSpeed> ss = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from NetworkSpeed");
			ss = (List<NetworkSpeed>) query.list();

		} catch (HibernateException e) {
			throw new HibernateException("Cannot get NetworkSpeed. ", e);
		} finally {
			if (transaction!=null) {
				transaction.commit();
			}
		}

		return ss;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DevicePlatform> getDevicePlatforms(Long appId) throws HibernateException
	{
		log.info("Getting all device platforms");
		List<DevicePlatform> ps = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from DevicePlatform");
			ps = (List<DevicePlatform>) query.list();

		} catch (HibernateException e) {
			throw new HibernateException("Cannot get Device Platform. ", e);
		} finally {
			if (transaction!=null) {
				transaction.commit();
			}
		}

		return ps;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DeviceModel> getDeviceModels(Long appId) throws HibernateException
	{  
		log.info("Getting all device models");
		List<DeviceModel> ms = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from DeviceModel");
			ms = (List<DeviceModel>) query.list();

		} catch (HibernateException e) {
			throw new HibernateException("Cannot get Device Platform. ", e);
		} finally {
			if (transaction!=null) {
				transaction.commit();
			}
		}

		return ms;
	}

	@SuppressWarnings("unchecked")
	public List<App> getAppsAddedSince(Date date) {
		log.info("Getting all application added since " + date.toString());
		List<App> apps = null;
		Session session = null;
		Transaction transaction = null;
		try {
			session = ServiceFactory.getHibernateSession();
			transaction = session.beginTransaction();
			Query query = session.createQuery("from App as m where m.createdDate > :givenDate and m.deleted != true");
			query.setParameter("givenDate", date);
			apps = (List<App>) query.list();
			transaction.commit();
		} catch (HibernateException e) {
			transaction.rollback();
			throw new HibernateException("Cannot get a list of newly added applications ", e);
		} 

		return apps;
	}

	/**
	 * @param xstream the xstream to set
	 */
	public void setXStream(XStream xstream) {
		this.xStream = xstream;
	}

	/**
	 * @return the xstream
	 */
	public XStream getXStream() {
		return xStream;
	}

	


}
