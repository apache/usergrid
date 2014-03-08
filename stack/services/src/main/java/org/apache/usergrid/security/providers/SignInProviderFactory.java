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
package org.apache.usergrid.security.providers;


import org.springframework.beans.factory.annotation.Autowired;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.persistence.entities.Application;


/** @author zznate */
public class SignInProviderFactory {

    private EntityManagerFactory emf;
    private ManagementService managementService;


    @Autowired
    public void setEntityManagerFactory( EntityManagerFactory emf ) {
        this.emf = emf;
    }


    @Autowired
    public void setManagementService( ManagementService managementService ) {
        this.managementService = managementService;
    }


    public SignInAsProvider facebook( Application application ) {
        FacebookProvider facebookProvider =
                new FacebookProvider( emf.getEntityManager( application.getUuid() ), managementService );
        facebookProvider.configure();
        return facebookProvider;
    }


    public SignInAsProvider foursquare( Application application ) {
        FoursquareProvider foursquareProvider =
                new FoursquareProvider( emf.getEntityManager( application.getUuid() ), managementService );
        foursquareProvider.configure();
        return foursquareProvider;
    }


    public SignInAsProvider pingident( Application application ) {
        PingIdentityProvider pingIdentityProvider =
                new PingIdentityProvider( emf.getEntityManager( application.getUuid() ), managementService );
        pingIdentityProvider.configure();
        return pingIdentityProvider;
    }
}
