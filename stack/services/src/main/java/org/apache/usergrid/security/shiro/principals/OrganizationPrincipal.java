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
import java.util.UUID;

import com.google.common.collect.HashBiMap;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.shiro.Realm;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.tokens.TokenService;


public class OrganizationPrincipal extends PrincipalIdentifier {

    OrganizationInfo organization;


    public OrganizationPrincipal() {}


    public OrganizationPrincipal( OrganizationInfo organization ) {
        this.organization = organization;
    }


    public OrganizationInfo getOrganization() {
        return organization;
    }


    public UUID getOrganizationId() {
        return organization.getUuid();
    }


    @Override
    public String toString() {
        return organization.toString();
    }

    @Override
    public UUID getApplicationId() {
        return CpNamingUtils.MANAGEMENT_APPLICATION_ID;
    }

    @Override
    public void grant(
        UsergridAuthorizationInfo info,
        EntityManagerFactory emf,
        ManagementService management,
        TokenService tokens) {

        // OrganizationPrincipals are usually only through OAuth
        // They have access to a single organization

        Map<UUID, String> organizationSet = HashBiMap.create();
        Map<UUID, String> applicationSet = HashBiMap.create();
        ApplicationInfo application = null;

        role( info, Realm.ROLE_ORGANIZATION_ADMIN );
        role( info, Realm.ROLE_APPLICATION_ADMIN );

        grant( info, "organizations:access:" + organization.getUuid() );
        organizationSet.put( organization.getUuid(), organization.getName() );

        Map<UUID, String> applications = null;
        try {
            applications = management.getApplicationsForOrganization( organization.getUuid() );
        }
        catch ( Exception e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if ( ( applications != null ) && !applications.isEmpty() ) {
            grant( info, "applications:admin,access,get,put,post,delete:" + StringUtils
                .join(applications.keySet(), ',') );

            applicationSet.putAll( applications );
        }

        info.setOrganization(organization);
        info.addOrganizationSet(organizationSet);
        info.setApplication(application);
        info.addApplicationSet(applicationSet);
    }
}
