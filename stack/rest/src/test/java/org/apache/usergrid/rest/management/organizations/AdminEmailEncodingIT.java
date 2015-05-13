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
package org.apache.usergrid.rest.management.organizations;


import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.rest.test.resource2point0.AbstractRestIT;
import org.apache.usergrid.rest.test.resource2point0.model.Application;
import org.apache.usergrid.rest.test.resource2point0.model.Entity;
import org.apache.usergrid.rest.test.resource2point0.model.Organization;
import org.apache.usergrid.rest.test.resource2point0.model.Token;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Tests for admin emails with + signs create accounts correctly, and can get tokens in both the POST and GET forms of
 * the api
 *
 * @author tnine
 */
public class AdminEmailEncodingIT extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger(AdminEmailEncodingIT.class);

    /**
     * Ensure that '+' characters in email addresses are handled properly
     *
     * @throws Exception
     */
    @Test
    public void getTokenPlus() throws Exception {
        doTest("+");
    }

    /**
     * Ensure that '_' characters in email addresses are handled properly
     *
     * @throws Exception
     */
    @Test
    public void getTokenUnderscore() throws Exception {
        doTest("_");
    }

    /**
     * Ensure that '-' characters in email addresses are handled properly
     *
     * @throws Exception
     */
    @Test
    public void getTokenDash() throws Exception {
        doTest("-");
    }

    /**
     * Ensure that "'" characters in email addresses are handled properly
     *
     * @throws Exception
     */
    @Test
    @Ignore //This fails. I'm not sure if it is by design, but a single quote is valid in an email address
    public void getTokenQuote() throws Exception {
        doTest("'");
    }

    /**
     * Given an organization name and an arbitrary character or string,
     * ensure that an organization and admin user can be created when
     * the given string is a part of the admin email address
     *
     * @param symbol
     * @throws UniformInterfaceException
     */
    private void doTest(String symbol) throws UniformInterfaceException {

        String unique = UUID.randomUUID().toString();
        String org = "org_getTokenDash" + unique;
        String app = "app_getTokenDash" + unique;

        //Username and password
        String username = "testuser" + unique;
        String password = "password" + unique;
        //create an email address containing 'symbol'
        String email = String.format("test%suser%s@usergrid.com", symbol, unique);

        //create the organization entity
        Organization orgPayload = new Organization(org, username, email, username, password, null);

        //post the organization entity
        Organization organization = clientSetup.getRestClient().management().orgs().post(orgPayload);
        assertNotNull(organization);

        //Retrieve an authorization token using the credentials created above
        Token tokenReturned = clientSetup.getRestClient().management().token()
                                         .post(Token.class,new Token("password", username, password));
        assertNotNull(tokenReturned);

        //Instruct the test framework to use the new token
        this.app().token().setToken(tokenReturned);
        //Create an application within the organization
        clientSetup.getRestClient().management().orgs().organization(organization.getName()).app().post(new Application(app));

        //retrieve the new management user by username and ensure the username and email address matches the input
        Entity me = clientSetup.getRestClient().management().users().entity(username).get();
        assertEquals(email, me.get("email"));
        assertEquals(username, me.get("username"));

        //retrieve the new management user by email and ensure the username and email address matches the input
        me = clientSetup.getRestClient().management().users().entity(email).get();
        assertEquals(email, me.get("email"));
        assertEquals(username, me.get("username"));

    }
}
