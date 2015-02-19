package org.apache.usergrid.persistence.collection.serialization.impl;

import com.google.inject.Inject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.collection.mvcc.entity.impl.MvccEntityImpl;
import org.apache.usergrid.persistence.collection.serialization.SerializationFig;
import org.apache.usergrid.persistence.collection.util.EntityHelper;
import org.apache.usergrid.persistence.core.astyanax.CassandraFig;
import org.apache.usergrid.persistence.core.guicyfig.SetConfigTestBypass;
import org.apache.usergrid.persistence.core.util.ValidationUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

/**
 * Classy class class.
 */
public abstract class MvccEntitySerializationStrategyV3Test extends MvccEntitySerializationStrategyImplTest{
    @Inject
    protected SerializationFig serializationFig;

    @Inject
    protected CassandraFig cassandraFig;

    private int setMaxEntitySize;

    @Before
    public void setUp() {
        setMaxEntitySize = serializationFig.getMaxEntitySize();
    }


    @After
    public void tearDown() {
        SetConfigTestBypass.setValueByPass(serializationFig, "getMaxEntitySize", setMaxEntitySize + "");
    }

    /**
     * Tests an entity with more than  65535 bytes worth of data is successfully stored and retrieved
     */
    @Test
    public void largeEntityWriteRead() throws ConnectionException {
        final int setSize = 65535 * 2;


        //this is the size it works out to be when serialized, we want to allow this size

        SetConfigTestBypass.setValueByPass( serializationFig, "getMaxEntitySize", 65535 * 10 + "" );
        final Entity entity = EntityHelper.generateEntity(setSize);

        //now we have one massive, entity, save it and retrieve it.
        CollectionScope context =
                new CollectionScopeImpl( new SimpleId( "organization" ), new SimpleId( "parent" ), "tests" );


        final Id id = entity.getId();
        ValidationUtils.verifyIdentity(id);
        final UUID version = UUIDGenerator.newTimeUUID();
        final MvccEntity.Status status = MvccEntity.Status.COMPLETE;

        final MvccEntity mvccEntity = new MvccEntityImpl( id, version, status, entity );


        getMvccEntitySerializationStrategy().write( context, mvccEntity ).execute();

        //now load it
        final MvccEntity loadedEntity =
                getMvccEntitySerializationStrategy().load( context, id );


        assertLargeEntity( mvccEntity, loadedEntity );


        MvccEntity returned =
                serializationStrategy.load( context, Collections.singleton(id), version ).getEntity( id );

        assertLargeEntity( mvccEntity, returned );
    }



    protected void assertLargeEntity( final MvccEntity expected, final MvccEntity returned ) {

        org.junit.Assert.assertEquals( "The loaded entity should match the stored entity", expected, returned );

        EntityHelper.verifyDeepEquals( expected.getEntity().get(), returned.getEntity().get() );
    }


}
