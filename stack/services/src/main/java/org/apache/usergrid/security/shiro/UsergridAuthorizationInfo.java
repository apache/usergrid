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

package org.apache.usergrid.security.shiro;

import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


/**
 * Extend so that we can add organizations and applications that user has access to.
 */
public class UsergridAuthorizationInfo extends SimpleAuthorizationInfo {

    Map<UUID, String> organizationSet = new HashMap<>();
    Map<UUID, String> applicationSet = new HashMap<>();
    OrganizationInfo organization = null;
    ApplicationInfo application = null;


    /**
     * Default no-argument constructor.
     */
    public UsergridAuthorizationInfo() { }


    /**
     * Creates a new instance with the specified roles and no permissions.
     * @param roles the roles assigned to the realm account.
     */
    public UsergridAuthorizationInfo(Set<String> roles) {
        this.roles = roles;
    }

    public Map<UUID, String> getOrganizationSet() {
        return organizationSet;
    }

    public Map<UUID, String> getApplicationSet() {
        return applicationSet;
    }

    public void setOrganization(OrganizationInfo organization) {
        if ( organization != null ) {
            this.organization = organization;
        }
    }

    public OrganizationInfo getOrganization() {
        return organization;
    }

    public void setApplication(ApplicationInfo application) {
        if ( application != null ) {
            this.application = application;
        }
    }

    public ApplicationInfo getApplication() {
        return application;
    }

    public void addApplicationSet(Map<UUID, String> applicationSet) {
        this.applicationSet.putAll( applicationSet );
    }

    public void addOrganizationSet(Map<UUID, String> organizationSet) {
        this.organizationSet.putAll( organizationSet );
    }

    @Override
    public String toString() {
        String orgName = null;
        if ( organization != null ) {
            orgName = organization.getName();
        }
        String appName = null;
        if ( application != null ) {
            orgName = application.getName();
        }
        return "{org: " + orgName + " app: " + appName + " orgs: " + organizationSet + " apps: " + applicationSet + "}";
    }
}
