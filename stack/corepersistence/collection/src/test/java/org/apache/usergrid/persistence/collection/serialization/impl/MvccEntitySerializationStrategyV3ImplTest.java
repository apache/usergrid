package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.TestCollectionModule;
import org.apache.usergrid.persistence.collection.serialization.MvccEntitySerializationStrategy;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.core.test.ITRunner;
import org.apache.usergrid.persistence.core.test.UseModules;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;


/**
 * Same tests as v2, we just override some methods to ensure they throw the correct exceptions
 */
@RunWith( ITRunner.class )
@UseModules( TestCollectionModule.class )
public class MvccEntitySerializationStrategyV3ImplTest extends MvccEntitySerializationStrategyV2Test {
    @Inject
    private MvccEntitySerializationStrategyV3Impl serializationStrategy;


    @Override
    protected MvccEntitySerializationStrategy getMvccEntitySerializationStrategy() {
        return serializationStrategy;
    }


    @Test( expected = UnsupportedOperationException.class )
    public void loadAscendingHistory() throws ConnectionException {
        final Id applicationId = new SimpleId( "application" );
        final String name = "test";

        ApplicationScope context = new ApplicationScopeImpl( applicationId );


        final Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), name );
        final UUID version1 = UUIDGenerator.newTimeUUID();

        serializationStrategy.loadAscendingHistory( context, entityId, version1, 20 );
    }


    @Test( expected = UnsupportedOperationException.class )
    public void loadDescendingHistory() throws ConnectionException {


        final String name = "test";

        final Id applicationId = new SimpleId( "application" );

        ApplicationScope context = new ApplicationScopeImpl( applicationId );

        final Id entityId = new SimpleId( UUIDGenerator.newTimeUUID(), name );
        final UUID version1 = UUIDGenerator.newTimeUUID();

        serializationStrategy.loadDescendingHistory( context, entityId, version1, 20 );
    }
}

