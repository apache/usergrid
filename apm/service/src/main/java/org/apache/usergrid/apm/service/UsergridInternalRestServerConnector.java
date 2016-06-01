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


import java.io.IOException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.AppConfigCustomParameter;



public class UsergridInternalRestServerConnector {	
	
	private static final Log log = LogFactory.getLog(UsergridInternalRestServerConnector.class);


	public static  boolean isCrashNotificationDisabled (String orgName, String appName) {
		String restServerUrl = DeploymentConfig.geDeploymentConfig().getUsergridRestUrl();
		//String restServerUrl = "http://instaops-apigee-mobile-app-prod.apigee.net/";
		restServerUrl += "/" + orgName + "/"+appName+"/apm/apigeeMobileConfig";
		log.info("Checking if crash notification is disabled for " + orgName + " app : " + appName);

		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(restServerUrl);
		HttpResponse response;
		try {
			response = httpClient.execute(getMethod);

			HttpEntity entity = response.getEntity();
			String jsonObject = EntityUtils.toString(entity);			
			ObjectMapper mapper = new ObjectMapper();
			App app = mapper.readValue(jsonObject,App.class);
			Set<AppConfigCustomParameter> parameters = app.getDefaultAppConfig().getCustomConfigParameters();
			for(AppConfigCustomParameter param : parameters)
			{
				if(param.getTag().equalsIgnoreCase("ALARM") && 
						param.getParamKey().equalsIgnoreCase("SUPPRESS_ALARMS") && 
						param.getParamValue().equalsIgnoreCase("TRUE"));
				{
					return true;
				}
			}

		} catch (ClientProtocolException e) {	
			log.error("Problem connectiong to Usergrid internal REST server " + restServerUrl);
			e.printStackTrace();
			
		} catch (IOException e) {
			log.error("Problem connectiong to Usergrid internal REST server " + restServerUrl);
			e.printStackTrace();
		}
		httpClient = null;
		return false;
	}
	
	public static void main (String[] args) {
		UsergridInternalRestServerConnector.isCrashNotificationDisabled("wesleyhales", "sandbox");
	}

}
