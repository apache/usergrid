package org.apache.usergrid.persistence.collection.mvcc.stage.write;


import org.junit.Test;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.mvcc.stage.AbstractMvccEntityStageTest;
import org.apache.usergrid.persistence.collection.mvcc.stage.IoEvent;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.model.entity.Entity;

import rx.Observable;
import rx.util.functions.Func1;

import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.fromEntity;
import static org.apache.usergrid.persistence.collection.mvcc.stage.TestEntityGenerator.generateEntity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;


/** @author tnine */

public class WriteVerifyTest extends AbstractMvccEntityStageTest {

    /** Standard flow */
    @Test
    public void testStartStage() throws Exception {


        final EntityCollection entityCollection = mock( EntityCollection.class );




        //set up the mock to return the entity from the start phase
        final Entity entity = generateEntity();

        final MvccEntity mvccEntity = fromEntity( entity );




        //run the stage
        WriteVerify newStage = new WriteVerify(  );


        Observable<IoEvent<MvccEntity>> observable = newStage.call( new IoEvent<MvccEntity>( entityCollection, mvccEntity ) );

        //verify the observable is correct
        IoEvent<MvccEntity> result = observable.toBlockingObservable().single();



        assertSame("Context was correct", entityCollection, result.getEntityCollection()) ;

        //verify the log entry is correct
        MvccEntity entry = result.getEvent();

        assertEquals( "version did not not match entityId", mvccEntity.getVersion(), entry.getVersion() );
        assertEquals( "Entity is correct", entity, entry.getEntity().orNull() );


    }




    @Override
    protected Func1<IoEvent<MvccEntity>, ?> getInstance() {
        return new WriteVerify( );
    }
}


