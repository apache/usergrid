/*
 * Created by IntelliJ IDEA.
 * User: akarasulu
 * Date: 12/13/13
 * Time: 8:26 PM
 */
package org.apache.usergrid.persistence.collection.cassandra;


import java.util.Properties;

import org.apache.usergrid.persistence.collection.archaius.DynamicPropertyNames;
import org.apache.usergrid.persistence.collection.guice.PropertyUtils;

import com.google.inject.AbstractModule;


/**
 * This Module is responsible for injecting dynamic properties into a {@link
 * DynamicCassandraConfig} object and injecting a singleton instance of it
 * anywhere an {@link ICassandraConfig} or {@link IDynamicCassandraConfig} is
 * required.
 */
public class CassandraConfigModule extends AbstractModule {
    /** The location of the defaults properties file */
    private static final String CASSANDRA_DEFAULTS_PROPERTIES = "cassandra-defaults.properties";

    /** Additional defaults overrides to properties */
    private final Properties overrides = new Properties();

    public CassandraConfigModule() {}

    public CassandraConfigModule( Properties overrides ) {
        this.overrides.putAll( overrides );
    }


    protected void configure() {
        bind( ICassandraConfig.class ).to( DynamicCassandraConfig.class ).asEagerSingleton();
        bind( IDynamicCassandraConfig.class ).to( DynamicCassandraConfig.class ).asEagerSingleton();

        // Load from the defaults properties file
        Properties props = PropertyUtils.loadFromClassPath( CASSANDRA_DEFAULTS_PROPERTIES );

        // Apply programmatic overrides
        props.putAll( overrides );

        // Apply command line user overrides
        props.putAll( PropertyUtils.loadSystemProperties( ICassandraConfig.OPTIONS ) );

        // Extract only the properties we care about
        Properties extracted = new Properties();
        for ( String key : ICassandraConfig.OPTIONS ) {
            extracted.setProperty( key, props.getProperty( key ) );
        }

        // Finally convert overlaid properties into dynamic property bindings
        DynamicPropertyNames.bindProperties( binder(), extracted );
    }
}
