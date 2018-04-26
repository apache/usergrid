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
package org.apache.usergrid.services.assets;

import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.services.assets.data.AWSBinaryStore;
import org.apache.usergrid.services.assets.data.BinaryStore;
import org.apache.usergrid.services.assets.data.GoogleBinaryStore;
import org.apache.usergrid.services.assets.data.LocalFileBinaryStore;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import static org.apache.usergrid.management.AccountCreationProps.PROPERTIES_USERGRID_BINARY_UPLOADER;


public class BinaryStoreFactory {

    public enum Provider{
        local,aws,google
    }

    private EntityManagerFactory entityManagerFactory;
    private Properties properties;
    private String reposLocation;
    private LocalFileBinaryStore localFileBinaryStore;
    private AWSBinaryStore awsBinaryStore;
    private GoogleBinaryStore googleCloudStorageBinaryStore;

    public BinaryStoreFactory(Properties properties, EntityManagerFactory entityManagerFactory, String reposLocation) throws IOException, GeneralSecurityException {
        this.properties = properties;
        this.entityManagerFactory = entityManagerFactory;
        this.reposLocation = reposLocation;
        this.localFileBinaryStore = new LocalFileBinaryStore(properties, entityManagerFactory, reposLocation);
        this.awsBinaryStore = new AWSBinaryStore(properties, entityManagerFactory, reposLocation);
        this.googleCloudStorageBinaryStore = new GoogleBinaryStore(properties, entityManagerFactory, reposLocation);
    }

    public synchronized BinaryStore getBinaryStore(String provider) throws IOException, GeneralSecurityException {

        provider = provider != null? provider.toLowerCase(): "";

        if( provider.isEmpty() ){

            if(properties.getProperty( PROPERTIES_USERGRID_BINARY_UPLOADER ).equals( Provider.local.name() )){
                return localFileBinaryStore;
            } else if (properties.getProperty( PROPERTIES_USERGRID_BINARY_UPLOADER ).equals( Provider.google.name() )){
                return googleCloudStorageBinaryStore;
            } else{
                return awsBinaryStore;
            }
        }

        if ( provider.equals(Provider.local.name())){
            return localFileBinaryStore;
        }
        if ( provider.equals(Provider.google.name())){
            return googleCloudStorageBinaryStore;
        }
        if( provider.equals(Provider.aws.name())){
            return awsBinaryStore;
        }

        // this for backwards compatibility because historically anything other than "local" meant AWS
        return awsBinaryStore;
    }

}
