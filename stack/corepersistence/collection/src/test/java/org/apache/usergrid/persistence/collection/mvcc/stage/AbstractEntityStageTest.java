package org.apache.usergrid.persistence.collection.mvcc.stage;


import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.collection.util.InvalidEntityGenerator;
import org.apache.usergrid.persistence.collection.util.InvalidIdGenerator;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;

import static org.mockito.Mockito.mock;


/** @author tnine */
@RunWith(Theories.class)
public abstract class AbstractEntityStageTest {

    /** Test every input with NonNull validation */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNoEntityId(@InvalidEntityGenerator.NullFields final Entity entity, @InvalidIdGenerator.NullFields final Id id) throws Exception {
        testStage(entity, id );
    }


    /** Test every Entity with */
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testWrongEntityType(@InvalidEntityGenerator.IllegalFields final Entity entity, @InvalidIdGenerator.IllegalFields final Id id) throws Exception {
         testStage(entity, id);
    }


    /**
     * Test the stage, should throw an exception
     * @param id
     */
    private void testStage(final Entity entity, final Id id){


        final CollectionScope context = mock( CollectionScope.class );

        if(entity != null){
            EntityUtils.setId( entity, id );
        }


        //run the stage
        validateStage( new CollectionIoEvent<Entity>( context, entity ) );
    }


    /** Get an instance of the Func1 That takes an CollectionIoEvent with an entity type for validation testing */
    protected abstract void validateStage(CollectionIoEvent<Entity> event);

}
