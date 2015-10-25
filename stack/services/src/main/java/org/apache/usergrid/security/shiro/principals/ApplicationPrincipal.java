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
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.shiro.Realm;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.tokens.TokenService;


public class ApplicationPrincipal extends PrincipalIdentifier {

    ApplicationInfo application;


    public ApplicationPrincipal() {}

    public ApplicationPrincipal( ApplicationInfo application ) {
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

        // ApplicationPrincipal are usually only through OAuth
        // They have access to a single application

        Map<UUID, String> organizationSet = HashBiMap.create();
        Map<UUID, String> applicationSet = HashBiMap.create();
        OrganizationInfo organization = null;
        ApplicationInfo application = null;

        role( info, Realm.ROLE_APPLICATION_ADMIN );

        application = getApplication();
        grant( info, "applications:admin,access,get,put,post,delete:" + application.getId() );
        applicationSet.put( application.getId(), application.getName().toLowerCase() );

        info.setOrganization(organization);
        info.addOrganizationSet(organizationSet);
        info.setApplication(application);
        info.addApplicationSet(applicationSet);
    }
}
