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

import java.util.Date;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.Statement.Effect;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.auth.policy.conditions.IpAddressCondition;
import com.amazonaws.auth.policy.resources.S3ObjectResource;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;


public class AWSUtil {


	//sample queue name: https://queue.amazonaws.com/366243268945/production_apm_orgName_appName
	//sample crash file name:https://s3.amazonaws.com/instaops_logs/prod/crashLogs/orgName_appName/34623729-0069-44C3-8E8A-8EA92936A0F5.crash


	public static String AWSEnv = DeploymentConfig.geDeploymentConfig().getEnvironment();

	public static String AWSCrashLogFolder = DeploymentConfig.geDeploymentConfig().getS3CrashLogFolder();

	public static String AWSAcountId = DeploymentConfig.geDeploymentConfig().getSqsAccount();
	
	public static String AWSCrashS3Bucket = DeploymentConfig.geDeploymentConfig().getS3LogBucket();


	//arg 0 = accountNumber, arg 1 = AWS_ENV, arg 2 = orgName_appName
	public static String SQS_QUEUE_NAME_FORMAT_ABSOLUTE = "https://queue.amazonaws.com/%s/%s_apm_%s"; //accountNumber/prod_apm_orgName_appName
	public static String SQS_QUEUE_NAME_FORMAT="%s_apm_%s";

	//environment-name/crashLogFolder/orgName_appName/fileName i.e ug-max-prod/crashLogs/myOrg_myApp/xyz.stacktrace
	//For prod,  full S3 URL will be mobile-analytics-production.s3.amazaonaws.com/ug-max-prod/crashLogs/myOrg_myApp/xyz.stacktrace
	public static String S3_CRASH_LOG_FILE_FORMAT = "%s/%s/%s/%s";


	public static String formFullQueueUrl(String fullAppName)
	{
		return String.format(SQS_QUEUE_NAME_FORMAT_ABSOLUTE, AWSAcountId, AWSEnv,fullAppName);
	}

	public static String formQueueName(String fullAppName)
	{
		return String.format(SQS_QUEUE_NAME_FORMAT, AWSEnv,fullAppName);
	}




	public static String formS3CrashFileUrl(String fullAppName, String fileName) {
		String formattedS3CrashFileName = String.format (S3_CRASH_LOG_FILE_FORMAT,AWSEnv,AWSCrashLogFolder,fullAppName,fileName);
		return formattedS3CrashFileName;
	}

	public  static String generatePresignedURLForCrashLog(String fullAppName, String fileName) {
		DeploymentConfig config = DeploymentConfig.geDeploymentConfig();
		AWSCredentials credentials = new BasicAWSCredentials( config.getAccessKey(), config.getSecretKey() );
		AmazonS3Client client = new AmazonS3Client( credentials );
		String s3FullFileName = AWSUtil.formS3CrashFileUrl(fullAppName, fileName);
		GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest( config.getS3LogBucket(), s3FullFileName, HttpMethod.GET);	    
		request.setExpiration( new Date( System.currentTimeMillis() + (120 * 60 * 1000) )); //expires in 2 hour
		return client.generatePresignedUrl( request ).toString();	   
	}
	
	public static IpAddressCondition[] getIPAddressRangeWhiteList() {
		
		String[] ipWhiteList = DeploymentConfig.geDeploymentConfig().getIpAddressWhiteList();
		IpAddressCondition[] ipAddressConditions = null;		
		
		if (ipWhiteList != null) {
			ipAddressConditions = new IpAddressCondition[ipWhiteList.length];			
			for(int i = 0; i < ipWhiteList.length; i++) {						
				ipAddressConditions[i] = new IpAddressCondition(ipWhiteList[i]);
			}
		}
		return ipAddressConditions;
		
	}

	public static String getSQSIPAddressWhiteListPolicy (String queueArn) {		
		Policy policy = null;
		IpAddressCondition[] ipAddressConditions = AWSUtil.getIPAddressRangeWhiteList();
		if (ipAddressConditions != null && ipAddressConditions.length != 0) {
			policy = new Policy().withStatements(
					new Statement(Effect.Allow)
					.withPrincipals(Principal.AllUsers)
					.withActions(SQSActions.SendMessage)				
					.withConditions(ipAddressConditions)
					.withResources(new Resource(queueArn)));			 
		} else {
			policy = new Policy().withStatements(
					new Statement(Effect.Allow)
					.withPrincipals(Principal.AllUsers)
					.withActions(SQSActions.SendMessage)
					.withResources(new Resource(queueArn)));
		} 

		return policy.toJson();
	}
	
	public static String getS3IPAddressWhiteListPolicy (String s3Bucket) {		
		Policy policy = null;
		IpAddressCondition[] ipAddressConditions = AWSUtil.getIPAddressRangeWhiteList();
		if (ipAddressConditions != null  && ipAddressConditions.length != 0) {
			policy = new Policy().withStatements(
					new Statement(Effect.Allow)
					.withPrincipals(Principal.AllUsers)
					.withActions(S3Actions.PutObject, S3Actions.GetObject)				
					.withConditions(ipAddressConditions)
					.withResources(new S3ObjectResource(s3Bucket,"*")));	 
		} else {
			policy = new Policy().withStatements(
					new Statement(Effect.Allow)
					.withPrincipals(Principal.AllUsers)
					.withActions(S3Actions.PutObject, S3Actions.GetObject)		
					.withResources(new S3ObjectResource(s3Bucket,"*")));	 
		} 

		return policy.toJson();
	}

}
