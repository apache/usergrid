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
