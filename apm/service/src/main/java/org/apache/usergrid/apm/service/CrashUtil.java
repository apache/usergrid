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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.usergrid.apm.service.crashlogparser.AndroidCrashLogParser;
import org.apache.usergrid.apm.service.crashlogparser.CrashLogParser;
import org.apache.usergrid.apm.service.crashlogparser.iOSCrashLogParser;


public class CrashUtil {

	private static final Log log = LogFactory.getLog(CrashUtil.class);

	public static String getCrashSummary(String fullAppName, String s3CrashFileName) {

		DeploymentConfig config = DeploymentConfig.geDeploymentConfig();
		AWSCredentials credentials = new BasicAWSCredentials( config.getAccessKey(), config.getSecretKey() );
		AmazonS3Client s3Client = new AmazonS3Client( credentials );
		String s3FullFileName = AWSUtil.formS3CrashFileUrl(fullAppName, s3CrashFileName);	
		
		log.info("Crash file bucket " + config.getS3LogBucket() + " and file name : " + s3FullFileName);

		GetObjectRequest objectRequest = new GetObjectRequest(config.getS3LogBucket(), s3FullFileName);
		String crashSummary = null;
		try {
			S3Object s3Object = s3Client.getObject(objectRequest);			
			InputStream is = s3Object.getObjectContent();
			String fileContents = IOUtils.toString(is);

			CrashLogParser parser = null;
			if (fileContents != null) {
				if (s3CrashFileName.endsWith(".crash")) {
					parser = new iOSCrashLogParser();
					if (parser.parseCrashLog(fileContents))
						crashSummary = parser.getCrashSummary();
					else {
							log.error("problem parsing ios crash file for app " + fullAppName + " file: " + s3CrashFileName );
							crashSummary = "Not able to get summary for iOS crash log";
					}
				} else if (s3CrashFileName.endsWith(".stacktrace")) {
					parser = new AndroidCrashLogParser();
					if (parser.parseCrashLog(fileContents))
						crashSummary = parser.getCrashSummary();
					else {
						log.error("problem parsing Android crash file for app " + fullAppName + " file: " + s3CrashFileName );
						crashSummary = "Not able to get summary for Android crash log";
					}
				}							
			}
		} catch (AmazonServiceException e1) {
			e1.printStackTrace();
			log.error("Promblem downloading crash file from S3 for " + s3FullFileName , e1);
		} catch (Exception e) {			
			e.printStackTrace();
			log.error("Promblem downloading crash file from S3 for S3 for " + s3FullFileName, e);
		} 	
		
		log.info("Crash summary " + crashSummary);
		if (crashSummary != null && crashSummary.length() > 250) {
			crashSummary = crashSummary.substring(0, 249);
		}
		return crashSummary;
	}


}
