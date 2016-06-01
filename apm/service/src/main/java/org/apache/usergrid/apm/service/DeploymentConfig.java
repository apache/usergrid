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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DeploymentConfig
{

	private static final Log log = LogFactory.getLog(DeploymentConfig.class);
	private static DeploymentConfig deploymentConfig = null;

	private String accessKey;
	private String secretKey;
	private String environment; //such as dev, test, production
	private String sqsAccount;
	private String s3ConfigBucket;
	private String s3LogBucket;
	private String s3CrashLogFolder;

	private boolean enableInjestor;
	private String usergridRestUrl;
	private int timeBufferForChartData = 0;	//in minutes
	private int metricsInjestorInterval = 60; //in seconds
	
	private String ipAddressWhiteList [];

	private DeploymentConfig() {

	}
	public static DeploymentConfig geDeploymentConfig()  {
		if (deploymentConfig != null)
			return deploymentConfig;
		Properties deploymentConfigProperties = new Properties();
		try
		{
			String customPropFile = System.getProperty("apm-custom-prop-file");
			if (customPropFile == null) {
				log.info("No custom prop file given. Going to use deployment-config.properties from classpath");
				deploymentConfigProperties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("deployment-config.properties"));
			} else {
				System.out.println ("Going to use custom deployment prop file from " + customPropFile);
				log.info("Going to use custom deployment prop file from " + customPropFile);
				deploymentConfigProperties.load(new FileInputStream(customPropFile));
			}
			
			String access = deploymentConfigProperties.getProperty("AWSAccessKey");
			String secret = deploymentConfigProperties.getProperty("AWSSecretKey");
			String env  = deploymentConfigProperties.getProperty("AWSEnvironment");
			String sqsAccount = deploymentConfigProperties.getProperty("AWSSQSAccount");
			String s3ConfigBucket = deploymentConfigProperties.getProperty("AWSS3ConfigBucket");
			String s3LogBucket=deploymentConfigProperties.getProperty("AWSS3LogsBucket");
			String s3CrashFolder=deploymentConfigProperties.getProperty("AWSS3CrashFolder");

			String injestor = deploymentConfigProperties.getProperty("EnableMetricsInjestor");
			String usergridURL = deploymentConfigProperties.getProperty("UsergridRESTInternalURL");
			String timeBuffForChart = deploymentConfigProperties.getProperty("TimeBufferForChartData");
			String injInterval= deploymentConfigProperties.getProperty("MetricsInjestorTimeInterval");
			String cidrList = deploymentConfigProperties.getProperty("IpAddressWhiteList");
			//TODO: Validate the CIDR properly
			String[] ipList = null;
			if (cidrList != null && !cidrList.contains("ip.address.whitelist"))
				  ipList = cidrList.split(",");

			deploymentConfig = new DeploymentConfig();
			deploymentConfig.accessKey = access;
			deploymentConfig.secretKey = secret;
			deploymentConfig.environment = env;
			deploymentConfig.sqsAccount = sqsAccount;
			deploymentConfig.s3ConfigBucket = s3ConfigBucket;
			deploymentConfig.s3LogBucket = s3LogBucket;
			deploymentConfig.s3CrashLogFolder = s3CrashFolder;
			deploymentConfig.usergridRestUrl = usergridURL;
			deploymentConfig.timeBufferForChartData = Integer.valueOf(timeBuffForChart).intValue();
			deploymentConfig.metricsInjestorInterval = Integer.valueOf(injInterval).intValue();
			deploymentConfig.ipAddressWhiteList = ipList;
			if (injestor.equals("true"))
				deploymentConfig.enableInjestor = true;
			else
				deploymentConfig.enableInjestor = false;
			log.info("created AWS for environment " + env + " with buffer for chart " + deploymentConfig.timeBufferForChartData  + 
					" and injestor interval " + deploymentConfig.metricsInjestorInterval);
		}catch (FileNotFoundException e)
		{
			System.out.println ("This is so bad that we are going to log into console. Could not load properties file");
			log.fatal("Problem loading properties file for env variables");
			log.fatal(e);
		}
		catch (IOException e)
		{
			System.out.println ("This is so bad that we are going to log into console. Could not load properties file");
			log.fatal("Problem loading properties file for env variables");
			log.fatal(e);
		}
		catch (Exception e) {
			System.out.println ("This is so bad that we are going to log into console. Could not load properties file");
			log.fatal("Problem loading properties file for env variables");
			log.fatal(e);
		}
		return deploymentConfig;
	}

	public String getAccessKey()
	{
		return accessKey;
	}  
	

	public String getEnvironment()
	{
		return environment;
	}  	

	public String getSecretKey() {
		return secretKey;
	}

	public String getSqsAccount() {
		return sqsAccount;
	}

	public String getS3ConfigBucket() {
		return s3ConfigBucket;
	}

	public String getS3LogBucket() {
		return s3LogBucket;
	}
	
	

	public String getS3CrashLogFolder() {
		return s3CrashLogFolder;
	}


	public boolean isEnableInjestor()
	{
		return enableInjestor;
	}	

	public String getUsergridRestUrl() {
		return usergridRestUrl;
	}
	
	public int getTimeBufferForChartData() {
		return timeBufferForChartData;
	}

	public int getMetricsInjestorInterval() {
		return metricsInjestorInterval;
	}	

	public String[] getIpAddressWhiteList() {
		return ipAddressWhiteList;
	}	
	public static void main (String[] args) {
		DeploymentConfig conf = DeploymentConfig.geDeploymentConfig();
		System.out.println( conf.getAccessKey());
		//System.out.println( conf.getSecreteKey());
		System.out.println( conf.getEnvironment());
		System.out.println( conf.isEnableInjestor());      
	}

}


