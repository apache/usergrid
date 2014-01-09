package org.apache.usergrid.persistence.collection.migration;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.astyanax.MultiTennantColumnFamilyDefinition;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.exceptions.BadRequestException;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;


/**
 * Implementation of the migration manager to set up keyspace
 *
 * @author tnine
 */
@Singleton
public class MigrationManagerImpl implements MigrationManager {


    private static final Logger logger = LoggerFactory.getLogger( MigrationManagerImpl.class );

    private final Set<Migration> migrations;
    private final Keyspace keyspace;
    private final Properties props;

    private final MigrationManagerFig fig;


    @Inject
    public MigrationManagerImpl( final Keyspace keyspace, final Set<Migration> migrations, final Properties props,
                                 MigrationManagerFig fig ) {
        this.keyspace = keyspace;
        this.migrations = migrations;
        this.props = props;
        this.fig = fig;
    }


    @Override
    public void migrate() throws MigrationException {


        try {

            testAndCreateKeyspace();

            for ( Migration migration : migrations ) {

                final Collection<MultiTennantColumnFamilyDefinition> columnFamilies = migration.getColumnFamilies();


                if ( columnFamilies == null ) {
                    logger.warn(
                            "Class {} implements {} but returns null column families for migration.  Either implement"
                                    + " this method or remove the interface from the class", migration.getClass(),
                            Migration.class );
                    continue;
                }

                for ( MultiTennantColumnFamilyDefinition cf : columnFamilies ) {
                    testAndCreateColumnFamilyDef( cf );
                }
            }
        }
        catch ( Throwable t ) {
            logger.error( "Unable to perform migration", t );
            throw new MigrationException( "Unable to perform migration", t );
        }
    }


    /**
     * Check if the column family exists.  If it dosn't create it
     */
    private void testAndCreateColumnFamilyDef( MultiTennantColumnFamilyDefinition columnFamily )
            throws ConnectionException {
        final KeyspaceDefinition keyspaceDefinition = keyspace.describeKeyspace();

        final ColumnFamilyDefinition existing =
                keyspaceDefinition.getColumnFamily( columnFamily.getColumnFamily().getName() );

        if ( existing != null ) {
            return;
        }

        keyspace.createColumnFamily( columnFamily.getColumnFamily(), columnFamily.getOptions() );

        waitForMigration();
    }


    /**
     * Check if they keyspace exists.  If it doesn't create it
     */
    private void testAndCreateKeyspace() throws ConnectionException {


        KeyspaceDefinition keyspaceDefinition = null;

        try {
            keyspaceDefinition = keyspace.describeKeyspace();
        }
        catch ( BadRequestException badRequestException ) {

            //check if it's b/c the keyspace is missing, if so
            final String message = badRequestException.getMessage();

            boolean missingKeyspace = message.contains( "why:Keyspace" ) && message.contains( "does not exist" );

            if ( !missingKeyspace ) {
                throw badRequestException;
            }
        }


        if ( keyspaceDefinition != null ) {
            return;
        }


        ImmutableMap.Builder<String, Object> strategyOptions =
                ImmutableMap.<String, Object>builder().put( "replication_factor", fig.getReplicationFactor() );

        strategyOptions.putAll( getKeySpaceProps() );


        ImmutableMap<String, Object> options =
                ImmutableMap.<String, Object>builder().put( "strategy_class", fig.getStrategyClass() )
                            .put( "strategy_options", strategyOptions.build() ).build();


        keyspace.createKeyspace( options );

        waitForMigration();
    }


    /**
     * Get keyspace properties
     */
    private Map<String, String> getKeySpaceProps() {
        Map<String, String> keyspaceProps = new HashMap<String, String>();

        for ( Map.Entry<Object, Object> entry : props.entrySet() ) {
            final String key = entry.getKey().toString();

            if ( ! key.startsWith( fig.getKeyByMethod( "getStrategyOptions" ) ) ) {
                continue;
            }

            final String optionKey = key.substring( fig.getKeyByMethod( "getStrategyOptions" ).length() + 1 );

            keyspaceProps.put( optionKey, entry.getValue().toString() );
        }

        return keyspaceProps;
    }


    private void waitForMigration() throws ConnectionException {

        while ( true ) {

            final Map<String, List<String>> versions = keyspace.describeSchemaVersions();

            if ( versions != null && versions.size() == 1 ) {
                return;
            }

            //sleep and try it again
            try {
                Thread.sleep( 100 );
            }
            catch ( InterruptedException e ) {
                //swallow
            }
        }
    }
}
