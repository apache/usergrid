/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.api.store.amazon;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.internal.StaticCredentialsProvider;
import com.amazonaws.services.ec2.AmazonEC2Client;


public class AmazonUtils {

    /**
     * @param accessKey
     * @param secretKey
     * @return
     */
    public static AmazonEC2Client getEC2Client( String accessKey, String secretKey ) {
        AWSCredentialsProvider provider;
        if ( accessKey != null && secretKey != null ) {
            AWSCredentials credentials = new BasicAWSCredentials( accessKey, secretKey );
            provider = new StaticCredentialsProvider( credentials );
        }
        else {
            provider = new DefaultAWSCredentialsProviderChain();
        }

        AmazonEC2Client client = new AmazonEC2Client( provider );

        ClientConfiguration configuration = new ClientConfiguration();
        configuration.setProtocol( Protocol.HTTPS );
        client.setConfiguration( configuration );
        return client;
    }


    public static String getEndpoint( String availabilityZone ) {
        // see http://docs.aws.amazon.com/general/latest/gr/rande.html#ec2_region
        if ( availabilityZone != null ) {
            if ( availabilityZone.contains( "us-east-1" ) ) {
                return "ec2.us-east-1.amazonaws.com";
            }
            else if ( availabilityZone.contains( "us-west-1" ) ) {
                return "ec2.us-west-1.amazonaws.com";
            }
            else if ( availabilityZone.contains( "us-west-2" ) ) {
                return "ec2.us-west-2.amazonaws.com";
            }
            else if ( availabilityZone.contains( "eu-west-1" ) ) {
                return "ec2.eu-west-1.amazonaws.com";
            }
            else if ( availabilityZone.contains( "ap-southeast-1" ) ) {
                return "ec2.ap-southeast-1.amazonaws.com";
            }
            else if ( availabilityZone.contains( "ap-southeast-2" ) ) {
                return "ec2.ap-southeast-2.amazonaws.com";
            }
            else if ( availabilityZone.contains( "ap-northeast-1" ) ) {
                return "ec2.ap-northeast-1.amazonaws.com";
            }
            else if ( availabilityZone.contains( "sa-east-1" ) ) {
                return "ec2.sa-east-1.amazonaws.com";
            }
            else {
                return "ec2.us-east-1.amazonaws.com";
            }
        }
        else {
            return "ec2.us-east-1.amazonaws.com";
        }
    }
}
