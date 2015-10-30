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
package org.apache.usergrid.persistence.queue.impl;


import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;


/**
 * Contains the helper methods for getting the Usergrid Credentials
 */
public class UsergridAwsCredentials implements AWSCredentials {
    @Override
    public String getAWSAccessKeyId() {
        String accessKey = System.getProperty( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR);
        if( StringUtils.isEmpty( accessKey )){
            accessKey = System.getProperty(SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR);
        }
        return StringUtils.trim(accessKey);
    }

    @Override
    public String getAWSSecretKey() {
        String secret = System.getProperty(SDKGlobalConfiguration.SECRET_KEY_ENV_VAR);
        if(StringUtils.isEmpty(secret)){
            secret = System.getProperty(SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR);
        }

        return StringUtils.trim(secret);
    }


    public String getBucketName(){
        String bucketName = System.getProperty( "usergrid.binary.bucketname" );

        return StringUtils.trim( bucketName );
    }
    // do these methods in json.
    public String getAWSAccessKeyIdJson(Map<String,Object> jsonObject){
        String accessKey = (String) jsonObject.get( SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR );
        if ( StringUtils.isEmpty( accessKey ) ){
            accessKey = (String) jsonObject.get( SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR );
        }

        if(StringUtils.isEmpty(accessKey)){
            throw new AmazonClientException("Could not get aws access key from json object.");
        }

        return StringUtils.trim( accessKey );
    }

    public String getAWSSecretKeyJson(Map<String,Object> jsonObject){
        String secretKey = (String) jsonObject.get( SDKGlobalConfiguration.SECRET_KEY_ENV_VAR );
        if ( StringUtils.isEmpty( secretKey ) ){
            secretKey = (String) jsonObject.get( SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR );
        }
        if(StringUtils.isEmpty(secretKey)){
            throw new AmazonClientException("Could not get aws secret key from json object.");
        }
        return StringUtils.trim( secretKey );
    }
}
