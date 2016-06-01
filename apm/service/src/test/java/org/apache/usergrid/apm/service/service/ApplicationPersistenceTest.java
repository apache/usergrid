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

import java.util.List;

import junit.framework.TestCase;

import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.ApplicationConfigurationModel;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.ServiceFactory;


public class ApplicationPersistenceTest extends TestCase {

	protected void setup() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testSaveApp() {
		
		App app = new App();
		app.setAppName("prabhat");
		app.setAppOwner("prabhat");
		
		ApplicationConfigurationModel model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);	
		app.setDefaultAppConfig(model);
		
		Long id = ServiceFactory.getApplicationService().saveApplication(app);
		System.out.println("model saved with id " + id);
		}

	public void testAppWithDifferentAppConfigs() {
		
	   
	   App app = new App();
      app.setAppName("prabhat1");
      app.setAppOwner("prabhat");
      

      ApplicationConfigurationModel model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);  
      app.setDefaultAppConfig(model);

		
		app.setDefaultAppConfig(model);		
				
		Long id = ServiceFactory.getApplicationService().saveApplication(app);
		System.out.println("model saved");
		
	   App appAgain = ServiceFactory.getApplicationService().getApplication(id);
		
		ApplicationConfigurationModel m2 = appAgain.getDefaultAppConfig();
		System.out.println("number of regex " + m2.getUrlRegex().size());
		assertTrue("no of regex for model",m2.getUrlRegex().size() ==2);
		
		
	   ServiceFactory.getApplicationService().updateApplication(appAgain);
	    
	    ApplicationConfigurationModel m3 = ServiceFactory.getApplicationService().getApplication(id).getDefaultAppConfig();
	    assertTrue("no of regex for model",m3.getUrlRegex().size() ==3);
	   // assertTrue(m2.getAppConfigId().equals(m3.getAppConfigId()));
		
	}
	
	public void testGetAllApps () {
		List<App> apps = ServiceFactory.getApplicationService().getAllApps();
		System.out.println("total number of apps " + apps.size());
		for (int i = 0; i < apps.size(); i ++) {
			System.out.println(apps.get(i).toString());
			
		}
	}
	
	/*public void testDeleteRegex() {
		TestUtil.deleteLocalDB();
		ApplicationConfigurationModel model = new ApplicationConfigurationModel();
		model.setDescription("modelDelete");		

		AppConfigURLRegex regex1 = new AppConfigURLRegex();
		regex1.setRegex("123");
		regex1.setApplication(model);

		AppConfigURLRegex regex2 = new AppConfigURLRegex();
		regex2.setRegex("prabhat");
		regex2.setApplication(model);

		Set<AppConfigURLRegex> set = new HashSet<AppConfigURLRegex> ();
		set.add(regex1);
		set.add(regex2);
		model.setUrlRegex(set);		
		
		Long id = ServiceFactory.getApplicationService().saveApplication(model);
		System.out.println("modelDelete saved");
		
		ApplicationConfigurationModel mod = ServiceFactory.getApplicationService().getApplicationConfiguration(id);
		assertTrue("size of regex is 2 before deleting", mod.getUrlRegex().size() == 2);
		Iterator<AppConfigURLRegex> it = mod.getUrlRegex().iterator();
		while(it.hasNext()) {
			it.next().setApplication(null);
			//mod.getUrlRegex().remove(it.next());
		}
		ServiceFactory.getApplicationService().updateApplicationConfiguration(mod);
		System.out.println("size after removing the regex" + ServiceFactory.getApplicationService().getApplicationConfiguration(id).getUrlRegex().size());
        assertTrue("size of regex is zero after deleting", ServiceFactory.getApplicationService().getApplicationConfiguration(id).getUrlRegex().size() == 0);
		
	}
	
	public void testAddRegex()  {
		TestUtil.deleteLocalDB();
		ApplicationConfigurationModel model = new ApplicationConfigurationModel();
		model.setDescription("modelAdd");		

		AppConfigURLRegex regex1 = new AppConfigURLRegex("alan",model);
		AppConfigURLRegex regex2 = new AppConfigURLRegex("eejot", model);				
		
		Long id = ServiceFactory.getApplicationService().saveApplication(model);
		System.out.println("modelAdd saved");
		
		ApplicationConfigurationModel mod = ServiceFactory.getApplicationService().getApplicationConfiguration(id);
		assertTrue("size of regex is 2 before addition", mod.getUrlRegex().size() == 2);
		mod.addUrlRegex(new AppConfigURLRegex("re-alan",mod));
		ServiceFactory.getApplicationService().updateApplicationConfiguration(mod);
		System.out.println("size after adding the regex" + ServiceFactory.getApplicationService().getApplicationConfiguration(id).getUrlRegex().size());
        assertTrue("size of regex is 3 after adding", ServiceFactory.getApplicationService().getApplicationConfiguration(id).getUrlRegex().size() == 3);
	}
*/	
	public void testGetAppByOwner() {
		
		App app1 = new App();
		app1.setDescription("modelAdd");
		app1.setAppOwner("eejot");
		app1.setAppName("eejot");
		
		ApplicationConfigurationModel model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);  
	   app1.setDefaultAppConfig(model);
		
		App app2 = new App();
		app2.setDescription("modelAdd");
		app2.setAppOwner("eejot");
		app2.setAppName("eejot1");
	
		model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);  
      app2.setDefaultAppConfig(model);
		
		App app3 = new App();
		app3.setDescription("modelAdd");
		app3.setAppOwner("not-eejot");
		app3.setAppName("eejot3");
		
		model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);  
      app3.setDefaultAppConfig(model);
		
		ServiceFactory.getApplicationService().saveApplication(app1);
		ServiceFactory.getApplicationService().saveApplication(app2);
		ServiceFactory.getApplicationService().saveApplication(app3);
		
		List<App> ms = ServiceFactory.getApplicationService().getApplicationsForOrg("eejot");
		System.out.println("size of list " +  ms.size());
        assertTrue("no of apps by owner should be 2", ms.size() == 2);		
	}
	
	  public void testGetAppByOwnerForDeleted() {
	      
	      App app1 = new App();
	      app1.setDescription("modelAdd");
	      app1.setAppOwner("eejot00");
	      app1.setAppName("eejot00");
	      
	      ApplicationConfigurationModel model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);  
	      app1.setDefaultAppConfig(model);
	      
	      App app2 = new App();
	      app2.setDescription("modelAdd");
	      app2.setAppOwner("eejot00");
	      app2.setAppName("eejot01");
	   
	      model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);  
	      app2.setDefaultAppConfig(model);
	      
	      App app3 = new App();
	      app3.setDescription("modelAdd");
	      app3.setAppOwner("not-eejot");
	      app3.setAppName("eejot03");
	      
	      model = new ApplicationConfigurationModel(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);  
	      app3.setDefaultAppConfig(model);
	      
	      ServiceFactory.getApplicationService().saveApplication(app1);
	      ServiceFactory.getApplicationService().saveApplication(app2);
	      ServiceFactory.getApplicationService().saveApplication(app3);
	      
	      ServiceFactory.getApplicationService().deleteApplication(app2.getInstaOpsApplicationId());
	      
	      List<App> ms = ServiceFactory.getApplicationService().getApplicationsForOrg("eejot00");
	      System.out.println("size of list " +  ms.size());
	        assertTrue("no of apps by owner after deletion should be 1", ms.size() == 1);     
	   }
	
	

}
