package org.apache.usergrid.persistence.collection.migration;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.collection.astynax.MultiTennantColumnFamilyDefinition;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
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

    public static final String STRATEGY_CLASS = "collections.keyspace.strategy.class";
    public static final String STRATEGY_OPTIONS = "collections.keyspace.strategy.options";
    public static final String REPLICATION_FACTOR = "collections.keyspace.replicationfactor";


    private final String strategyClass;
    private final String replicationFactor;


    private final Set<Migration> migrations;
    private final Keyspace keyspace;
    private final Properties props;


    @Inject
    public MigrationManagerImpl( final Keyspace keyspace, final Set<Migration> migrations, final Properties props,
                                 @Named(STRATEGY_CLASS) final String strategyClass,
                                 @Named(REPLICATION_FACTOR) final String replicationFactor ) {
        this.keyspace = keyspace;
        this.migrations = migrations;
        this.props = props;
        this.strategyClass = strategyClass;
        this.replicationFactor = replicationFactor;
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


    /** Check if the column family exists.  If it dosn't create it */
    private void testAndCreateColumnFamilyDef( MultiTennantColumnFamilyDefinition columnFamily ) throws ConnectionException {
        final KeyspaceDefinition keyspaceDefinition = keyspace.describeKeyspace();

        final ColumnFamilyDefinition existing =
                keyspaceDefinition.getColumnFamily( columnFamily.getColumnFamily().getName() );

        if ( existing != null ) {
            return;
        }

        keyspace.createColumnFamily( columnFamily.getColumnFamily(), columnFamily.getOptions() );

        waitForMigration();
    }


    /** Check if they keyspace exists.  If it doesn't create it */
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
                ImmutableMap.<String, Object>builder().put( "replication_factor", replicationFactor );

        strategyOptions.putAll( getKeySpaceProps() );


        ImmutableMap<String, Object> options =
                ImmutableMap.<String, Object>builder().put( "strategy_class", strategyClass )
                            .put( "strategy_options", strategyOptions.build() ).build();


        keyspace.createKeyspace( options );

        waitForMigration();
    }


    /** Get keyspace properties */
    private Map<String, String> getKeySpaceProps() {
        Map<String, String> keyspaceProps = new HashMap<String, String>();

        for ( Map.Entry<Object, Object> entry : props.entrySet() ) {
            final String key = entry.getKey().toString();

            if ( !key.startsWith( STRATEGY_OPTIONS ) ) {
                continue;
            }

            final String optionKey = key.substring( STRATEGY_OPTIONS.length() + 1 );

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
