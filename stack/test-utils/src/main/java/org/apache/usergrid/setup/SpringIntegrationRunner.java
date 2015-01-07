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

package org.apache.usergrid.setup;


import org.junit.ClassRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import org.apache.usergrid.cassandra.CassandraResource;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.persistence.index.impl.ElasticSearchResource;


/**
 * Initializes the casandra and configuration before starting spring
 */
public class SpringIntegrationRunner extends BlockJUnit4ClassRunner {



    private static boolean initialized;




    /**
     *
     */
    public SpringIntegrationRunner( final Class<?> clazz ) throws InitializationError {
        super( clazz );
    }


    @Override
    protected Statement withBeforeClasses( final Statement statement ) {

        if(!initialized){
            runSetup();
        }


        final Statement toReturn =  super.withBeforeClasses( statement );

        return toReturn;

    }


    /**
     * Run the setup once per JVM
     */
    public static synchronized void runSetup() {

        if(initialized){
            return;
        }

        try {
            new SystemSetup().maybeInitialize();
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Unable to initialize the system", e );
        }


        initialized = true;


    }
}
