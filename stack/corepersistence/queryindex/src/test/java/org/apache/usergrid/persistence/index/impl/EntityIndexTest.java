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
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.collection.guice.MvccEntityDelete;
import org.apache.usergrid.persistence.core.cassandra.CassandraRule;
import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.consistency.AsyncProcessor;
import org.apache.usergrid.persistence.core.consistency.QueueListenerHelper;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.core.scope.OrganizationScopeImpl;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.persistence.index.guice.TestIndexModule;
import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.persistence.index.query.Query;
import org.apache.usergrid.persistence.index.query.Results;
import org.apache.usergrid.persistence.index.utils.EntityMapUtils;
import org.jukito.UseModules;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(ITRunner.class)
@UseModules({ TestIndexModule.class })
public class EntityIndexTest extends BaseIT {

    private static final Logger log = LoggerFactory.getLogger(EntityIndexTest.class);

    @ClassRule
    public static CassandraRule cass = new CassandraRule();

    @Rule
    public ElasticSearchRule elasticSearchRule = new ElasticSearchRule();

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    public EntityIndexFactory cif;

    @Inject
    public EntityCollectionManagerFactory cmf;

    @Inject
    @MvccEntityDelete
    public AsyncProcessor entityDelete ;

    @Test
    public void testIndex() throws IOException {

        final int MAX_ENTITIES = 100;

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl(orgId);
        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl(orgId, appId, "test-app");
        CollectionScope scope = new CollectionScopeImpl(appId, orgId, "contacts");

        EntityIndex entityIndex = cif.createEntityIndex(orgScope, appScope);
        EntityCollectionManager entityManager = cmf.createCollectionManager(scope);

        InputStream is = this.getClass().getResourceAsStream("/sample-large.json");
        ObjectMapper mapper = new ObjectMapper();
        List<Object> sampleJson = mapper.readValue(is, new TypeReference<List<Object>>() {
        });

        int count = 0;
        StopWatch timer = new StopWatch();
        timer.start();
        for (Object o : sampleJson) {

            Map<String, Object> item = (Map<String, Object>) o;

            Entity entity = new Entity(new SimpleId(UUIDGenerator.newTimeUUID(), scope.getName()));
            entity = EntityMapUtils.fromMap(entity, item);
            EntityUtils.setVersion(entity, UUIDGenerator.newTimeUUID());

            entity = entityManager.write(entity).toBlockingObservable().last();

            entityIndex.index(scope, entity);

            if (count++ > MAX_ENTITIES) {
                break;
            }
        }
        timer.stop();
        log.info("Total time to index {} entries {}ms, average {}ms/entry",
                new Object[]{count, timer.getTime(), timer.getTime() / count});

        entityIndex.refresh();

        testQueries(entityIndex, scope);
    }


    @Test
    public void testDeindex() {

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl(orgId);
        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl(orgId, appId, "test-app");
        CollectionScope scope = new CollectionScopeImpl(appId, orgId, "fastcars");

        EntityIndex entityIndex = cif.createEntityIndex(orgScope, appScope);
        EntityCollectionManager entityManager = cmf.createCollectionManager(scope);

        Map entityMap = new HashMap() {{
            put("name", "Ferrari 212 Inter");
            put("introduced", 1952);
            put("topspeed", 215);
        }};

        Entity entity = EntityMapUtils.fromMap(entityMap);
        EntityUtils.setId(entity, new SimpleId("fastcar"));
        entity = entityManager.write(entity).toBlockingObservable().last();
        entityIndex.index(scope, entity);

        entityIndex.refresh();

        Results results = entityIndex.search(scope, Query.fromQL("name contains 'Ferrari*'"));
        assertEquals(1, results.size());

        entityManager.delete(entity.getId());
        entityIndex.deindex(scope, entity);

        entityIndex.refresh();

        results = entityIndex.search(scope, Query.fromQL("name contains 'Ferrari*'"));
        assertEquals(0, results.size());
    }


    private void testQuery(EntityIndex entityIndex, CollectionScope scope, String queryString, int num) {

        StopWatch timer = new StopWatch();
        timer.start();
        Query query = Query.fromQL(queryString);
        query.setLimit(1000);
        Results results = entityIndex.search(scope, query);
        timer.stop();

        if (num == 1) {
            assertNotNull(results.getEntity() != null);
        } else {
            assertEquals(num, results.size());
        }
        log.debug("Query time {}ms", timer.getTime());
    }


    private void testQueries(EntityIndex entityIndex, CollectionScope scope) {

        testQuery(entityIndex, scope, "name = 'Morgan Pierce'", 1);

        testQuery(entityIndex, scope, "name = 'morgan pierce'", 1);

        testQuery(entityIndex, scope, "name = 'Morgan'", 0);

        testQuery(entityIndex, scope, "name contains 'Morgan'", 1);

        testQuery(entityIndex, scope, "company > 'GeoLogix'", 64);

        testQuery(entityIndex, scope, "gender = 'female'", 45);

        testQuery(entityIndex, scope, "name = 'Minerva Harrell' and age > 39", 1);

        testQuery(entityIndex, scope, "name = 'Minerva Harrell' and age > 39 and age < 41", 1);

        testQuery(entityIndex, scope, "name = 'Minerva Harrell' and age > 40", 0);

        testQuery(entityIndex, scope, "name = 'Minerva Harrell' and age >= 40", 1);

        testQuery(entityIndex, scope, "name = 'Minerva Harrell' and age <= 40", 1);
    }


    @Test
    public void testEntityToMap() throws IOException {

        InputStream is = this.getClass().getResourceAsStream("/sample-small.json");
        ObjectMapper mapper = new ObjectMapper();
        List<Object> contacts = mapper.readValue(is, new TypeReference<List<Object>>() {
        });

        for (Object o : contacts) {

            Map<String, Object> map1 = (Map<String, Object>) o;

            // convert map to entity
            Entity entity1 = EntityMapUtils.fromMap(map1);

            // convert entity back to map
            Map map2 = EntityMapUtils.toMap(entity1);

            // the two maps should be the same except for six new system properties
            Map diff = Maps.difference(map1, map2).entriesDiffering();
            assertEquals(6, diff.size());
        }
    }

    @Test
    public void getEntityVersions() throws Exception {

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl(orgId);
        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl(orgId, appId, "test-app");
        CollectionScope scope = new CollectionScopeImpl(appId, orgId, "user");

        EntityIndex entityIndex = cif.createEntityIndex(orgScope, appScope);
        EntityCollectionManager entityManager = cmf.createCollectionManager(scope);

        final String middleName = "middleName" + UUIDUtils.newTimeUUID();
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("username", "edanuff");
        properties.put("email", "ed@anuff.com");
        properties.put("middlename", middleName);

        Map entityMap = new HashMap() {{
            put("username", "edanuff");
            put("email", "ed@anuff.com");
            put("middlename", middleName);
        }};

        Entity user = EntityMapUtils.fromMap(entityMap);
        EntityUtils.setId(user, new SimpleId("edanuff"));
        user = entityManager.write(user).toBlockingObservable().last();
        entityIndex.index(scope, user);

        user.setField(new StringField("address1", "1782 address st"));
        user = entityManager.write(user).toBlockingObservable().last();
        entityIndex.index(scope, user);
        user.setField(new StringField("address2", "apt 508"));
        user = entityManager.write(user).toBlockingObservable().last();
        entityIndex.index(scope, user);
        user.setField(new StringField("address3", "apt 508"));
        user = entityManager.write(user).toBlockingObservable().last();
        entityIndex.index(scope, user);
        entityIndex.refresh();

        Results results = entityIndex.getEntityVersions(user.getId(), scope);
        List<Entity> entities = results.getEntities(true);
        assertEquals(entities.size(), 4);
        assertEquals(entities.get(0).getId(), user.getId());
        assertEquals(entities.get(3).getVersion(), user.getVersion());
    }

    @Test
    public void deleteVerification() throws Exception {

        Id orgId = new SimpleId("organization");
        OrganizationScope orgScope = new OrganizationScopeImpl(orgId);
        Id appId = new SimpleId("application");
        CollectionScope appScope = new CollectionScopeImpl(orgId, appId, "test-app");
        CollectionScope scope = new CollectionScopeImpl(appId, orgId, "user");

        EntityIndex ei = cif.createEntityIndex(orgScope, appScope);
        EntityCollectionManager em = cmf.createCollectionManager(scope);

        QueueListenerHelper helper = new QueueListenerHelper(entityDelete,2,0);

        final String middleName = "middleName" + UUIDUtils.newTimeUUID();
        Map entityMap = new HashMap() {{
            put("username", "edanuff");
            put("email", "ed@anuff.com");
            put("middlename", middleName);
        }};

        Entity user = EntityMapUtils.fromMap(entityMap);
        EntityUtils.setId(user, new SimpleId("edanuff"));
        user = em.write(user).toBlockingObservable().last();
        ei.index(scope, user);
        user.setField(new StringField("address1","1782 address st"));
        user = em.write(user).toBlockingObservable().last();
        ei.index(scope, user);
        user.setField(new StringField("address2", "apt 508"));
        user = em.write(user).toBlockingObservable().last();
        ei.index(scope, user);
        user.setField(new StringField("address3","apt 508"));
        user = em.write(user).toBlockingObservable().last();
        ei.index(scope, user);
        ei.refresh();

        user = em.load(user.getId()).toBlockingObservable().last();
        em.delete(user.getId()).toBlockingObservable().last();
        try {
            helper.awaitWithoutErrors(10, TimeUnit.SECONDS);
        }catch(Throwable e){
            throw new Exception(e);
        }
        // EntityRef
        Query query = new Query();
        query.addEqualityFilter( "username", "edanuff" );
        Results r = ei.search( scope , query );

        assertTrue(r.size() == 0);
    }
}



