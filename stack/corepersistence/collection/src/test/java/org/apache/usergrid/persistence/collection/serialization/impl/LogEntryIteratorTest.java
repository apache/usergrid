package org.apache.usergrid.persistence.collection.serialization.impl;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import org.junit.Test;

import org.apache.usergrid.persistence.collection.MvccLogEntry;
import org.apache.usergrid.persistence.collection.serialization.MvccLogEntrySerializationStrategy;
import org.apache.usergrid.persistence.collection.util.LogEntryMock;
import org.apache.usergrid.persistence.collection.util.VersionGenerator;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests iterator paging
 */
public class LogEntryIteratorTest {


    @Test
    public void empty() throws ConnectionException {

        final MvccLogEntrySerializationStrategy logEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );

        final ApplicationScope scope =
                new ApplicationScopeImpl( new SimpleId( "application" ));

        final Id entityId = new SimpleId( "entity" );

        final int pageSize = 100;


        //set the start version, it should be discarded
        UUID start = UUIDGenerator.newTimeUUID();

        when( logEntrySerializationStrategy.load( same( scope ), same( entityId ), same( start ), same( pageSize ) ) )
                .thenReturn( new ArrayList<MvccLogEntry>() );


        //now iterate we should get everything
        LogEntryIterator itr = new LogEntryIterator( logEntrySerializationStrategy, scope, entityId, start, pageSize );


        assertFalse( itr.hasNext() );
    }


    @Test
    public void partialLastPage() throws ConnectionException {


        final int pageSize = 10;
        final int totalPages = 3;
        final int lastPageSize = pageSize / 2;

        //have one half page

        pageElements( pageSize, totalPages, lastPageSize );
    }


    @Test
    public void emptyLastPage() throws ConnectionException {


        final int pageSize = 10;
        final int totalPages = 3;
        final int lastPageSize = 0;

        //have one half page

        pageElements( pageSize, totalPages, lastPageSize );
    }


    public void pageElements( final int pageSize, final int totalPages, final int lastPageSize )
            throws ConnectionException {

        final MvccLogEntrySerializationStrategy logEntrySerializationStrategy =
                mock( MvccLogEntrySerializationStrategy.class );

        final ApplicationScope scope =
                new ApplicationScopeImpl( new SimpleId( "application" ) );

        final Id entityId = new SimpleId( "entity" );


        //have one half page
        final int toGenerate = pageSize * totalPages + lastPageSize;


        final LogEntryMock mockResults =
                LogEntryMock.createLogEntryMock( logEntrySerializationStrategy, scope, entityId, VersionGenerator.generateVersions( toGenerate ) );

        Iterator<MvccLogEntry> expectedEntries = mockResults.getEntries().iterator();

        //this element should be skipped
        UUID start = expectedEntries.next().getVersion();

        //now iterate we should get everything
        LogEntryIterator itr = new LogEntryIterator( logEntrySerializationStrategy, scope, entityId, start, pageSize );


        while ( expectedEntries.hasNext() && itr.hasNext() ) {
            final MvccLogEntry expected = expectedEntries.next();

            final MvccLogEntry returned = itr.next();

            assertEquals( expected, returned );
        }


        assertFalse( itr.hasNext() );
        assertFalse( expectedEntries.hasNext() );
    }
}
