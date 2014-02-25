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
package org.apache.usergrid.test;


import com.google.inject.Inject;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.cassandra.CassandraRule;
import org.apache.usergrid.persistence.utils.JsonUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractCoreIT {
    private static final Logger LOG = LoggerFactory.getLogger( AbstractCoreIT.class );

    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @ClassRule
    public static CoreITSetup setup = new CoreITSetupImpl();

    @Inject
    public EntityCollectionManagerFactory factory;

    @Rule
    public CoreApplication app = new CoreApplication( setup );


    public void dump( Object obj ) {
        dump( "Object", obj );
    }


    public void dump( String name, Object obj ) {
        if ( obj != null && LOG.isInfoEnabled() ) {
            LOG.info( name + ":\n" + JsonUtils.mapToFormattedJsonString( obj ) );
        }
    }
}
