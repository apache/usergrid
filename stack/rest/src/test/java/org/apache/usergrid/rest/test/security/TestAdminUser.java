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
package org.apache.usergrid.rest.test.security;


import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.usergrid.rest.test.resource.TestContext;


/** @author tnine */
public class TestAdminUser extends TestUser {

    /**
     * @param user
     * @param password
     * @param email
     */
    public TestAdminUser( String user, String password, String email ) {
        super( user, password, email );
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.rest.test.security.TestUser#getToken(java.lang.String, java.lang.String,
     * org.apache.usergrid.rest.test.resource.TestContext)
     */
    @Override
    protected String getToken( TestContext context ) {
        try {
            return context.management().tokenGet( user, password );
        } catch (IOException ex) {
            throw new RuntimeException("Cannot parse JSON data", ex);
        }
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.rest.test.security.TestUser#create(org.apache.usergrid.rest.test.resource.TestContext)
     */
    @Override
    protected JsonNode createInternal( TestContext context ) {
        try {
            return context.application().users().create( user, email, password );
        } catch (IOException ex) {
            throw new RuntimeException("Error reading JSON data", ex);
        }
    }
}
