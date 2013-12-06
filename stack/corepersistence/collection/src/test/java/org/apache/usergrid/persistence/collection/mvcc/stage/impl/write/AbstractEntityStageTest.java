package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;

import org.junit.Test;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.stage.impl.IoEvent;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import rx.Observable;
import rx.util.functions.Func1;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** @author tnine */
@RunWith(Theories.class)
public abstract class AbstractEntityStageTest {

    @Parameterized.Parameter
    public Entity entity;

    /** Test every input with NonNull validation */
    @Test( expected = NullPointerException.class )
    @Theory
    public void testNoEntityId(@InvalidEntityGenerator.NullEntityFields final Entity entity) throws Exception {
        testStage( entity );
    }


    /** Test every Entity with */
    @Test( expected = IllegalArgumentException.class )
    @Theory
    public void testWrongEntityType(@InvalidEntityGenerator.IllegalEntityFields final Entity entity) throws Exception {
         testStage(entity);

    }


    /**
     * Test the stage, should throw an exception
     * @param entity
     */
    private void testStage(final Entity entity){


        final EntityCollection context = mock( EntityCollection.class );


        //run the stage
        Func1<IoEvent<Entity>, ?> newStage = getInstance();

        newStage.call( new IoEvent<Entity>( context, entity ) );
    }


    /** Get an instance of the Func1 That takes an IoEvent with an entity type for validation testing */
    protected abstract Func1<IoEvent<Entity>, ?> getInstance();
}
