package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.model.entity.Entity;

import com.google.common.base.Optional;

import rx.util.functions.Func1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** @author tnine */
@RunWith( Theories.class )
public abstract class AbstractMvccEntityStageTest {


    /**
     * Tests all possible combinations that will result in a NullPointerException input fail the MvccEntity interface to be a
     * mockito mock impl
     */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNonNullable(@InvalidMvccEntityGenerator.NullFields final MvccEntity mvccEntity, @InvalidEntityGenerator.NullFields final Entity nullValidationFailEntity ) throws Exception {
        testStage( mvccEntity, nullValidationFailEntity );
    }


    /**
     * Tests all possible combinations that will result in an invalid input Excepts the MvccEntity interface to be a
     * mockito mock impl
     */
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testInvalidValue(@InvalidMvccEntityGenerator.IllegalFields  final MvccEntity mvccEntity,  @InvalidEntityGenerator.IllegalFields  final Entity invalidValueEntity ) throws Exception {

        testStage( mvccEntity, invalidValueEntity );
    }

    public void testStage( final MvccEntity mvccEntity, final Entity invalidValueEntity ) throws Exception {


           final EntityCollection context = mock( EntityCollection.class );

           when( mvccEntity.getEntity() ).thenReturn( Optional.of( invalidValueEntity ) );


           //run the stage
           Func1<IoEvent<MvccEntity>, ?> newStage = getInstance();

           newStage.call( new IoEvent<MvccEntity>( context, mvccEntity ) );
       }


    /** Get an instance of the Func1 That takes an IoEvent with an entity type for validation testing */
    protected abstract Func1<IoEvent<MvccEntity>, ?> getInstance();
}
