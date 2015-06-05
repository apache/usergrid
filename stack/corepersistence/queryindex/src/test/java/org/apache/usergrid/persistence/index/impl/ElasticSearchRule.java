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

package org.apache.usergrid.persistence.index.impl;


import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.schema.MigrationException;
import org.apache.usergrid.persistence.core.migration.schema.MigrationManager;
import org.apache.usergrid.persistence.index.EntityIndex;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Creates our index before our tests run
 */
@Singleton
public class ElasticSearchRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger( ElasticSearchRule.class );


    @Override
    protected void before() throws MigrationException {
        logger.info( "Initializing index" );

    }
}
