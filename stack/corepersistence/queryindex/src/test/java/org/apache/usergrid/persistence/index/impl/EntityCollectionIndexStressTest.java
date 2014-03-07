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
package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Inject;
import java.io.IOException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(JukitoRunner.class)
@UseModules(TestIndexModule.class)
public class EntityCollectionIndexStressTest {
    private static final Logger log = LoggerFactory.getLogger( EntityCollectionIndexStressTest.class );
        
    @Inject
    public EntityCollectionIndexFactory collectionIndexFactory;    

    @Test
    public void indexThousands() throws IOException {

        Id appId = new SimpleId("application");
        Id orgId = new SimpleId("organization");
        CollectionScope scope = new CollectionScopeImpl( appId, orgId, "contacts" );
        EntityCollectionIndex index = collectionIndexFactory.createCollectionIndex( scope );

        int limit = 10000;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( int i = 0; i < limit; i++ ) { 

            Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), scope.getName()));
            EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );
            entity.setField( new StringField( "name", RandomStringUtils.randomAlphabetic(20)) );

            index.index( entity );

            if ( i % 1000 == 0 ) {
                log.info("   Indexed: " + i);
            }
        }
        timer.stop();
        log.info( "Total time to index {} entries {}ms, average {}ms/entry", 
            limit, timer.getTime(), timer.getTime() / limit );
    }
}
