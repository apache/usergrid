/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.graph.guice;


import org.apache.usergrid.persistence.core.guice.CommonModule;
import org.apache.usergrid.persistence.core.guice.TestModule;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.migration.data.TestMigrationDataProvider;
import org.apache.usergrid.persistence.graph.serialization.impl.migration.GraphNode;

import com.google.inject.TypeLiteral;


/**
 * Wrapper for configuring our guice test env
 */
public class TestGraphModule extends TestModule {

    @Override
    protected void configure() {
        /**
         * Runtime modules
         */
        install( new CommonModule() );
        install( new GraphModule() {

            @Override
            public void configureMigrationProvider() {
                //configure our migration data provider

                TestMigrationDataProvider<GraphNode> migrationDataProvider = new TestMigrationDataProvider<>();
                bind( new TypeLiteral<MigrationDataProvider<GraphNode>>() {} )
                        .toInstance( migrationDataProvider );
            }
        } );
    }
}
