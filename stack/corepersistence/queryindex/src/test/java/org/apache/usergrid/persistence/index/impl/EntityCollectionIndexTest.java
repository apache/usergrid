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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.index.EntityCollectionIndex;
import org.apache.usergrid.persistence.index.EntityCollectionIndexFactory;
import org.apache.usergrid.persistence.index.guice.IndexTestModule;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.query.Query;
import org.apache.usergrid.persistence.query.Results;
import org.apache.usergrid.test.EntityMapUtils;
import org.elasticsearch.client.Client;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(JukitoRunner.class)
@UseModules({ IndexTestModule.class })
public class EntityCollectionIndexTest {

    private static final Logger logger = LoggerFactory.getLogger( EntityCollectionIndexTest.class );
        
    @Inject
    public EntityCollectionIndexFactory collectionIndexFactory;

    @Test
    public void testIndex() throws IOException {

        final CollectionScope scope = mock( CollectionScope.class );
        when( scope.getName() ).thenReturn( "contacts" );
        String index = RandomStringUtils.randomAlphanumeric(20 ).toLowerCase();
        String type = scope.getName();

        EntityCollectionIndex entityIndex = collectionIndexFactory.createCollectionIndex( scope );

        InputStream is = this.getClass().getResourceAsStream( "/sample-large.json" );
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue( is, new TypeReference<List<Object>>() {} );

        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();
        for ( Object o : sampleJson ) {

            Map<String, Object> item = (Map<String, Object>)o;

            Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), scope.getName()));
            entity = EntityMapUtils.mapToEntity( scope.getName(), entity, item );
            EntityUtils.setVersion( entity, UUIDGenerator.newTimeUUID() );

            entityIndex.index( entity );

            count++;
        }
        timer.stop();
        logger.info( "Total time to index {} entries {}ms, average {}ms/entry", 
            count, timer.getTime(), timer.getTime() / count );

        testQueries( entityIndex );
    }
   
   
    private void testQuery( EntityCollectionIndex entityIndex, String queryString, int num ) {

        StopWatch timer = new StopWatch();
        timer.start();
        Query query = Query.fromQL( queryString );
        query.setLimit( 1000 );
        Results results = entityIndex.execute( query );
        timer.stop();

        assertEquals( num, results.getRefs().size() );
        logger.debug( "Query time {}ms", timer.getTime() );
    }


    private void testQueries( EntityCollectionIndex entityIndex ) {

        testQuery( entityIndex, "name = 'Morgan Pierce'", 1);

        testQuery( entityIndex, "name = 'morgan pierce'", 1);

        testQuery( entityIndex, "name = 'Morgan'", 0);

        testQuery( entityIndex, "name_ug_analyzed = 'Morgan'", 1);

        testQuery( entityIndex, "gender = 'female'", 433);

        testQuery( entityIndex, "name = 'Minerva Harrell' and age > 39", 1);

        testQuery( entityIndex, "name = 'Minerva Harrell' and age > 39 and age < 41", 1);

        testQuery( entityIndex, "name = 'Minerva Harrell' and age > 40", 0);

        testQuery( entityIndex, "name = 'Minerva Harrell' and age >= 40", 1);

        testQuery( entityIndex, "name = 'Minerva Harrell' and age <= 40", 1);
    }

    
    @Test // TODO 
    @Ignore
    public void testRemoveIndex() {
        fail("Not implemented");
    }
}
