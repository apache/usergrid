package org.apache.usergrid.persistence.collection.cassandra;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.RandomStringUtils;

import org.apache.usergrid.persistence.collection.guice.PropertyUtils;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.config.sources.URLConfigurationSource;

import static org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig.CASSANDRA_CLUSTER_NAME;
import static org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig.CASSANDRA_CONNECTIONS;
import static org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig.CASSANDRA_HOSTS;
import static org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig.CASSANDRA_PORT;
import static org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig.CASSANDRA_TIMEOUT;
import static org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig.CASSANDRA_VERSION;
import static org.apache.usergrid.persistence.collection.cassandra.ICassandraConfig.COLLECTIONS_KEYSPACE_NAME;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.Assert.assertTrue;


/**
 * Tests the proper operation of the DynamicCassandraConfig.
 */
@RunWith( JukitoRunner.class )
public class DynamicCassandraConfigTest {

    private static final String APP_CONFIG = "dynamic-test.properties";
    private static final Logger LOG = LoggerFactory.getLogger( DynamicCassandraConfigTest.class );
    private static final Properties original = PropertyUtils.loadFromClassPath( APP_CONFIG );
    private static FixedDelayPollingScheduler scheduler;
    private static DynamicConfiguration dynamicConfig;
    private static ConcurrentCompositeConfiguration finalConfig;


    @BeforeClass
    public static void setup() {
        scheduler = new FixedDelayPollingScheduler( 100, 100, false );
        URLConfigurationSource source = new URLConfigurationSource( ClassLoader.getSystemResource( APP_CONFIG ) );
        dynamicConfig = new DynamicConfiguration( source, scheduler );

        finalConfig = new ConcurrentCompositeConfiguration();
        finalConfig.addConfiguration( dynamicConfig );
        ConfigurationManager.install( finalConfig );
    }


    @AfterClass
    public static void restore() throws Exception {
        write( original );
        finalConfig.removeConfiguration( dynamicConfig );
        scheduler.stop();
    }


    @Test
    @UseModules( CassandraConfigModule.class )
    public void testSingleEvent( IDynamicCassandraConfig config ) throws Exception {
        final Set<ConfigChangeType> changes = new HashSet<ConfigChangeType>();
        final List<CassandraConfigEvent> events = new ArrayList<CassandraConfigEvent>();

        Properties properties = PropertyUtils.loadFromClassPath( APP_CONFIG );
        String oldPort = properties.getProperty( CASSANDRA_PORT );
        LOG.debug( "old port = {}", oldPort );

        assertNotNull( config );
        config.register( new CassandraConfigListener() {
            @Override
            public void reconfigurationEvent( final CassandraConfigEvent event ) {
                LOG.debug( "got reconfiguration even: {}" + event );
                changes.addAll( event.getChanges() );
                events.add( event );
            }
        } );

        // change the property
        String newPort = getRandomNumber( oldPort );
        LOG.debug( "new port = {}", oldPort );
        properties.setProperty( CASSANDRA_PORT, newPort );
        long startTime = System.currentTimeMillis();
        write( properties );

        while ( changes.isEmpty() ) {
            Thread.sleep( 100L );
            LOG.debug( "waking up" );
        }
        long propagationTime = System.currentTimeMillis() - startTime;

        assertTrue( "the default notification delay is not working: propagation time = " + propagationTime,
                propagationTime >= DynamicCassandraConfig.DEFAULT_NOTIFICATION_DELAY );

        assertEquals( "there should only be one changed property", 1, changes.size() );
        assertEquals( "there should only be one event", 1, events.size() );
        assertEquals( "only the port should change", ConfigChangeType.PORT, changes.iterator().next() );
        assertEquals( "the old port does not match", Integer.parseInt( oldPort ), events.get( 0 ).getOld().getPort() );
        assertEquals( "the new port does not match", Integer.parseInt( newPort ), events.get( 0 ).getCurrent().getPort() );
        assertEquals( "only one event change should be present", 1, events.get( 0 ).getChanges().size() );
        assertTrue( "the port change should exist", events.get( 0 ).hasChange( ConfigChangeType.PORT ) );

    }


    @Test
    @UseModules( CassandraConfigModule.class )
    public void testAllEvents( IDynamicCassandraConfig config ) throws Exception {
        final Set<ConfigChangeType> changes = new HashSet<ConfigChangeType>();
        final List<CassandraConfigEvent> events = new ArrayList<CassandraConfigEvent>();

        Properties properties = PropertyUtils.loadFromClassPath( APP_CONFIG );
        String oldPort = properties.getProperty( CASSANDRA_PORT );
        String oldConnections = properties.getProperty( CASSANDRA_CONNECTIONS );
        String oldCluster = properties.getProperty( CASSANDRA_CLUSTER_NAME );
        String oldHosts = properties.getProperty( CASSANDRA_HOSTS );
        String oldTimeout = properties.getProperty( CASSANDRA_TIMEOUT );
        String oldVersion = properties.getProperty( CASSANDRA_VERSION );
        String oldKeyspace = properties.getProperty( COLLECTIONS_KEYSPACE_NAME );

        LOG.debug( "old port = {}", oldPort );
        LOG.debug( "old connections = {}", oldConnections );
        LOG.debug( "old cluster = {}", oldCluster );
        LOG.debug( "old hosts = {}", oldHosts );
        LOG.debug( "old timeout = {}", oldTimeout );
        LOG.debug( "old version = {}", oldVersion );
        LOG.debug( "old keyspace = {}", oldKeyspace );

        assertNotNull( config );
        CassandraConfigListener listener = new CassandraConfigListener() {
            @Override
            public void reconfigurationEvent( final CassandraConfigEvent event ) {
                LOG.debug( "got reconfiguration even: {}" + event );
                changes.addAll( event.getChanges() );
                events.add( event );
            }
        };
        config.register( listener );

        // change the property
        String newPort = getRandomNumber( oldPort );
        String newConnections = getRandomNumber( oldConnections );
        String newCluster = getRandomString( oldCluster );
        String newHosts = getRandomString( oldHosts );
        String newTimeout = getRandomNumber( oldTimeout );
        String newVersion = getRandomString( oldVersion );
        String newKeyspace = getRandomString( oldKeyspace );

        LOG.debug( "new port = {}", newPort );
        LOG.debug( "new connections = {}", newConnections );
        LOG.debug( "new cluster = {}", newCluster );
        LOG.debug( "new hosts = {}", newHosts );
        LOG.debug( "new timeout = {}", newTimeout );
        LOG.debug( "new version = {}", newVersion );
        LOG.debug( "new keyspace = {}", newKeyspace );

        properties.setProperty( CASSANDRA_PORT, newPort );
        properties.setProperty( CASSANDRA_CONNECTIONS, newConnections );
        properties.setProperty( CASSANDRA_CLUSTER_NAME, newCluster );
        properties.setProperty( CASSANDRA_HOSTS, newHosts );
        properties.setProperty( CASSANDRA_TIMEOUT, newTimeout );
        properties.setProperty( CASSANDRA_VERSION, newVersion );
        properties.setProperty( COLLECTIONS_KEYSPACE_NAME, newKeyspace );

        long startTime = System.currentTimeMillis();
        write( properties );

        while ( changes.size() < 7 ) {
            Thread.sleep( 100L );
            LOG.debug( "waking up" );
        }
        long propagationTime = System.currentTimeMillis() - startTime;

        assertTrue( "the default notification delay is not working: propagation time = " + propagationTime,
                propagationTime >= DynamicCassandraConfig.DEFAULT_NOTIFICATION_DELAY );

        assertEquals( "there should be 7 changed properties", 7, changes.size() );
        assertEquals( "there should only be one event", 1, events.size() );

        assertEquals( "the old port does not match", Integer.parseInt( oldPort ), events.get( 0 ).getOld().getPort() );
        assertEquals( "the new port does not match", Integer.parseInt( newPort ), events.get( 0 ).getCurrent().getPort() );
        assertTrue( "the port change should exist", events.get( 0 ).hasChange( ConfigChangeType.PORT ) );

        assertEquals( "the old connections does not match", Integer.parseInt( oldConnections ), events.get( 0 ).getOld().getConnections() );
        assertEquals( "the new connections does not match", Integer.parseInt( newConnections ), events.get( 0 ).getCurrent().getConnections() );
        assertTrue( "the connections change should exist", events.get( 0 ).hasChange( ConfigChangeType.CONNECTIONS ) );

        assertEquals( "the old cluster does not match", oldCluster, events.get( 0 ).getOld().getClusterName() );
        assertEquals( "the new cluster does not match", newCluster, events.get( 0 ).getCurrent().getClusterName() );
        assertTrue( "the cluster change should exist", events.get( 0 ).hasChange( ConfigChangeType.CLUSTER_NAME ) );

        assertEquals( "the old hosts does not match", oldHosts, events.get( 0 ).getOld().getHosts() );
        assertEquals( "the new hosts does not match", newHosts, events.get( 0 ).getCurrent().getHosts() );
        assertTrue( "the hosts change should exist", events.get( 0 ).hasChange( ConfigChangeType.HOSTS ) );

        assertEquals( "the old timeout does not match", Integer.parseInt( oldTimeout ), events.get( 0 ).getOld().getTimeout() );
        assertEquals( "the new timeout does not match", Integer.parseInt( newTimeout ), events.get( 0 ).getCurrent().getTimeout() );
        assertTrue( "the timeout change should exist", events.get( 0 ).hasChange( ConfigChangeType.TIMEOUT ) );

        assertEquals( "the old version does not match", oldVersion, events.get( 0 ).getOld().getVersion() );
        assertEquals( "the new version does not match", newVersion, events.get( 0 ).getCurrent().getVersion() );
        assertTrue( "the version change should exist", events.get( 0 ).hasChange( ConfigChangeType.VERSION ) );

        assertEquals( "the old keyspace does not match", oldKeyspace, events.get( 0 ).getOld().getKeyspaceName() );
        assertEquals( "the new keyspace does not match", newKeyspace, events.get( 0 ).getCurrent().getKeyspaceName() );
        assertTrue( "the keyspace change should exist", events.get( 0 ).hasChange( ConfigChangeType.KEYSPACE_NAME ) );

        assertEquals( "7 event change should be present", 7, events.get( 0 ).getChanges().size() );

        // now we test that unregister actually works

        changes.clear();
        events.clear();
        config.unregister( listener );

        write( original );

        Thread.sleep( DynamicCassandraConfig.DEFAULT_NOTIFICATION_DELAY );

        assertTrue( "events should be empty with listener removed", events.isEmpty() );
        assertTrue( "changes should be empty with listener removed", changes.isEmpty() );
    }


    private static void write( Properties properties ) throws IOException {
        URL path = ClassLoader.getSystemClassLoader().getResource( APP_CONFIG );
        assert path != null;
        File file = new File( path.getFile() );
        FileOutputStream out = new FileOutputStream( file );
        properties.store( out, null );
        out.flush();
        out.close();
    }


    private static String getRandomString( String oldValue ) {
        if ( oldValue == null ) {
            return RandomStringUtils.randomAlphabetic( 7 );
        }

        // make sure we do not generate random port same as old - no change event that way
        String newValue = RandomStringUtils.randomAlphabetic( oldValue.length() );
        while ( newValue.equals( oldValue ) ) {
            newValue = RandomStringUtils.randomAlphabetic( oldValue.length() );
        }

        return newValue;
    }


    private static String getRandomNumber( String oldNumber ) {
        if ( oldNumber == null ) {
            return RandomStringUtils.randomNumeric( 4 );
        }

        // make sure we do not generate random number same as old - no change event that way
        String newValue = RandomStringUtils.randomNumeric( oldNumber.length() );
        while ( newValue.equals( oldNumber) ) {
            newValue = RandomStringUtils.randomNumeric( oldNumber.length() );
        }

        return newValue;
    }
}
