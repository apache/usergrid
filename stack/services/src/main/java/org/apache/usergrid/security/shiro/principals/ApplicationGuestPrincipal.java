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
package org.apache.usergrid.security.shiro.principals;


import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.HashBiMap;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.shiro.Realm;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.tokens.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.usergrid.security.shiro.utils.SubjectUtils.getPermissionFromPath;


public class ApplicationGuestPrincipal extends PrincipalIdentifier {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserPrincipal.class);


    ApplicationInfo application;


    public ApplicationGuestPrincipal( ) {}

    public ApplicationGuestPrincipal( ApplicationInfo application ) {
        this.application = application;
    }


    public UUID getApplicationId() {
        return application.getId();
    }


    public ApplicationInfo getApplication() {
        return application;
    }


    @Override
    public String toString() {
        return application.toString();
    }


    @Override
    public void grant(
        UsergridAuthorizationInfo info,
        EntityManagerFactory emf,
        ManagementService management,
        TokenService tokens) {

        Map<UUID, String> organizationSet = HashBiMap.create();
        Map<UUID, String> applicationSet = HashBiMap.create();
        OrganizationInfo organization = null;
        ApplicationInfo application = null;

        role( info, Realm.ROLE_APPLICATION_USER );

        UUID applicationId = getApplicationId();

        EntityManager em = emf.getEntityManager( applicationId );
        try {
            String appName = ( String ) em.getProperty( em.getApplicationRef(), "name" );
            applicationSet.put( applicationId, appName );
            application = new ApplicationInfo( applicationId, appName );
        }
        catch ( Exception e ) {
        }

        grant( info, getPermissionFromPath( applicationId, "access" ) );

        try {
            Set<String> permissions = em.getRolePermissions( "guest" );
            grant( info, applicationId, permissions );
        }
        catch ( Exception e ) {
            logger.error( "Unable to get user default role permissions", e );
        }


        info.setOrganization(organization);
        info.addOrganizationSet(organizationSet);
        info.setApplication(application);
        info.addApplicationSet(applicationSet);

    }
}
