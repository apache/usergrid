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
package org.apache.usergrid.management.cassandra;


import java.util.Arrays;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.management.ApplicationCreator;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.exceptions.ApplicationCreationException;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;

import com.google.common.base.Preconditions;


/** @author zznate */
public class ApplicationCreatorImpl implements ApplicationCreator {

    public static final String DEF_SAMPLE_APP_NAME = "sandbox";

    private static final Logger logger = LoggerFactory.getLogger( ApplicationCreatorImpl.class );

    private final ManagementService managementService;
    private final EntityManagerFactory entityManagerFactory;
    private String sampleAppName = DEF_SAMPLE_APP_NAME;


    public ApplicationCreatorImpl( EntityManagerFactory entityManagerFactory, ManagementService managementService ) {
        this.entityManagerFactory = entityManagerFactory;
        this.managementService = managementService;
    }


    public void setSampleAppName( String sampleAppName ) {
        this.sampleAppName = sampleAppName;
    }


    @Override
    public ApplicationInfo createSampleFor( OrganizationInfo organizationInfo ) throws ApplicationCreationException {

        Preconditions.checkArgument( organizationInfo != null, "OrganizationInfo was null" );
        Preconditions.checkArgument(organizationInfo.getUuid() != null, "OrganizationInfo had no UUID");

        logger.info( "create sample app {} in: {}", sampleAppName, organizationInfo.getName() );
        ApplicationInfo info;
        try {
            info = managementService.createApplication( organizationInfo.getUuid(), sampleAppName );
        }
        catch ( Exception ex ) {
            throw new ApplicationCreationException(
                    "'" + sampleAppName + "' could not be created for organization: " + organizationInfo.getUuid(),
                    ex );
        }
        logger.info( "granting permissions for: {} in: {}", sampleAppName, organizationInfo.getName() );
        // grant access to all default collections with groups
        EntityManager em = entityManagerFactory.getEntityManager( info.getId() );
        try {
            em.grantRolePermissions( "guest", Arrays.asList( "get,post,put,delete:/**" ) );
            em.grantRolePermissions( "default", Arrays.asList( "get,put,post,delete:/**" ) );
        }
        catch ( Exception ex ) {
            throw new ApplicationCreationException(
                    "Could not grant permissions to guest for default collections in '" + sampleAppName + "'", ex );
        }
        // re-load the applicationinfo so the correct name is set
        try {
            ApplicationInfo returnInfo = managementService.getApplicationInfo(info.getId());
            returnInfo = returnInfo!=null ? returnInfo :  new ApplicationInfo(info.getId(), info.getName());
            return returnInfo;
        }
        catch ( Exception ex ) {
            throw new ApplicationCreationException( "Could not load new Application.", ex );
        }
    }
}
