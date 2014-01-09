package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.collection.util.InvalidEntityGenerator;
import org.apache.usergrid.persistence.collection.util.InvalidIdGenerator;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import com.google.common.base.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** @author tnine */
@RunWith(Theories.class)
public abstract class AbstractMvccEntityStageTest {

    /**
     * Tests all possible combinations that will result in a NullPointerException input fail the 
     * MvccEntity interface to be a mockito mock impl
     */
    @Test(expected = NullPointerException.class)
    @Theory
    public void testNonNullable( 
            @InvalidMvccEntityGenerator.NullFields final MvccEntity mvccEntity, 
            @InvalidEntityGenerator.NullFields final Entity entity,
            @InvalidIdGenerator.NullFields final Id nullValidationFailId ) throws Exception {
        testStage( mvccEntity, entity, nullValidationFailId );
    }


    /**
     * Tests all possible combinations that will result in an invalid input Excepts the MvccEntity 
     * interface to be a mockito mock impl
     */
    @Test(expected = IllegalArgumentException.class)
    @Theory
    public void testInvalidValue( 
            @InvalidMvccEntityGenerator.IllegalFields final MvccEntity mvccEntity,   
            @InvalidEntityGenerator.IllegalFields final Entity entity,
            @InvalidIdGenerator.IllegalFields final Id invalidValueId ) throws Exception {

        testStage( mvccEntity, entity, invalidValueId );
    }


    public void testStage( 
            final MvccEntity mvccEntity, final Entity entity,  final Id id ) throws Exception {

        if(entity != null){
            EntityUtils.setId( entity, id );
        }

        final CollectionScope context = mock( CollectionScope.class );

        if(mvccEntity != null){
            when(mvccEntity.getEntity() ).thenReturn( Optional.fromNullable( entity ) );
            when(mvccEntity.getId()).thenReturn( id );
        }

        validateStage( new CollectionIoEvent<MvccEntity>( context, mvccEntity ) );
    }


    /** 
     * Get an instance of the Func1 That takes an CollectionIoEvent with an entity type 
     * for validation testing 
     */
    protected abstract void validateStage( CollectionIoEvent<MvccEntity> event );
}
