package org.apache.usergrid.persistence.map;


import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.apache.usergrid.persistence.collection.guice.MigrationManagerRule;
import org.apache.usergrid.persistence.core.cassandra.ITRunner;
import org.apache.usergrid.persistence.map.guice.TestMapModule;
import org.apache.usergrid.persistence.map.impl.MapScopeImpl;
import org.apache.usergrid.persistence.model.entity.SimpleId;

import com.google.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


@RunWith( ITRunner.class )
@UseModules( { TestMapModule.class } )
public class MapManagerTest {


    @Inject
    @Rule
    public MigrationManagerRule migrationManagerRule;


    @Inject
    protected MapManagerFactory mmf;

    protected MapScope scope;


    @Before
    public void mockApp() {
        this.scope = new MapScopeImpl( new SimpleId( "application" ), "testMap" );
    }


    @Test
    public void writeReadString() {
        MapManager mm = mmf.getMapManager( this.scope );

        final String key = "key";
        final String value = "value";

        mm.putString( key, value );

        final String returned = mm.getString( key );

        assertEquals( value, returned );
    }


    @Test
    public void readMissingEntry() {
        MapManager mm = mmf.getMapManager( this.scope );

        final String returned = mm.getString( "key" );

        assertNull( returned );
    }


    @Test
    public void deleteString() {
        MapManager mm = mmf.getMapManager( this.scope );

        final String key = "key";
        final String value = "value";

        mm.putString( key, value );

        final String returned = mm.getString( key );

        assertEquals( value, returned );

        mm.delete( key );

        final String postDelete = mm.getString( key );

        assertNull( postDelete );
    }


    @Test( expected = IllegalArgumentException.class )
    public void nullInput() {
        MapManager mm = mmf.getMapManager( this.scope );

        mm.putString( null, null );
    }
}
