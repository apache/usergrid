package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.util.InvalidIdGenerator;
import org.apache.usergrid.persistence.model.entity.Id;

import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith( Theories.class )
public abstract class AbstractIdStageTest {


    /**
     * Tests all possible combinations that will result in a NullPointerException input fail the MvccEntity interface to be a
     * mockito mock impl
     */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNonNullable(@InvalidIdGenerator.NullFields final Id id ) throws Exception {
        testStage( id );
    }


    /**
     * Tests all possible combinations that will result in an invalid input Excepts the MvccEntity interface to be a
     * mockito mock impl
     */
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testInvalidValue(@InvalidIdGenerator.IllegalFields final Id id ) throws Exception {
            testStage( id );
    }


    /**
     * Run the stage's invalid I/O tests
     * @param id
     * @throws Exception
     */
    public void testStage( final Id id ) throws Exception {

           final CollectionScope context = mock( CollectionScope.class );


           //run the stage
           validateStage( new CollectionIoEvent<Id>( context, id ) );
       }


    /** Get an instance of the Func1 That takes an CollectionIoEvent with an entity type for validation testing */
    protected abstract void validateStage(CollectionIoEvent<Id> event);
}
