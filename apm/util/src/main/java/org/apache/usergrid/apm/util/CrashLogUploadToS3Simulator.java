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

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.UUID;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.apache.usergrid.apm.model.ApigeeMobileAPMConstants;
import org.apache.usergrid.apm.model.ClientLog;
import org.apache.usergrid.apm.service.DeploymentConfig;
import org.apache.usergrid.apm.service.ServiceFactory;

public class CrashLogUploadToS3Simulator {


	public static boolean uploadSimulatedCrashLogsToS3 (String appId, String fileName) {
		DeploymentConfig config = DeploymentConfig.geDeploymentConfig();
		AWSCredentials credentials = new BasicAWSCredentials( config.getAccessKey(), config.getSecretKey() );
		AmazonS3Client s3Client = new AmazonS3Client( credentials );
		PutObjectRequest putObjectRequest;


		String s3FullFileName =config.getEnvironment() + "/crashlog/"+appId+"/"+fileName;

		try {

			ObjectMetadata metaData = new ObjectMetadata();

			metaData.setHeader(Headers.S3_CANNED_ACL, CannedAccessControlList.AuthenticatedRead);

			putObjectRequest = new PutObjectRequest(config.getS3LogBucket(), 
					s3FullFileName, 
					new ByteArrayInputStream(fileName.getBytes("UTF-8")),null);
			PutObjectResult result = s3Client.putObject(putObjectRequest);
			return true;


		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();	         
		}	   
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Long appId = 3L; //change it
		String fileName = UUID.randomUUID().toString()+".log";
		ClientLog log = new ClientLog();
		log.setLogMessage(fileName);
		log.setLogLevel(ApigeeMobileAPMConstants.logLevelsString[ApigeeMobileAPMConstants.LOG_ASSERT]);
		log.setTag("CRASH");
		log.setTimeStamp(new Date( System.currentTimeMillis() - (3* 60 * 1000) )); //current time -3 min
		log.setAppId(appId);
		log.setDeviceId(UUID.randomUUID().toString());
		log.setDeviceModel("Some-Droid");
		log.setDevicePlatform("Android");
		log.setDeviceOperatingSystem("2.3");
		log.setApplicationVersion("1.0.0.Alpha");
		
		if (CrashLogUploadToS3Simulator.uploadSimulatedCrashLogsToS3(appId.toString(), fileName)) {
			ServiceFactory.getLogDBService().saveLog(log);
			System.out.println ("Sent a crash log named " + fileName);
		}
		

	}

}
