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
package org.apache.usergrid.apm.service.util;

import java.util.HashMap;

import org.apache.usergrid.apm.service.DeploymentConfig;

public class DemoAppClickTracker {
	private static DemoAppClickTracker instance = null;
	private HashMap<String, Boolean> emailSentForDemoApp = null;
	
	
	public static DemoAppClickTracker getInstance() {
		if (instance == null) {
			instance = new DemoAppClickTracker();
			instance.emailSentForDemoApp = new HashMap<String, Boolean>();
		}
		return instance;
	}

	public void sendEmailToNewDemoAppVisitor(String user) {
		if (user == null || user.endsWith("apigee.com"))
			return ; //we don't care if apigee employee has seen the demo app.
		
		if (emailSentForDemoApp.get(user) != null)
			return;
		else {
			emailSentForDemoApp.put(user, true);
			AsyncMailer.send(new Email ("New visitor for demo app " + user,
					"Demo Analytics visited by " + user + " in environment " +  DeploymentConfig.geDeploymentConfig().getEnvironment(),
					AsyncMailer.getMobileAnalyticsAdminEmail()));
		}
	}

    public void sendEmailForAppSetupHelp(String user) {
        if (user == null)
            return ;

        AsyncMailer.send(new Email ("User " + user + " is requesting help.",
                "User " + user + " is having problems installing the SDK and clicked the \"I have installed the SDK and need help\" button. in environment " +  DeploymentConfig.geDeploymentConfig().getEnvironment(),
                AsyncMailer.getMobileAnalyticsAdminEmail()));
    }

}
