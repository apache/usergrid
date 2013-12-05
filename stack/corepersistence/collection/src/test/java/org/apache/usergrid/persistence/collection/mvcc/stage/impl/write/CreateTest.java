package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.usergrid.persistence.collection.EntityCollection;
import org.apache.usergrid.persistence.collection.mvcc.entity.CollectionEventBus;
import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;
import org.apache.usergrid.persistence.collection.mvcc.stage.Result;
import org.apache.usergrid.persistence.collection.service.TimeService;
import org.apache.usergrid.persistence.collection.service.UUIDService;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/** @author tnine */
public class CreateTest {

    /** Test the start stage for happy path */
    @Test
    public void testValidInput() throws ConnectionException, ExecutionException, InterruptedException {


        //mock returning the time
        final TimeService timeService = mock( TimeService.class );

        final long time = System.currentTimeMillis();

        when( timeService.getTime() ).thenReturn( time );


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );

        final UUID newEntityId = UUIDGenerator.newTimeUUID();
        final UUID newVersion = newEntityId;


        //mock the uuid service
        when( uuidService.newTimeUUID() ).thenReturn( newEntityId );

        final CollectionEventBus eventBus = mock( CollectionEventBus.class );

        Result result = new Result();


        //perform the stage
        final Create create = new Create(eventBus,  timeService, uuidService );



        //set up the mock to return the entity from the start phase
        final Entity entity = new Entity();


        final EntityCollection context = mock(EntityCollection.class);

        EventCreate createEvent = new EventCreate(context,  entity, result );
        create.performStage( createEvent );



        //now verify our output was correct
        ArgumentCaptor<EventStart> event = ArgumentCaptor.forClass( EventStart.class );


        verify( eventBus ).post( event.capture() );

        Entity created = event.getValue().getData();

        //verify uuid and version in both the MvccEntity and the entity itself
        assertEquals( "Entity re-set into context", entity, created );
        assertEquals( "entity id did not match generator", newEntityId, created.getUuid() );
        assertEquals( "version did not not match entityId", newVersion, created.getVersion() );

        //check the time
        assertEquals( "created time matches generator", time, created.getCreated() );
        assertEquals( "updated time matches generator", time, created.getUpdated() );


    }


    /** Test the start stage for happy path */
    @Test(expected = NullPointerException.class)
    public void testInvalidInput() throws ConnectionException, ExecutionException, InterruptedException {

        final ExecutionContext executionContext = mock( ExecutionContext.class );


        when( executionContext.getMessage( Entity.class ) ).thenReturn( null );


        //mock returning the time
        final TimeService timeService = mock( TimeService.class );


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );

        final CollectionEventBus eventBus = mock(CollectionEventBus.class);



        //perform the stage
        final Create create = new Create( eventBus, timeService, uuidService );

        //should throw an NPE
        create.performStage( null );


    }


    /** Test no time service */
    @Test(expected = NullPointerException.class)
    public void testNoEventBus() throws ConnectionException, ExecutionException, InterruptedException {




        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );

        final TimeService timeService = mock(TimeService.class);



        //perform the stage
        new Create( null, timeService, uuidService );
    }



    /** Test no time service */
    @Test(expected = NullPointerException.class)
    public void testNoTimeService() throws ConnectionException, ExecutionException, InterruptedException {


        final CollectionEventBus eventBus = mock(CollectionEventBus.class);

        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );



        //perform the stage
        new Create( eventBus, null, uuidService );
    }


    /** Test no time service */
    @Test(expected = NullPointerException.class)
    public void testNoUUIDService() throws ConnectionException, ExecutionException, InterruptedException {

        final CollectionEventBus eventBus = mock(CollectionEventBus.class);


        //mock returning the time
        final TimeService timeService = mock( TimeService.class );


        //throw NPE
        new Create(eventBus,  timeService, null );
    }
}
