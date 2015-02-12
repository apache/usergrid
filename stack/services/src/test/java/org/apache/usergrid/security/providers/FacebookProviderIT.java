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

import static org.apache.usergrid.TestHelper.newUUIDString;
import static org.apache.usergrid.TestHelper.uniqueOrg;
import static org.apache.usergrid.TestHelper.uniqueUsername;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/** @author zznate */

public class FacebookProviderIT {

    private static SignInProviderFactory providerFactory;
    private static UUID applicationId;

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( );


    @BeforeClass
    public static void setup() throws Exception {
        providerFactory =  SpringResource.getInstance().getBean( SignInProviderFactory.class );
        UserInfo adminUser = setup.getMgmtSvc()
                                  .createAdminUser( uniqueUsername(), "Facebook User", "user"+newUUIDString()+"@facebook.com", "test", false,
                                          false );
        OrganizationInfo organization = setup.getMgmtSvc().createOrganization( uniqueOrg(), adminUser, true );
        applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "fb-application" ).getId();
    }


    @Test
    @Ignore("Requires Facebook credentials")
    public void verifyGetOrCreateOk() throws Exception {
        Application application = setup.getEmf().getEntityManager( applicationId ).getApplication();
        Map fb_user = MapUtils.hashMap( "id", "12345678" ).map( "name", "Facebook User" ).map( "username", "fb.user" );

        FacebookProvider facebookProvider = ( FacebookProvider ) providerFactory.facebook( application );

        String fb_access_token = "CAAE...NJIZD";
        User user1 = facebookProvider.createOrAuthenticate( fb_access_token );

        assertNotNull( user1 );
    }


    @Test
    public void verifyConfigureOk() throws Exception {
        Application application = setup.getEmf().getEntityManager( applicationId ).getApplication();
        Map fbProps = MapUtils.hashMap( "api_url", "localhost" );
        FacebookProvider fp = ( FacebookProvider ) providerFactory.facebook( application );
        assertNotNull( fp );

        fp.saveToConfiguration( "facebookProvider", fbProps );

        fp.configure();

        Map map = fp.loadConfigurationFor( "facebookProvider" );
        assertEquals( "localhost", map.get( "api_url" ) );
    }
}
