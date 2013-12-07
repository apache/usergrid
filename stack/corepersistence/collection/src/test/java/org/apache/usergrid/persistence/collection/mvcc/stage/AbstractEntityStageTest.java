package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import rx.util.functions.Func1;

import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith(Theories.class)
public abstract class AbstractEntityStageTest {

    /** Test every input with NonNull validation */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNoEntityId(@InvalidEntityGenerator.NullFields final Entity entity) throws Exception {
        testStage( entity );
    }


    /** Test every Entity with */
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testWrongEntityType(@InvalidEntityGenerator.IllegalFields final Entity entity) throws Exception {
         testStage(entity);

    }


    /**
     * Test the stage, should throw an exception
     * @param entity
     */
    private void testStage(final Entity entity){


        final EntityCollection context = mock( EntityCollection.class );


        //run the stage
        validateStage( new IoEvent<Entity>( context, entity ) );
    }


    /** Get an instance of the Func1 That takes an IoEvent with an entity type for validation testing */
    protected abstract void validateStage(IoEvent<Entity> event);

}
