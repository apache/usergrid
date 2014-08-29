
package org.apache.usergrid.persistence.graph;


import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.graph.guice.TestGraphModule;
import org.apache.usergrid.persistence.graph.impl.SimpleSearchEdgeType;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;

import static org.apache.usergrid.persistence.graph.test.util.EdgeTestUtils.createEdge;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(ITRunner.class)
@UseModules({ TestGraphModule.class })
public class SimpleTest {

    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;

    @Inject
    protected GraphManagerFactory emf;

    protected ApplicationScope scope;


    @Before
    public void mockApp() {
        this.scope = mock( ApplicationScope.class );

        Id orgId = mock( Id.class );

        when( orgId.getType() ).thenReturn( "organization" );
        when( orgId.getUuid() ).thenReturn( UUIDGenerator.newTimeUUID() );

        when( this.scope.getApplication() ).thenReturn( orgId );
    }


    @Test
    public void testGetEdgesToTarget() {

        final GraphManager gm = emf.createEdgeManager( scope );

        Id sourceId1 = new SimpleId( "source1" );
        Id sourceId2 = new SimpleId( "source2" );
        Id targetId1 = new SimpleId( "target" );


        Edge testTargetEdge = createEdge( sourceId1, "test", targetId1, System.currentTimeMillis() );
        gm.writeEdge( testTargetEdge ).toBlockingObservable().singleOrDefault( null );

        Edge testTarget2Edge = createEdge( sourceId2, "edgeType1", targetId1, System.currentTimeMillis() );
        gm.writeEdge( testTarget2Edge ).toBlockingObservable().singleOrDefault( null );

        Edge test2TargetEdge = createEdge( sourceId1, "edgeType1", targetId1, System.currentTimeMillis() );
        gm.writeEdge( test2TargetEdge ).toBlockingObservable().singleOrDefault( null );

        Edge test3TargetEdge = createEdge( sourceId1, "edgeType2", targetId1, System.currentTimeMillis() );
        gm.writeEdge( test3TargetEdge ).toBlockingObservable().singleOrDefault( null );

        int count = gm.getEdgeTypesToTarget( new SimpleSearchEdgeType(targetId1, null, null) )
                .count().toBlockingObservable().last();
        assertEquals( 3, count );

        count = gm.getEdgeTypesToTarget( new SimpleSearchEdgeType(targetId1, "edgeType", null) )
                .count().toBlockingObservable().last();
        assertEquals( 2, count );
    }

}
