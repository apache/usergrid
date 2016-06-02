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

import java.util.Date;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import org.apache.usergrid.apm.service.DeploymentConfig;

public class AwsS3Util {
	
	public static String generatePresignedURL(String appId, String fileName) {
		DeploymentConfig config = DeploymentConfig.geDeploymentConfig();
		AWSCredentials credentials = new BasicAWSCredentials( config.getAccessKey(), config.getSecretKey() );
	    AmazonS3Client client = new AmazonS3Client( credentials );
	    String env = config.getEnvironment();
	    String s3FullFileName =env + "/crashlog/"+appId+"/"+fileName;
	    GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest( config.getS3LogBucket(), s3FullFileName, HttpMethod.GET); 
	    
	    request.setExpiration( new Date( System.currentTimeMillis() + (120 * 60 * 1000) )); //expires in 2 hour
	    return client.generatePresignedUrl( request ).toString();
	   
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AwsS3Util.generatePresignedURL("4", "1c588de7-9a1c-44dc-8e8d-84f5905ca1fc.stacktrace");

	}

}
