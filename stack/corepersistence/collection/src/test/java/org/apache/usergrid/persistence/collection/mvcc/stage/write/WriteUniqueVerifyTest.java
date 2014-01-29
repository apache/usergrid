package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractMvccEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.CollectionIoEvent;
import org.apache.usergrid.persistence.model.entity.Entity;

import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.fromEntity;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.generateEntity;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;


/** @author tnine */

public class WriteUniqueVerifyTest extends AbstractMvccEntityStageTest {

    /** Standard flow */
    @Test
    public void testStartStage() throws Exception {


        final CollectionScope collectionScope = mock( CollectionScope.class );




        //set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();

        final MvccEntity mvccEntity = fromEntity( entity );




        //run the stage
        WriteUniqueVerify newStage = new WriteUniqueVerify(  );


        CollectionIoEvent<MvccEntity>
                result = newStage.call( new CollectionIoEvent<MvccEntity>( collectionScope, mvccEntity ) );




        assertSame("Context was correct", collectionScope, result.getEntityCollection()) ;

        //verify the log entry is correct
        MvccEntity entry = result.getEvent();

         //verify uuid and version in both the MvccEntity and the entity itself
        //assertSame is used on purpose.  We want to make sure the same instance is used, not a copy.
        //this way the caller's runtime type is retained.
        assertSame( "id correct", entity.getId(), entry.getId() );
        assertSame( "version did not not match entityId", entity.getVersion(), entry.getVersion() );
        assertSame( "Entity correct", entity, entry.getEntity().get() );

    }


    @Override
    protected void validateStage( final CollectionIoEvent<MvccEntity> event ) {
        new WriteUniqueVerify( ).call( event );
    }

}


