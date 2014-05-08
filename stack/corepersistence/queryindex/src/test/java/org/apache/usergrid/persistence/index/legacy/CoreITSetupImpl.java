/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.index.legacy;


import com.netflix.config.ConfigurationManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.index.utils.JsonUtils;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CoreITSetupImpl implements CoreITSetup {
    private static final Logger LOG = LoggerFactory.getLogger( CoreITSetupImpl.class );

    protected EntityCollectionManagerFactory emf;
    protected boolean enabled = false;


//    public CoreITSetupImpl( CassandraResource cassandraResource ) {
//        this.cassandraResource = cassandraResource;
//    }


    @Override
    public Statement apply( Statement base, Description description ) {
        return statement( base, description );
    }


    private Statement statement( final Statement base, final Description description ) {
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


    /**
     * Sets up the resources for the test here.
     *
     * @throws Throwable if setup fails (which will disable {@code after}
     */
    protected void before( Description description ) throws Throwable {
        LOG.info( "Setting up for {}", description.getDisplayName() );
        initialize();
    }


    private void initialize() {
    }


    /** Override to tear down your specific external resource. */
    protected void after( Description description ) {
        LOG.info( "Tearing down for {}", description.getDisplayName() );
    }


    @Override
    public void dump( String name, Object obj ) {
        if ( obj != null && LOG.isInfoEnabled() ) {
            LOG.info( name + ":\n" + JsonUtils.mapToFormattedJsonString( obj ) );
        }
    }

    public EntityCollectionManagerFactory getEmf() {
        return emf;
    }
}
