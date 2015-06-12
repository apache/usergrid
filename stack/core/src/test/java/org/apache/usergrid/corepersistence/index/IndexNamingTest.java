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
package org.apache.usergrid.corepersistence.index;

import com.google.inject.Inject;
import net.jcip.annotations.NotThreadSafe;
import org.apache.usergrid.corepersistence.TestIndexModule;
import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.index.impl.EsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Classy class class.
 */
@RunWith( EsRunner.class )
@UseModules( { TestIndexModule.class } )
@NotThreadSafe
public class IndexNamingTest {
    @Inject
    public CassandraFig cassandraFig;

    @Inject
    public IndexFig indexFig;


    @Inject
    public CoreIndexFig indexProcessorFig;

    @Inject
    public IndexLocationStrategyFactory indexLocationStrategyFactory;

    @Inject
    public ApplicationIndexBucketLocator bucketLocator;

    private ApplicationScope applicationScope;
    private ApplicationScope managementApplicationScope;
    private ApplicationIndexLocationStrategy applicationLocationStrategy;
    private ManagementIndexLocationStrategy managementLocationStrategy;

    @Before
    public void setup(){
        this.applicationScope = CpNamingUtils.getApplicationScope(UUID.randomUUID());
        this.managementApplicationScope = CpNamingUtils.getApplicationScope(CpNamingUtils.getManagementApplicationId().getUuid());
        this.managementLocationStrategy = new ManagementIndexLocationStrategy(indexFig, indexProcessorFig);
        this.applicationLocationStrategy = new ApplicationIndexLocationStrategy(cassandraFig,indexFig,applicationScope,bucketLocator);
    }

    @Test
    public void managementNaming(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(managementApplicationScope);
        assertEquals(indexLocationStrategy.getIndexRootName(),managementLocationStrategy.getIndexRootName());
        assertEquals(indexLocationStrategy.getIndexRootName(),indexProcessorFig.getManagementAppIndexName());
        assertEquals(indexLocationStrategy.getIndexRootName(), indexLocationStrategy.getIndexBucketName());
        assertEquals(indexLocationStrategy.getIndexBucketName(),indexProcessorFig.getManagementAppIndexName());

    }
    @Test
    public void applicationNaming(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);
        assertEquals(indexLocationStrategy.getIndexRootName(),applicationLocationStrategy.getIndexRootName());

        assertTrue(indexLocationStrategy.getIndexRootName().contains(cassandraFig.getApplicationKeyspace().toLowerCase()));
        assertTrue(indexLocationStrategy.getAlias().getReadAlias().contains(applicationScope.getApplication().getUuid().toString().toLowerCase()));
        assertTrue(indexLocationStrategy.getAlias().getWriteAlias().contains(applicationScope.getApplication().getUuid().toString().toLowerCase()));
        assertTrue(indexLocationStrategy.getAlias().getWriteAlias().contains("write"));
        assertTrue(indexLocationStrategy.getAlias().getReadAlias().contains("read"));
        Set<String> names = new HashSet<>();
        for(int i=0;i<10;i++){
            IndexLocationStrategy indexLocationStrategyBucket = new ApplicationIndexLocationStrategy(cassandraFig,indexFig,applicationScope, new ApplicationIndexBucketLocator(indexProcessorFig));
            names.add(indexLocationStrategyBucket.getIndexBucketName());
        }
        String expectedName = cassandraFig.getApplicationKeyspace().toLowerCase()+"_\\d+";
        Pattern regex = Pattern.compile(expectedName);
        //always hashes to same bucket
        assertTrue(names.size() == 1);
         names = new HashSet<>();
        for(int i=0;i<100;i++){
            IndexLocationStrategy indexLocationStrategyBucket =
                new ApplicationIndexLocationStrategy(
                    cassandraFig,
                    indexFig,
                    new ApplicationScopeImpl(CpNamingUtils.generateApplicationId(UUID.randomUUID())),
                    new ApplicationIndexBucketLocator(indexProcessorFig));
            String name = indexLocationStrategyBucket.getIndexBucketName();
            assertTrue("failed to match correct name",regex.matcher(name).matches());
            names.add(name);
        }
        //always hashes to diff't bucket
        assertTrue(names.size()==indexProcessorFig.getNumberOfIndexBuckets());

    }

}
