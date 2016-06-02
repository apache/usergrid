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
package org.apache.usergrid.apm.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;


import org.apache.usergrid.apm.rest.AppDetailsForAPM;
import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.App;
import org.apache.usergrid.apm.model.AppConfigCustomParameter;
import org.apache.usergrid.apm.model.AppConfigOverrideFilter;
import org.apache.usergrid.apm.model.ApplicationConfigurationModel;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import junit.framework.TestCase;

/**
 * Unit test for simple App.
 */
public class APMRestServiceSmokeTest extends TestCase
{
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	private String serverURL = "http://localhost:8080/apigee-apm/org/app/apm/";
	public APMRestServiceSmokeTest( String testName )
	{
		super( testName );
	}



	public void testCreateAndGetApp()
	{

		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"appConfig");

			AppDetailsForAPM app = new AppDetailsForAPM();
			app.setAppUUID(UUID.randomUUID());
			app.setOrgUUID(UUID.randomUUID());
			app.setAppName("app");
			app.setOrgName("org");
			app.setCreatedDate(new Date());


			String input =  new ObjectMapper().writeValueAsString(app);
			System.out.println("JSON request to server for create app" + input);

			ClientResponse response = webResource.type("application/json")
					.post(ClientResponse.class, input);

			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ response.getStatus());
			}	


			System.out.println("Output for create request .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);

			//Calling to get app 

			webResource = client
					.resource(serverURL+"appConfig/1");
			response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get App .... \n");
			output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);


		} catch (Exception e) {

			fail();
			e.printStackTrace();
			

		}
	}

	public void testGetVisibleSessionChartCriteria() {

		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"sessionChartCriteria/1");
		
			ClientResponse response = webResource.type("application/x-javascript")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Visible session chart criteria .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}


	}
	
	public void testGetVisibleLogChartCriteria() {

		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"logChartCriteria/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Visible log chart criteria .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}


	}
	
	public void testGetVisibleNetworkChartCriteria() {

		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"networkChartCriteria/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Visible network chart criteria .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testGetSessionChartData() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"sessionChartData/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get sesson chart data .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testGetSessionChartDataBarChart() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"sessionChartData/1?period=3h&chartType=bar");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get sesson chart data for bar chart.... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testGetLogChartData() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"logChartData/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Log chart data .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testGetLogChartDataPieChart() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"logChartData/3?period=3h&chartType=pie");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Log chart data for pie chart.... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testNetworkChartData() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"networkChartData/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Log chart data .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testLogRawData() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"logRawData/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Raw Log data .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testCrashRawData() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"crashRawData/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Crash Log data .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testNetworkRawData() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource(serverURL+"networkRawData/1");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Get Raw Network data .... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
			
		}
		catch (Exception e) {
			fail();
			e.printStackTrace();
		}
	}
	
	public void testGetStatus() {
		try {
			Client client = Client.create();

			WebResource webResource = client.resource("http://localhost:8080/apigee-apm/apm/status");
		
			ClientResponse response = webResource.type("application/json")
					.get (ClientResponse.class);

			System.out.println("Output from Server for Getting Status.... \n");
			String output = response.getEntity(String.class);
			System.out.println(output);
			assert (response.getStatus() == HttpStatus.SC_OK);
		}
		catch (Exception e) {
			fail("testGetStatus");
			e.printStackTrace();
			
		}
		
	}
	
	public void _testCrashLogSend () {
		
        final Client client = Client.create();
 
        final WebResource webResource = client.resource(serverURL+"/crashLogs/myfile.crash");
 
        String crashContent = null;
		try {
			crashContent = readFile("ios.crash");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        ClientResponse response = webResource.type("application/json")
				.post(ClientResponse.class, crashContent);

		if (response.getStatus() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
					+ response.getStatus());
		}	

		System.out.println("Output for create request .... \n");
		String output = response.getEntity(String.class);
		System.out.println(output);
		assert (response.getStatus() == HttpStatus.SC_OK);
      
	}
	
	public void testGetRawLogData() {
		
	}
	
	private String readFile( String file ) throws IOException {
	    BufferedReader reader = new BufferedReader( new InputStreamReader(getClass().getClassLoader().getResourceAsStream(file)));
	    String         line = null;
	    StringBuilder  stringBuilder = new StringBuilder();
	    String         ls = System.getProperty("line.separator");

	    while( ( line = reader.readLine() ) != null ) {
	        stringBuilder.append( line );
	        stringBuilder.append( ls );
	    }
	    reader.close();

	    return stringBuilder.toString();
	}
	
	public void testGetAppWithEverythingInIt() throws JsonGenerationException, JsonMappingException, IOException {
		App app = new App();
		
		app.setInstaOpsApplicationId(235L);
		app.setOrgName("prabhat-org");
		app.setAppName("prabhat-app");
		app.setOrganizationUUID(UUID.randomUUID().toString());
		app.setApplicationUUID(UUID.randomUUID().toString());
		app.setAppleId(UUID.randomUUID().toString());
		app.setApplicationUUID(UUID.randomUUID().toString());
		app.setAppName("App with Everything In It");
		app.setAppOwner("Mr. Jha");
		app.setCreatedDate(new Date());
		app.setDeleted(Boolean.FALSE);
		app.setDescription("App with all fields populated so that UI JSON parsing can be tested");
		app.setEnvironment("prabhat-test");
		
		app.setGoogleId(UUID.randomUUID().toString());
		
		ApplicationConfigurationModel defaultConfig = new ApplicationConfigurationModel();
		defaultConfig.setAgentUploadIntervalInSeconds(60L);
		defaultConfig.setAppConfigType(ApigeeMobileAPMConstants.CONFIG_TYPE_DEFAULT);
		defaultConfig.setDeviceIdCaptureEnabled(Boolean.TRUE);
		defaultConfig.setDeviceModelCaptureEnabled(Boolean.TRUE);
		defaultConfig.setEnableLogMonitoring(Boolean.TRUE);
		defaultConfig.setEnableUploadWhenMobile(Boolean.TRUE);
		defaultConfig.setEnableUploadWhenRoaming(Boolean.FALSE);
		defaultConfig.setLogLevelToMonitor(ApigeeMobileAPMConstants.LOG_INFO);
		defaultConfig.setSamplingRate(100L);
		defaultConfig.setSessionDataCaptureEnabled(Boolean.TRUE);
		
		Set<AppConfigCustomParameter> customParameters = new HashSet<AppConfigCustomParameter>();
		customParameters.add(new AppConfigCustomParameter("UI", "ENABLE_SCROLLING", "TRUE"));
		customParameters.add(new AppConfigCustomParameter("NETWORK", "OPEN_REST_URL", "http://myopenserver.com/v1"));
		defaultConfig.setCustomConfigParameters(customParameters);
		
		app.setDefaultAppConfig(defaultConfig);
		
		Set<AppConfigOverrideFilter> deviceIdFilters = new HashSet<AppConfigOverrideFilter>();
		deviceIdFilters.add(new AppConfigOverrideFilter(UUID.randomUUID().toString(), ApigeeMobileAPMConstants.FILTER_TYPE_DEVICE_ID));
		deviceIdFilters.add(new AppConfigOverrideFilter(UUID.randomUUID().toString(), ApigeeMobileAPMConstants.FILTER_TYPE_DEVICE_ID));		
		app.setDeviceIdFilters(deviceIdFilters);
		
		Set<AppConfigOverrideFilter> deviceNumberFilters = new HashSet<AppConfigOverrideFilter>();
		deviceNumberFilters.add(new AppConfigOverrideFilter("512-111-222", ApigeeMobileAPMConstants.FILTER_TYPE_DEVICE_NUMBER));
		deviceNumberFilters.add(new AppConfigOverrideFilter("512-222-333", ApigeeMobileAPMConstants.FILTER_TYPE_DEVICE_NUMBER));		
		app.setDeviceNumberFilters(deviceNumberFilters);	
			
		app.setABTestingPercentage(50);
		
		String json = new ObjectMapper().writeValueAsString(app);
		System.out.println(json);
	}
	
	public void testGetDomain () throws URISyntaxException {
		String url = "http://ws.spotify.com/search/1/artist?q=zz top";
		url = url.substring(0,url.indexOf('?'));
		String domain = new URI(url).getHost();
		System.out.println (domain);
	}
}
