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
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.guicyfig.ClusterFig;
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
    public ClusterFig clusterFig;

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
    private String keyspaceName;
    private String clusterName;

    @Before
    public void setup(){
        keyspaceName = cassandraFig.getApplicationKeyspace().toLowerCase();
        clusterName = clusterFig.getClusterName().toLowerCase();
        this.applicationScope = CpNamingUtils.getApplicationScope(UUID.randomUUID());
        this.managementApplicationScope = CpNamingUtils.getApplicationScope(CpNamingUtils.getManagementApplicationId().getUuid());
        this.managementLocationStrategy = new ManagementIndexLocationStrategy(clusterFig,cassandraFig,indexFig, indexProcessorFig);
        this.applicationLocationStrategy = new ApplicationIndexLocationStrategy(clusterFig, cassandraFig,indexFig,applicationScope,bucketLocator);
    }

    @Test
    public void managementNaming(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(managementApplicationScope);
        //check that factory works
        assertEquals(indexLocationStrategy.getIndexRootName(),managementLocationStrategy.getIndexRootName());
        //check that root name is as expected
        assertEquals(indexLocationStrategy.getIndexRootName(),clusterName + "_" + keyspaceName + "_" + indexProcessorFig.getManagementAppIndexName());
        //check bucket name is as expected
        assertEquals(indexLocationStrategy.getIndexRootName(), indexLocationStrategy.getIndexInitialName());
        assertEquals(indexLocationStrategy.getIndexInitialName(),clusterName + "_" + keyspaceName + "_" +indexProcessorFig.getManagementAppIndexName());

    }

    @Test
    public void managementAliasName(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(managementApplicationScope);

        String managementAppIndexName = indexProcessorFig.getManagementAppIndexName();
        assertEquals(
            indexLocationStrategy.getAlias().getReadAlias(),
            clusterName + "_" + keyspaceName + "_" + managementAppIndexName + "_read_" + indexFig.getAliasPostfix()
        );
        assertEquals(
            indexLocationStrategy.getAlias().getWriteAlias(),
            clusterName + "_" + keyspaceName + "_" + managementAppIndexName + "_write_" + indexFig.getAliasPostfix()
        );
    }

    @Test
    public void applicationRootNaming(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);

        assertEquals(
            indexLocationStrategy.getIndexRootName(),
            applicationLocationStrategy.getIndexRootName()
        );

        assertEquals(
            indexLocationStrategy.getIndexRootName(),
            clusterName
        );

    }

    @Test
    public void applicationAliasName(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);
        String applicationId = applicationScope.getApplication().getUuid().toString().toLowerCase();
        assertEquals(
            indexLocationStrategy.getAlias().getReadAlias(),
            clusterName +"_"+ applicationId + "_read_" + indexFig.getAliasPostfix()
        );
        assertEquals(
            indexLocationStrategy.getAlias().getWriteAlias(),
            clusterName +"_"+ applicationId + "_write_" + indexFig.getAliasPostfix()
        );
    }

    @Test
    public void applicationBucketNaming(){
        Set<String> names = new HashSet<>();
        for(int i=0;i<10;i++){
            IndexLocationStrategy indexLocationStrategyBucket =
                new ApplicationIndexLocationStrategy(
                    clusterFig, cassandraFig, indexFig,applicationScope,
                    new ApplicationIndexBucketLocator(indexProcessorFig)
                );
            names.add(indexLocationStrategyBucket.getIndexInitialName());
        }
        Pattern regex = Pattern.compile(clusterName+"_applications_\\d+");
        //always hashes to same bucket
        assertTrue(names.size() == 1);
        names = new HashSet<>();
        //get 100 names you should get 5 unique values in the set since app id is the same
        for(int i=0;i<100;i++){
            IndexLocationStrategy indexLocationStrategyBucket =
                new ApplicationIndexLocationStrategy(
                    clusterFig,
                    cassandraFig,
                    indexFig,
                    new ApplicationScopeImpl(CpNamingUtils.generateApplicationId(UUID.randomUUID())),
                    new ApplicationIndexBucketLocator(indexProcessorFig));
            String name = indexLocationStrategyBucket.getIndexInitialName();
            assertTrue("failed to match correct name",regex.matcher(name).matches());
            names.add(name);
        }
        //always hashes to diff't bucket
        assertTrue(names.size() == indexProcessorFig.getNumberOfIndexBuckets());
    }

    @Test
    public void testReplication(){
        IndexLocationStrategy indexLocationStrategy = indexLocationStrategyFactory.getIndexLocationStrategy(applicationScope);
        ReplicatedIndexLocationStrategy replicatedIndexLocationStrategy = new ReplicatedIndexLocationStrategy(indexLocationStrategy);
        assertEquals(replicatedIndexLocationStrategy.getApplicationScope(),indexLocationStrategy.getApplicationScope());
        assertEquals(replicatedIndexLocationStrategy.getIndexInitialName(),indexLocationStrategy.getIndexInitialName());
        assertEquals(replicatedIndexLocationStrategy.getIndexRootName(),indexLocationStrategy.getIndexRootName());
        assertEquals(replicatedIndexLocationStrategy.getNumberOfReplicas(), indexLocationStrategy.getNumberOfReplicas());
        assertEquals(replicatedIndexLocationStrategy.getNumberOfShards(),indexLocationStrategy.getNumberOfShards());
        assertEquals(replicatedIndexLocationStrategy.getAlias().getReadAlias(),indexLocationStrategy.getAlias().getReadAlias());
        assertEquals(replicatedIndexLocationStrategy.getAlias().getWriteAlias(),indexLocationStrategy.getAlias().getWriteAlias());


    }

}
