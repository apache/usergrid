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
package org.apache.usergrid.apm.service.service;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.ApplicationConfigurationModel;
import org.apache.usergrid.apm.service.ApplicationService;
import org.apache.usergrid.apm.service.ApplicationServiceImpl;
import org.apache.usergrid.apm.service.ServiceFactory;

public class ApplicationServiceImplTest extends TestCase {

	private static final Log log = LogFactory.getLog(ApplicationServiceImplTest.class);


	ApplicationService applicationService;
	ApplicationServiceImpl applicationServiceImpl;
	ApplicationConfigurationModel model;

	String temporarilyCreatedUrl;

	protected void setUp() throws Exception {
		super.setUp();

		/*model = new ApplicationConfigurationModel();

		model.setDescription("HelloWorld");

		model.setAppName("WordPress");
		model.setAppOwner("Matt Mullen");
		
		AppConfigURLRegex regex1 = new AppConfigURLRegex("http://karlunho.wordpress.com/.*", model);
		AppConfigURLRegex regex2 = new AppConfigURLRegex(".*", model);
		AppConfigURLRegex regex3 = new AppConfigURLRegex("http://www.google.com/.*", model);
		
		
		TestUtil.deleteLocalDB();
*/
		applicationService = ServiceFactory.getApplicationService();
		applicationServiceImpl = (ApplicationServiceImpl)applicationService;
	}

	protected void tearDown() throws Exception {
		super.tearDown();


		if(temporarilyCreatedUrl != null)
		{
			AmazonSQS amazonSQS = applicationServiceImpl.getSqsClient();

			DeleteQueueRequest deleteQueueRequest = new DeleteQueueRequest();

			deleteQueueRequest.setQueueUrl(temporarilyCreatedUrl);

			amazonSQS.deleteQueue(deleteQueueRequest);
			log.info("Deleted Temporary Queue : " + temporarilyCreatedUrl );
			temporarilyCreatedUrl = null;
		}
		TestUtil.deleteLocalDB();
	}

		
	public void testCreateCompositeAppS3ConfigurationSimplest()
   {
/*
      App app = new App ();
      app.setApplicationId(Long.parseLong("12"));      
      app.setAppName("myapp");
      app.setAppOwner("coolguy");
      
      Set<AppConfigOverrideFilter> deviceIdFilters = new HashSet<AppConfigOverrideFilter>();
      
      AppConfigOverrideFilter deviceIdFilter = new AppConfigOverrideFilter();
      deviceIdFilter.setFilterType(FILTER_TYPE.DEVICE_ID);
      deviceIdFilter.setFilterValue("HelloWorld");
      deviceIdFilters.add(deviceIdFilter);
      
      app.setDeviceIdFilters(deviceIdFilters);
      
      ApplicationConfigurationModel defaultModel = new ApplicationConfigurationModel();
      defaultModel.setDescription("default config");
      defaultModel.setCacheConfig(new CacheConfig());
      
      app.setDefaultAppConfig(defaultModel);
      
      applicationServiceImpl.createS3Configuration(app);
      */
      
   }
   

   public void testCreateCompositeAppSimplest()
   {

      App app = new App ();
            
      app.setAppName("myapp1");
      app.setAppOwner("coolguy1");
      app.setOrgName("eejot");
      
      
      ApplicationConfigurationModel defaultModel = new ApplicationConfigurationModel();
      
      defaultModel.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
      
      app.setDefaultAppConfig(defaultModel);  
      
      applicationServiceImpl.createApplication(app);
      assertTrue(app.getInstaOpsApplicationId() != null);
     //assertTrue (app.getDefaultAppConfig().getAppConfigId() != null);       
      
   }
   
   public void testSQSIPWhiteListPolicy() {
	   applicationServiceImpl.createSQSQueue("prabhat_junk");
   }
   
   public void testAppCount() {
	   Long x = ServiceFactory.getApplicationService().getTotalApplicationsCount();
	   assert (x !=0);
   }
   
   public void testCreateCompositeAppWithCustomConfigurations()
   {

     /* App app = new App ();
            
      app.setAppName("myapp11");
      app.setAppOwner("coolguy1");
      
      ApplicationConfigurationModel defaultModel = new ApplicationConfigurationModel();
      
      defaultModel.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
      
      AppConfigCustomParameter parameter = new AppConfigCustomParameter();
      parameter.setAppConfig(defaultModel);
      parameter.setTag("FEATURE");
      parameter.setParamKey("Hello");
      parameter.setParamValue("World");
      
      defaultModel.addCustomParameter(parameter);
      
      app.setDefaultAppConfig(defaultModel);  
      
      applicationServiceImpl.createApplication(app);
      assertTrue(app.getInstaOpsApplicationId() != null);
      assertTrue (app.getDefaultAppConfig().getAppConfigId() != null);       
      */
   }
   
   public void testCreateCompositeAppWithFilters()
   {
/*
      App app = new App ();
            
      app.setAppName("myapp2");
      app.setAppOwner("coolguy2");
      
     ApplicationConfigurationModel defaultModel = new ApplicationConfigurationModel();
      defaultModel.setDescription("default config");
      defaultModel.setCacheConfig(new CacheConfig());
      defaultModel.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
   
      
      AppConfigURLRegex regex1 = new AppConfigURLRegex("xyz",defaultModel);
      AppConfigURLRegex regex2 = new AppConfigURLRegex("abc",defaultModel);
      app.setDefaultAppConfig(defaultModel);  
      
      Set<AppConfigOverrideFilter> deviceNumRegexFilters = new HashSet<AppConfigOverrideFilter>();
      deviceNumRegexFilters.add(new AppConfigOverrideFilter("512", AppConfigOverrideFilter.FILTER_TYPE.DEVICE_NUMBER, app));
      deviceNumRegexFilters.add(new AppConfigOverrideFilter("205", AppConfigOverrideFilter.FILTER_TYPE.DEVICE_NUMBER, app));      
      
      
      app = applicationServiceImpl.createApplication(app);
      assertTrue( app.getInstaOpsApplicationId() != null);
      assertTrue (app.getDefaultAppConfig().getAppConfigId() != null);
      assertEquals("No of device numbers filters", app.getAppConfigOverrideFilters().size(),2);
   */   
   }
   
   public void testUpdateCompositeAppS3Configuration()  {
      /*
      App app = new App ();      
      app.setAppName("myapp3");
      app.setAppOwner("coolguy3");
      
      ApplicationConfigurationModel defaultModel = new ApplicationConfigurationModel();
      defaultModel.setDescription("default config");
      defaultModel.setCacheConfig(new CacheConfig());
      defaultModel.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);      
      app.setDefaultAppConfig(defaultModel);
      
      app = applicationServiceImpl.createApplication(app);  
      
      assertTrue(app.getInstaOpsApplicationId() != null);
      assertTrue(app.getDefaultAppConfig().getAppConfigId() != null);
      Long id = app.getInstaOpsApplicationId();
      Long appConfigId = app.getDefaultAppConfig().getAppConfigId();
            
      System.out.println("Got newly saved app");
      XStream xStream = HibernateXStream.getHibernateXStream();
      xStream.alias("compositeApp", App.class);
      xStream.alias("appConfig", ApplicationConfigurationModel.class);
      xStream.alias("urlRegex", AppConfigURLRegex.class);
      xStream.alias("overRideFilter", AppConfigOverrideFilter.class);
      String xml = xStream.toXML(app);
      App appFromXstream = (App) xStream.fromXML(xml);
      System.out.println("Was able to deserialize the newly created app");
      assertTrue ("Pre xstream and post xstream have same app id", app.getInstaOpsApplicationId().equals(appFromXstream.getInstaOpsApplicationId()));
      Date lastModified = appFromXstream.getLastModifiedDate(); 
      assertEquals(id.longValue(), appFromXstream.getInstaOpsApplicationId().longValue());
      assertEquals (appConfigId.longValue(), appFromXstream.getDefaultAppConfig().getAppConfigId().longValue());
      app.setMonitoringDisabled(true);
      applicationService.updateApplication(app);
      String xmlAgain = xStream.toXML(app);
      App modFromXstreamAgain = (App) xStream.fromXML(xmlAgain);
      System.out.println("Was able to deserialize the updated app");
      assertTrue("Last modified date got updated ", lastModified.before(modFromXstreamAgain.getLastModifiedDate()));
      */
      
   }
   
     
   public void testPrePopulateDBForDeviceModel() {
      ServiceFactory.getApplicationService().prepopulateDevicePlatformLookupData();
   }

	
	/**
	 * this is primarily for xstream problem with date data type where there is difference between creating a new configuration
	 * and updating configuration
	 */
   /*
	public void _testUpdateS3Configuration()  {
		TestUtil.deleteLocalDB();
		
		ApplicationConfigurationModel mod = applicationService.createApplicationConfiguration (new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT));

		System.out.println("Got newly saved app");
		XStream xStream = HibernateXStream.getHibernateXStream();
		xStream.alias("appConfigRegex", AppConfigURLRegex.class);
		xStream.alias("applicationConfigurationModel", ApplicationConfigurationModel.class);
		String xml = xStream.toXML(mod);
		ApplicationConfigurationModel modFromXstream = (ApplicationConfigurationModel) xStream.fromXML(xml);
		System.out.println("Was able to deserialize the newly created app");
		//mod.setIsActive(false);
		applicationService.updateApplicationConfiguration(mod);
		String xmlAgain = xStream.toXML(mod);
		ApplicationConfigurationModel modFromXstreamAgain = (ApplicationConfigurationModel) xStream.fromXML(xmlAgain);
		System.out.println("Was able to deserialize the updated app");
		System.out.println("Last modified date is " + modFromXstreamAgain.getLastModifiedDate().toString());
		
	}
	
	public void testCreateApplication()
	{
		model.setAppConfigId(null);
		ApplicationConfigurationModel returnedModel = applicationService.createApplicationConfiguration(model);
		assertEquals("HelloWorld",returnedModel.getDescription());
	}
	
	public void testSaveApplicationModel() {

		ApplicationConfigurationModel model = new ApplicationConfigurationModel();
		model.setCustomConfigParameters(null);
		model.setDescription("description");
		model.setIsActive(true);
		model.setS3ConfigFile("xyz.xml");
		model.setSQSQueue("q");
		//model.setURLRegex(null);
		Long id = ServiceFactory.getApplicationService().saveApplication(model);
		assertTrue("app config model saved", id != null);
		System.out.println("app config model saved with id " + id );

	}
	
	public void testSaveApplicationModel2() {
		
		ApplicationConfigurationModel model = new ApplicationConfigurationModel();
		//model.setAppConfigurationValues(null);
		model.setDescription("description");
		model.setIsActive(true);
		

		AppConfigURLRegex regex1 = new AppConfigURLRegex("http://karlunho.wordpress.com/.*", model);
		//regex1.setId(1L);
		AppConfigURLRegex regex2 = new AppConfigURLRegex(".*", model);
		//regex2.setId(2L);
		AppConfigURLRegex regex3 = new AppConfigURLRegex("http://www.google.com/.*", model);
		//regex3.setId(3L);
		
		Long id = ServiceFactory.getApplicationService().saveApplication(model);
		assertTrue("app config model saved", id != null);
		System.out.println("app config model saved with id " + id );

	}


	public void testCreateDateForApplicationModel() {

		ApplicationConfigurationModel model = new ApplicationConfigurationModel();
		//model.setAppConfigurationValues(null);
		model.setDescription("description");
		model.setIsActive(true);
		model.setS3ConfigFile("xyz.xml");
		model.setSQSQueue("q");
		//model.setURLRegex(null);
		Long id = ServiceFactory.getApplicationService().saveApplication(model);
		ApplicationConfigurationModel model2 = ServiceFactory.getApplicationService().getApplicationConfiguration(id);
		assertTrue("created date is valid" , model2.getCreateDate() != null);
		System.out.println(model2.getCreateDate());

	}

	public void testCreateDateNotChangedForApplicationModel() {

		ApplicationConfigurationModel model = new ApplicationConfigurationModel();
		//model.setAppConfigurationValues(null);
		model.setDescription("description");
		model.setIsActive(true);
		model.setS3ConfigFile("xyz.xml");
		model.setSQSQueue("q");
		//model.setURLRegex(null);
		Long id = ServiceFactory.getApplicationService().saveApplication(model);
		System.out.println("id of the object is " + id);		
		//ServiceFactory.getSchedulerService().stop();
		ApplicationConfigurationModel model2 = ServiceFactory.getApplicationService().getApplicationConfiguration(id);
		System.out.println("id of the object is " + model2.getAppConfigId());
		Date oldDate = model2.getCreateDate();
		model2.setIsActive(false);
		ServiceFactory.getApplicationService().updateApplicationConfiguration(model2);
		ApplicationConfigurationModel model3 = ServiceFactory.getApplicationService().getApplicationConfiguration(id);
		System.out.println("id of the object is " + model3.getAppConfigId());
		assertTrue("created date is not changed" , oldDate.equals(model3.getCreateDate()));		

	}

	public void testLastModifiedUpdatedForApplicationModel() throws InterruptedException {
       
		ApplicationConfigurationModel model = new ApplicationConfigurationModel();
		//model.setAppConfigurationValues(null);
		model.setDescription("description");
		model.setIsActive(true);
		model.setS3ConfigFile("xyz.xml");
		model.setSQSQueue("q");
		//model.setURLRegex(null);
		Long id = ServiceFactory.getApplicationService().saveApplication(model);
		System.out.println("id of the object is " + id);		
		//ServiceFactory.getSchedulerService().stop();
		ApplicationConfigurationModel model2 = ServiceFactory.getApplicationService().getApplicationConfiguration(id);
		System.out.println("id of the object is " + model2.getAppConfigId());
		Thread.sleep(1000);
		Date oldDate = model2.getCreateDate();
		System.out.println("created date " + oldDate.toString());
		model2.setIsActive(false);
		ServiceFactory.getApplicationService().updateApplicationConfiguration(model2);
		ApplicationConfigurationModel model3 = ServiceFactory.getApplicationService().getApplicationConfiguration(id);
		System.out.println("id of the object is " + model3.getAppConfigId());
		System.out.println("last updated date " + model3.getLastModifiedDate().toString());
		assertTrue("last modified updated" , oldDate.before(model3.getLastModifiedDate()));		

	}
	
	public void _testCreateSQSQueue() throws InterruptedException
   {

      model.setAppConfigId((new Random()).nextLong());

      applicationServiceImpl.createSQSClient(model);

      Thread.sleep(30000);
      
      AmazonSQS amazonSQS = applicationServiceImpl.getSqsClient();

      ListQueuesResult listQueuesResult = amazonSQS.listQueues();
      
      List<String> queueURLs = listQueuesResult.getQueueUrls();

      boolean existsNewQueue = false;
      
      for(String url : queueURLs)
      {
         if(url.contains(model.getAppConfigId().toString()))
         {
            existsNewQueue = true;
            temporarilyCreatedUrl = url;
         }
      }

      assertTrue(existsNewQueue);

   }
   
   public void _testCreateSQSQueue2()
   {

      model.setAppConfigId(Long.parseLong("7520545865654955716"));

      applicationServiceImpl.createSQSClient(model);

      AmazonSQS amazonSQS = applicationServiceImpl.getSqsClient();

      ListQueuesResult listQueuesResult = amazonSQS.listQueues();

      List<String> queueURLs = listQueuesResult.getQueueUrls();

      boolean existsNewQueue = false;

      for(String url : queueURLs)
      {
         if(url.contains(model.getAppConfigId().toString()))
         {
            existsNewQueue = true;
            temporarilyCreatedUrl = url;
         }
      }

      assertTrue(existsNewQueue);

   }


*/
}
