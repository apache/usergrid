package org.apache.usergrid.persistence.collection.mvcc.stage.impl.write;


import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.collection.mvcc.stage.ExecutionContext;
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
public class UpdateTest {


    /** Test the start stage for happy path */
    @Test
    public void testValidInput() throws Exception {

        final ExecutionContext executionContext = mock( ExecutionContext.class );


        //set up the mock to return the entity from the start phase
        final Entity entity = new Entity();
        final UUID existingEntityId = UUIDGenerator.newTimeUUID();
        final long createdTime = 100;

        FieldUtils.writeDeclaredField( entity, "uuid", existingEntityId, true );
        entity.setCreated( createdTime );


        when( executionContext.getMessage( Entity.class ) ).thenReturn( entity );


        //mock returning the time
        final TimeService timeService = mock( TimeService.class );

        final long updateTime = System.currentTimeMillis();

        when( timeService.getTime() ).thenReturn( updateTime );


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );


        final UUID newVersion = UUIDGenerator.newTimeUUID();


        //mock the uuid service
        when( uuidService.newTimeUUID() ).thenReturn( newVersion );


        //perform the stage
        final Update create = new Update( timeService, uuidService );


        create.performStage( executionContext );


        //now verify our output was correct
        ArgumentCaptor<Entity> mvccEntity = ArgumentCaptor.forClass( Entity.class );


        verify( executionContext ).setMessage( mvccEntity.capture() );

        Entity created = mvccEntity.getValue();

        //verify uuid and version in both the MvccEntity and the entity itself
        assertEquals( "Entity re-set into context", entity, created );
        assertEquals( "entity id did not match generator", existingEntityId, created.getUuid() );
        assertEquals( "version did not not match entityId", newVersion, created.getVersion() );

        //check the time
        assertEquals( "created time matches generator", createdTime, created.getCreated() );
        assertEquals( "updated time matches generator", updateTime, created.getUpdated() );


        //now verify the proceed was called
        verify( executionContext ).proceed();
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


        //perform the stage
        final Update create = new Update( timeService, uuidService );

        //should throw an NPE
        create.performStage( executionContext );


    }

    @Test(expected = NullPointerException.class)
     public void testInvalidInputNoId() throws ConnectionException, ExecutionException, InterruptedException {

         final ExecutionContext executionContext = mock( ExecutionContext.class );


         when( executionContext.getMessage( Entity.class ) ).thenReturn( new Entity(  ) );


         //mock returning the time
         final TimeService timeService = mock( TimeService.class );


         //mock the uuid service
         final UUIDService uuidService = mock( UUIDService.class );


         //perform the stage
         final Update create = new Update( timeService, uuidService );

         //should throw an NPE
         create.performStage( executionContext );


     }


    /** Test no time service */
    @Test(expected = NullPointerException.class)
    public void testNoTimeService() throws ConnectionException, ExecutionException, InterruptedException {

        final ExecutionContext executionContext = mock( ExecutionContext.class );


        when( executionContext.getMessage( Entity.class ) ).thenReturn( null );


        //mock the uuid service
        final UUIDService uuidService = mock( UUIDService.class );


        //perform the stage
        new Update( null, uuidService );
    }


    /** Test no time service */
    @Test(expected = NullPointerException.class)
    public void testNoUUIDService() throws ConnectionException, ExecutionException, InterruptedException {

        final ExecutionContext executionContext = mock( ExecutionContext.class );


        when( executionContext.getMessage( Entity.class ) ).thenReturn( null );


        //mock returning the time
        final TimeService timeService = mock( TimeService.class );


        //throw NPE
        new Update( timeService, null );
    }

}
