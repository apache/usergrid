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
package perftest2;

import com.google.inject.Injector
import com.netflix.config.ConfigurationManager
import org.apache.commons.lang3.RandomStringUtils
import org.apache.usergrid.persistence.collection.CollectionScope
import org.apache.usergrid.persistence.collection.EntityCollectionManager
import com.google.inject.Guice
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl
import org.apache.usergrid.persistence.core.migration.MigrationManager
import org.apache.usergrid.persistence.collection.util.EntityUtils
import org.apache.usergrid.persistence.index.EntityCollectionIndex
import org.apache.usergrid.persistence.index.guice.IndexModule
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory
import org.apache.usergrid.persistence.model.entity.Entity
import org.apache.usergrid.persistence.model.entity.Id
import org.apache.usergrid.persistence.model.entity.SimpleId
import org.apache.usergrid.persistence.model.field.LongField
import org.apache.usergrid.persistence.model.field.StringField
import org.apache.usergrid.persistence.model.util.UUIDGenerator
import org.apache.usergrid.persistence.query.Query
import org.apache.usergrid.persistence.query.Results


/** 
 * Just an example and not included in performance test.
 */
public class CreateEntity {

    public static void main( String[] args ) {

        // setup core persistence
        ConfigurationManager.loadCascadedPropertiesFromResources( "usergrid" )
        Injector injector = Guice.createInjector( new IndexModule() )

        // only on first run
        //MigrationManager m = injector.getInstance( MigrationManager.class )
        //m.migrate()

        EntityCollectionManagerFactory ecmf =
            injector.getInstance( EntityCollectionManagerFactory.class )
        EntityCollectionIndexFactory ecif =
            injector.getInstance( EntityCollectionIndexFactory.class )

        // create scope
        Id appId = new SimpleId("entitytest")
        Id orgId = new SimpleId("usergrid")
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "testentities" )

        // entity manager and index interfaces
        EntityCollectionManager ecm = ecmf.createCollectionManager( scope )
        EntityCollectionIndex eci = ecif.createCollectionIndex( scope )

        // create entity
        Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), "testentity"))
        String name = RandomStringUtils.randomAlphanumeric(10) 
        entity.setField( new StringField("name", name))
        entity.setField( new LongField("timestamp", new Date().getTime()))

        // write and index entity
        ecm.write( entity ).toBlockingObservable().last()
        eci.index( entity )

        eci.refresh()

        ecm = ecmf.createCollectionManager( scope )
        eci = ecif.createCollectionIndex( scope )

        // get back entity
        def got = ecm.load( entity.getId() ).toBlockingObservable().last()

        def returnedName = got.getField("name").getValue()
        def timestamp = got.getField("timestamp").getValue()
        println ">>>> Got back name=${returnedName} : time=${timestamp}"

        // search for entity
        def found = eci.execute( Query.fromQL( "name = '${name}'".toString() ) ).getEntities().get(0)

        returnedName = found.getField("name").getValue()
        timestamp = found.getField("timestamp").getValue()
        println ">>>> Found name=${returnedName} : time=${timestamp}"
    }
}
