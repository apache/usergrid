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
