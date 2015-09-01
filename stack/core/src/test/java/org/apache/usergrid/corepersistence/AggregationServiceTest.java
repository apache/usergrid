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
package org.apache.usergrid.corepersistence;

import com.google.inject.Injector;
import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.cassandra.SpringResource;
import org.apache.usergrid.corepersistence.service.AggregationService;
import org.apache.usergrid.corepersistence.service.AggregationServiceFactory;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Classy class class.
 */
public class AggregationServiceTest extends AbstractCoreIT {
    @Test
    public void testEntitySize() throws Exception {
        ApplicationScope applicationScope = CpNamingUtils.getApplicationScope(this.app.getId());
        Injector injector = SpringResource.getInstance().getBean(Injector.class);
        AggregationServiceFactory factory = injector.getInstance(AggregationServiceFactory.class);
        AggregationService aggregationService = factory.getAggregationService();
        Map<String,Object> props = new HashMap<>();
         props.put("test", 1234);
        props.put("name", "myname");
        Entity entity1 = this.app.getEntityManager().create("test", props);
        Entity entity2 = this.app.getEntityManager().create("test2", props);
        this.app.refreshIndex();
        Thread.sleep(500);

        long sum = aggregationService.getApplicationSize(applicationScope);

        Assert.assertTrue( sum >= 0 );
        Assert.assertTrue(sum > (entity1.getSize() + entity2.getSize()));

        long sum1 = aggregationService.getSize(applicationScope, CpNamingUtils.createCollectionSearchEdge(applicationScope.getApplication(), "tests"));
        Assert.assertEquals(sum1, entity1.getSize());

        long sum2 = aggregationService.getSize(applicationScope, CpNamingUtils.createCollectionSearchEdge(applicationScope.getApplication(), "test2s"));
        Assert.assertEquals(sum2, entity2.getSize());

        props = new HashMap<>();
        props.put("test", 1234);
        props.put("name", "myname2");
        Entity entity3 = this.app.getEntityManager().create("test", props);

        this.app.refreshIndex();
        long sum3 = aggregationService.getSize(applicationScope, CpNamingUtils.createCollectionSearchEdge(applicationScope.getApplication(), "tests"));
        Assert.assertEquals(sum3, entity1.getSize() + entity3.getSize());

        Map<String,Long> sumEach = aggregationService.getEachCollectionSize(applicationScope);
        Assert.assertTrue(sumEach.containsKey("tests") && sumEach.containsKey("test2s"));
        Assert.assertEquals(sum3, (long) sumEach.get("tests"));

    }
}
