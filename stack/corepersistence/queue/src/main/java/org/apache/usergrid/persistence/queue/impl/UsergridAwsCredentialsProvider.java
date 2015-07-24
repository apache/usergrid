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


import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;


/**
 * Pulls the aws keys from system properties.
 */
public class UsergridAwsCredentialsProvider implements AWSCredentialsProvider {


    private AWSCredentials creds = new UsergridAwsCredentials();


    public  UsergridAwsCredentialsProvider(){
        init();
    }

    private void init() {
        if(StringUtils.isEmpty(creds.getAWSAccessKeyId())){
            throw new AmazonClientException("could not get aws access key from system properties");
        }
        if(StringUtils.isEmpty(creds.getAWSSecretKey())){
            throw new AmazonClientException("could not get aws secret key from system properties");
        }
    }

    @Override
    public AWSCredentials getCredentials() {
        return creds;
    }

    @Override
    public void refresh() {
        init();
    }
}
