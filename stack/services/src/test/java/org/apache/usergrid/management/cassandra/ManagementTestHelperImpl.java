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
package org.apache.usergrid.management.cassandra;

/*******************************************************************************
 * Copyright 2010,2011 Ed Anuff and Usergrid, all rights reserved.
 ******************************************************************************/

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.management.ManagementTestHelper;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.security.tokens.TokenService;


@Component
public class ManagementTestHelperImpl implements ManagementTestHelper {
    private static final Logger logger = LoggerFactory.getLogger( ManagementTestHelperImpl.class );

    EntityManagerFactory emf;
    ManagementService management;
    TokenService tokens;
    Properties properties;


    @Override
    public void setup() throws Exception {
        String maven_opts = System.getenv( "MAVEN_OPTS" );
        logger.info( "Maven options: " + maven_opts );
    }


    @Override
    public void teardown() {
    }


    @Override
    public EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }


    @Override
    public ManagementService getManagementService() {
        return management;
    }


    @Override
    public Properties getProperties() {
        return properties;
    }


    @Override
    @Autowired
    public void setProperties( Properties properties ) {
        this.properties = properties;
    }


    @Override
    public TokenService getTokenService() {
        return tokens;
    }
}
