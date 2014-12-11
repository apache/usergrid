/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid;


import java.util.UUID;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.management.ApplicationInfo;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.UserInfo;

import static org.apache.usergrid.TestHelper.newUUIDString;


/**
 * Creates a new org and admin for every method in the class.  Also creates an application
 */
public class NewOrgAppAdminRule implements TestRule {

    private final static Logger LOG = LoggerFactory.getLogger( CoreApplication.class );

    public static final String ADMIN_NAME = "Test Admin";
    public static final String ADMIN_PASSWORD = "password";

    private final ServiceITSetup setup;

    private OrganizationOwnerInfo organizationOwnerInfo;
    private ApplicationInfo applicationInfo;


    public NewOrgAppAdminRule( final ServiceITSetup setup ) {
        this.setup = setup;
    }


    @Override
    public Statement apply( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );

                try {
                    base.evaluate();
                }
                finally {
                    after( description );
                }
            }
        };
    }


    protected void after( Description description ) {
        LOG.info( "Test {}: finish with application", description.getDisplayName() );
    }


    /**
     * Get the org and admin user info
     * @return
     */
    public OrganizationOwnerInfo getOrganizationOwnerInfo() {
        return organizationOwnerInfo;
    }


    /**
     * Get the applicationInfo
     * @return
     */
    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }


    /**
     * Get the organization info
     * @return
     */
    public OrganizationInfo getOrganizationInfo(){
        return getOrganizationOwnerInfo().getOrganization();
    }


    /**
     * Get the admin info
     * @return
     */
    public UserInfo getAdminInfo(){
        return getOrganizationOwnerInfo().getOwner();
    }


    /**
     * Create the org admin and application
     */
    protected void before( Description description ) throws Exception {
        final String className = description.getClassName();
        final String methodName = description.getMethodName();
        final String uuidString = newUUIDString();

        final String orgName = className + uuidString;
        final String appName = methodName;
        final String adminUsername = uuidString;
        final String email = uuidString + "@apache.org";

        organizationOwnerInfo = createOwnerAndOrganization( orgName, adminUsername, email, ADMIN_NAME, ADMIN_PASSWORD );
        applicationInfo = createApplication( organizationOwnerInfo.getOrganization().getUuid(), appName );
    }


    /**
     * Create the org and the admin that owns it
     */
    public OrganizationOwnerInfo createOwnerAndOrganization( final String orgName, final String adminUsername,
                                                             final String adminEmail, final String adminName,
                                                             final String adminPassword ) throws Exception {
        return setup.getMgmtSvc()
                    .createOwnerAndOrganization( orgName, adminUsername, adminName, adminEmail, adminPassword, false,
                            false );
    }


    /**
     * Create the new application
     */
    public ApplicationInfo createApplication( final UUID organizationId, final String applicationName )
            throws Exception {
        return setup.getMgmtSvc().createApplication( organizationId, applicationName );
    }



}
