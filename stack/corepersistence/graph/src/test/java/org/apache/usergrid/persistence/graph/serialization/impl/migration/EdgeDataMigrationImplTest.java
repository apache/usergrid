/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.graph.serialization.impl.migration;


import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.core.guice.DataMigrationResetRule;
import org.apache.usergrid.persistence.core.migration.data.DataMigrationManager;
import org.apache.usergrid.persistence.core.migration.data.TestProgressObserver;
import org.apache.usergrid.persistence.core.migration.data.MigrationDataProvider;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.core.util.IdGenerator;
import org.apache.usergrid.persistence.graph.Edge;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.graph.serialization.impl.GraphDataVersions;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.inject.Inject;

import net.jcip.annotations.NotThreadSafe;

import rx.Observable;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.apache.usergrid.persistence.core.util.IdGenerator.createId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


@NotThreadSafe
@RunWith(ITRunner.class)
@UseModules({ TestGraphModule.class })
public class EdgeDataMigrationImplTest implements DataMigrationResetRule.DataMigrationManagerProvider {


    @Inject
    public DataMigrationManager dataMigrationManager;

    @Inject
    public GraphManagerFactory graphManagerFactory;

    @Inject
    public EdgeDataMigrationImpl edgeDataMigrationImpl;

    /**
     * Rule to do the resets we need
     */
    @Rule
    public DataMigrationResetRule migrationTestRule = new DataMigrationResetRule(this, GraphMigrationPlugin.PLUGIN_NAME,  GraphDataVersions.INITIAL.getVersion());




    @Test
    public void testIdMapping() throws Throwable {

        assertEquals( "version 0 expected", GraphDataVersions.INITIAL.getVersion(),
            dataMigrationManager.getCurrentVersion( GraphMigrationPlugin.PLUGIN_NAME ) );


        final ApplicationScope applicationScope = new ApplicationScopeImpl( IdGenerator.createId( "application" ) );


        GraphManager gm = graphManagerFactory.createEdgeManager(applicationScope );


        final Id sourceId1 = IdGenerator.createId( "source1" );

        final Id sourceId2 = IdGenerator.createId( "source2" );


        final Id target1 = IdGenerator.createId( "target1" );

        final Id target2 = IdGenerator.createId( "target2" );


        Edge s1t1 = createEdge(sourceId1, "test", target1 );

        Edge s1t2 = createEdge( sourceId1, "baz", target2 );

        Edge s2t1 = createEdge(sourceId2, "foo", target1);

        Edge s2t2 = createEdge( sourceId2, "bar", target2 );




        gm.writeEdge( s1t1 ).toBlocking().last();
        gm.writeEdge( s1t2 ).toBlocking().last();
        gm.writeEdge( s2t1 ).toBlocking().last();
        gm.writeEdge( s2t2 ).toBlocking().last();


        //walk from s1 and s2
        final Observable<GraphNode> graphNodes = Observable.just( new GraphNode( applicationScope, sourceId1), new GraphNode(applicationScope, sourceId2 ) );

        final MigrationDataProvider<GraphNode> testMigrationProvider = new MigrationDataProvider<GraphNode>() {
            @Override
            public Observable<GraphNode> getData() {
                return graphNodes;
            }
        };


        final TestProgressObserver progressObserver = new TestProgressObserver();



        //read everything in previous version format and put it into our types.


        final int returned = edgeDataMigrationImpl.migrate( GraphDataVersions.INITIAL.getVersion() , testMigrationProvider, progressObserver );
        //perform the migration

        assertEquals( "Correct version returned", returned, GraphDataVersions.META_SHARDING.getVersion() );
        assertFalse( "Progress observer should not have failed", progressObserver.isFailed() );
        assertTrue( "Progress observer should have update messages", progressObserver.getUpdates().size() > 0 );


        //now check we can still read our data.

        List<String> source1Edges =
            gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( sourceId1, null, null ) ).toList().toBlocking().last();

        //now check both edge types are present

        assertTrue( "Edge type present", source1Edges.contains( "test" ) );
        assertTrue( "Edge type present", source1Edges.contains( "baz" ) );


        List<String> source2Edges =
            gm.getEdgeTypesFromSource( new SimpleSearchEdgeType( sourceId2, null, null ) ).toList().toBlocking().last();

        //now check both edge types are present

        assertTrue( "Edge type present", source2Edges.contains( "foo" ) );
        assertTrue( "Edge type present", source2Edges.contains( "bar" ) );



    }


    @Override
    public DataMigrationManager getDataMigrationManager() {
        return dataMigrationManager;
    }
}
