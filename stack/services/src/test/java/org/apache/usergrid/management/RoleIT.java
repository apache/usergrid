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
package org.apache.usergrid.management;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.shiro.subject.Subject;

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.cassandra.ClearShiroSubject;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.entities.User;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;
import org.apache.usergrid.security.shiro.PrincipalCredentialsToken;
import org.apache.usergrid.security.shiro.utils.SubjectUtils;

import static org.junit.Assert.assertFalse;



public class RoleIT {
    private static final Logger LOG = LoggerFactory.getLogger( RoleIT.class );

    @Rule
    public ClearShiroSubject clearShiroSubject = new ClearShiroSubject();

    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( );


    @Test
    public void testRoleInactivity() throws Exception {

        OrganizationOwnerInfo ooi = setup.getMgmtSvc()
                                         .createOwnerAndOrganization( "RoleIT", "edanuff5", "Ed Anuff", "ed@anuff.com5",
                                                 "test", true, false );

        OrganizationInfo organization = ooi.getOrganization();

        UUID applicationId = setup.getMgmtSvc().createApplication( organization.getUuid(), "test-app" ).getId();
        EntityManager em = setup.getEmf().getEntityManager( applicationId );

        setup.getEntityIndex().refresh();

        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put( "username", "edanuff5" );
        properties.put( "email", "ed@anuff.com5" );
        properties.put( "activated", true );
        User user = em.create( User.ENTITY_TYPE, User.class, properties );

        em.createRole( "logged-in", "Logged In", 1000 );
        setup.getEntityIndex().refresh();
        em.addUserToRole( user.getUuid(), "logged-in" );

        String accessToken = setup.getMgmtSvc().getAccessTokenForAppUser( applicationId, user.getUuid(), 0 );

        UserInfo user_info = setup.getMgmtSvc().getAppUserFromAccessToken( accessToken );

        PrincipalCredentialsToken token =
                PrincipalCredentialsToken.getFromAppUserInfoAndAccessToken( user_info, accessToken );

        Subject subject = SubjectUtils.getSubject();
        subject.login( token );

        subject.checkRole( "application-role:" + applicationId + ":logged-in" );

        LOG.info( "Has role \"logged-in\"" );

        Thread.sleep( 1000 );

        subject.login( token );

        assertFalse( subject.hasRole( "application-role:" + applicationId + ":logged-in" ) );

        LOG.info( "Doesn't have role \"logged-in\"" );
    }
}
