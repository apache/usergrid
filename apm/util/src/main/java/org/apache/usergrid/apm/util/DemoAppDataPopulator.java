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

package org.apache.usergrid.apm.util;

import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.service.ApplicationService;
import org.apache.usergrid.apm.service.ServiceFactory;

public class DemoAppDataPopulator
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{

		//while (true)  {
		System.out.println ("##########################################");
		System.out.println ("#                                        #");
		System.out.println ("#  Going to insert data for demo app.    #");
		System.out.println ("#      InstaOps Inc                      #");
		System.out.println ("#                                        #");
		System.out.println ("##########################################");

		System.out.println();
		System.out.println();

		App app = null;
		Long appId;
		if (args.length == 1) {
			appId = Long.parseLong(args[0]);			
			app = new App();
			app.setInstaOpsApplicationId(appId);
			app.setAppName(ApplicationService.DEMO_APP_NAME.toLowerCase());
			app.setOrgName(ApplicationService.DEMO_APP_ORG.toLowerCase());
			app.setAppOwner(ApplicationService.ADMIN_EMAIL_NAME);
			app.setFullAppName(ApplicationService.DEMO_APP_FULL_NAME.toLowerCase());
			app.setDescription("Demo App with Simulated Data");			
			//app = ServiceFactory.getApplicationService().getApplication(appId);
			System.out.println("Going to run simulator for user given appId : " + appId + " full app name " + app.getFullAppName());	
		}
		else
		{
			System.out.println ("No appId provided as argument so will query DB to find appId to which send data to");
			System.out.println ("Checking if demo app named " + ApplicationService.DEMO_APP_FULL_NAME + " already exists");
			ApplicationService appService = ServiceFactory.getApplicationService();
			app = appService.getApp(ApplicationService.DEMO_APP_FULL_NAME);
			if (app == null) {				
				System.out.println("Demo App does not exist in this environment so going to create one");
				App newDemoApp = new App();
				newDemoApp.setAppName(ApplicationService.DEMO_APP_NAME);
				newDemoApp.setOrgName(ApplicationService.DEMO_APP_ORG);
				newDemoApp.setAppOwner(ApplicationService.ADMIN_EMAIL_NAME);
				newDemoApp.setFullAppName(ApplicationService.DEMO_APP_FULL_NAME);
				newDemoApp.setDescription("Demo App with Simulated Data");
				newDemoApp = appService.createApplication(newDemoApp);
				app = newDemoApp;
			}
		}

		
		System.out.println ("Seding simulated Data for Demo app with name " + app.getFullAppName());
		MobileToSqsSimulator.start(app, 5, 1, 20, 10); //5 sessions, 1 minute interval,expire 20% of sessions every 10 iteration

		System.out.println ("##############################");
		System.out.println ("#                            #");
		System.out.println ("#   And, WE ARE DONE         #");
		System.out.println ("#         ENJOY              #");
		System.out.println ("#                            #");
		System.out.println ("##############################");
	}

}
