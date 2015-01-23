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


import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.entities.Application;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.utils.MapUtils;

import static junit.framework.Assert.assertNotNull;


/** @author zznate */
@Ignore("Experimental Ping Indentiyy test")

public class PingIdentityProviderIT {
    private static UserInfo adminUser;
    private static OrganizationInfo organization;
    private static UUID applicationId;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( );


    @BeforeClass
    public static void setup() throws Exception {
        adminUser = setup.getMgmtSvc()
                         .createAdminUser( "pinguser", "Ping User", "ping-user@usergrid.com", "test", false, false );
        organization = setup.getMgmtSvc().createOrganization( "ping-organization", adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "ping-application" ).getId();
    }


    @Test
    public void verifyLiveConnect() throws Exception {
        Application application = setup.getEmf().getEntityManager( applicationId ).getApplication();
        Map pingProps = MapUtils.hashMap( "api_url", "" ).map( "client_secret", "" )
                                .map( "client_id", "dev.app.appservicesvalidation" );

        PingIdentityProvider pingProvider =
                ( PingIdentityProvider ) setup.getProviderFactory().pingident( application );
        pingProvider.saveToConfiguration( pingProps );
        pingProvider.configure();
        User user = pingProvider.createOrAuthenticate( "u0qoW7TS9eT8Vmt7UzrEWrhHbhDK" );
        assertNotNull( user );
    }
}
