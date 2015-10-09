/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence;

import com.google.common.base.Optional;
import com.google.inject.Injector;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.service.AggregationService;
import org.apache.usergrid.corepersistence.service.AggregationServiceFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.SearchByEdgeType;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdge;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchByEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * testy test
 */
public class ApplicationServiceIT extends AbstractCoreIT {

    private static final Logger LOG = LoggerFactory.getLogger( ApplicationServiceIT.class );

    public ApplicationServiceIT() {
        super();
    }

    @Test
    public void testDeletes() throws Exception{
        EntityManager entityManager = this.app.getEntityManager();
        Map<String,Object> map = new HashMap<>();
        for(int i =0;i<10;i++) {
            map.put("somekey", UUID.randomUUID());
           Entity entity = entityManager.create("tests", map);
        }
        this.app.refreshIndex();
        Thread.sleep(500);
        ApplicationScope appScope  = CpNamingUtils.getApplicationScope(entityManager.getApplicationId());
        Observable<Id> ids =
            this.app.getApplicationService().deleteAllEntities(appScope, 5);
        int count = ids.count().toBlocking().last();
        Assert.assertEquals(count, 5);
        ids =
            this.app.getApplicationService().deleteAllEntities(appScope, 5);
        count = ids.count().toBlocking().last();
        Assert.assertEquals(count, 5);
        this.app.refreshIndex();
        Injector injector = SpringResource.getInstance().getBean(Injector.class);
        GraphManagerFactory factory = injector.getInstance(GraphManagerFactory.class);
        GraphManager graphManager = factory.createEdgeManager(appScope);
        SimpleSearchByEdgeType simpleSearchByEdgeType = new SimpleSearchByEdgeType(
            appScope.getApplication(),
            CpNamingUtils.getEdgeTypeFromCollectionName("tests")
            , Long.MAX_VALUE, SearchByEdgeType.Order.DESCENDING,
            Optional.<Edge>absent() );

        Iterator<Edge> results = graphManager.loadEdgesFromSource(simpleSearchByEdgeType).toBlocking().getIterator();
        if(results.hasNext()){
            int i = 0;

            while(results.hasNext()){
                results.next();
                i++;
            }
            Assert.fail("should be empty but has "+i);

        }else{
            Results searchCollection = entityManager.searchCollection(entityManager.getApplication(), "tests", Query.all());
            Assert.assertEquals(searchCollection.size(),0);
            AggregationServiceFactory aggregationServiceFactory = injector.getInstance(AggregationServiceFactory.class);
            long size = aggregationServiceFactory.getAggregationService().getCollectionSize(appScope,"tests");
            Assert.assertEquals(size,0);
            //success
        }
    }

}
