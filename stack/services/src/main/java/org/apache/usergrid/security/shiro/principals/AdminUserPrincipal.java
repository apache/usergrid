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


import com.google.common.collect.HashBiMap;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.management.*;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.shiro.Realm;
import org.apache.usergrid.security.shiro.UsergridAuthorizationInfo;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;
import org.apache.usergrid.security.tokens.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

import static org.apache.usergrid.security.shiro.utils.SubjectUtils.getPermissionFromPath;


public class AdminUserPrincipal extends UserPrincipal {
    private static final Logger logger = LoggerFactory.getLogger(AdminUserPrincipal.class);

    public AdminUserPrincipal() {
    }

    public AdminUserPrincipal( UUID managementAppId, UserInfo user ) {
        super( managementAppId, user );
    }

    @Override
    public void grant(
        UsergridAuthorizationInfo info,
        EntityManagerFactory emf,
        ManagementService management,
        TokenService tokens) {

        // AdminUserPrincipals are through basic auth and sessions
        // They have access to organizations and organization
        // applications

        UserInfo user = this.getUser();

        Map<UUID, String> organizationSet = HashBiMap.create();
        Map<UUID, String> applicationSet = HashBiMap.create();
        OrganizationInfo organization = null;
        ApplicationInfo application = null;

        boolean superUserEnabled = false;
        final String s = management.getProperties().getProperty(
            AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_ALLOWED);
        if ( s != null && "true".equalsIgnoreCase(s.trim())) {
            superUserEnabled = true;
        }

        String superUser = management.getProperties().getProperty(
            AccountCreationProps.PROPERTIES_SYSADMIN_LOGIN_NAME);

        if ( superUserEnabled && ( superUser != null ) && superUser.equals( user.getUsername() ) ) {

            // The system user has access to everything

            role(info, Realm.ROLE_SERVICE_ADMIN);
            role(info, Realm.ROLE_ORGANIZATION_ADMIN);
            role(info, Realm.ROLE_APPLICATION_ADMIN);
            role(info, Realm.ROLE_ADMIN_USER);

            grant(info, "system:access");

            grant(info, "organizations:admin,access,get,put,post,delete:*");
            grant(info, "applications:admin,access,get,put,post,delete:*");
            grant(info, "organizations:admin,access,get,put,post,delete:*:/**");
            grant(info, "applications:admin,access,get,put,post,delete:*:/**");
            grant(info, "users:access:*");

            grant(info, SubjectUtils.getPermissionFromPath(emf.getManagementAppId(), "access"));

            grant(info, SubjectUtils.getPermissionFromPath(emf.getManagementAppId(), "get,put,post,delete", "/**"));

            // get all organizations
            try {

                Map<UUID, String> allOrganizations = management.getOrganizations();

                if (allOrganizations != null) {

                    for (UUID id : allOrganizations.keySet()) {
                        grant(info, "organizations:admin,access,get,put,post,delete:" + id);
                    }
                    organizationSet.putAll(allOrganizations);

                    Map<UUID, String> allApplications =
                        management.getApplicationsForOrganizations(allOrganizations.keySet());

                    if ((allApplications != null) && ! allApplications.isEmpty()) {
                        grant(info, "applications:admin,access,get,put,post,delete:" + StringUtils
                            .join(allApplications.keySet(), ','));
                        applicationSet.putAll(allApplications);
                    }
                }
            } catch (Exception e) {
                logger.error("Unable to construct superuser permissions", e);
            }
        }

        else {

            // For regular service users, we find what organizations
            // they're associated with
            // An service user can be associated with multiple
            // organizations

            grant( info, getPermissionFromPath( emf.getManagementAppId(), "access" ) );

            // admin users cannot access the management app directly
            // so open all permissions
            grant( info, getPermissionFromPath(emf.getManagementAppId(), "get,put,post,delete", "/**") );

            role(info, Realm.ROLE_ADMIN_USER);

            try {

                Map<UUID, String> userOrganizations = management.getOrganizationsForAdminUser(user.getUuid());

                if ( userOrganizations != null ) {
                    for ( UUID id : userOrganizations.keySet() ) {
                        grant( info, "organizations:admin,access,get,put,post,delete:" + id );
                    }
                    organizationSet.putAll( userOrganizations );

                    Map<UUID, String> userApplications =
                        management.getApplicationsForOrganizations( userOrganizations.keySet() );
                    if ( ( userApplications != null ) && !userApplications.isEmpty() ) {
                        grant( info, "applications:admin,access,get,put,post,delete:" + StringUtils
                            .join(userApplications.keySet(), ',') );
                        applicationSet.putAll( userApplications );
                    }

                    role( info, Realm.ROLE_ORGANIZATION_ADMIN );
                    role( info, Realm.ROLE_APPLICATION_ADMIN );
                }
            }
            catch ( Exception e ) {
                logger.error( "Unable to construct admin user permissions", e );
            }
        }

        info.setOrganization(organization);
        info.addOrganizationSet(organizationSet);
        info.setApplication(application);
        info.addApplicationSet(applicationSet);
    }
}
