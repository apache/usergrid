/*
 * Created by IntelliJ IDEA.
 * User: akarasulu
 * Date: 12/13/13
 * Time: 8:26 PM
 */
package org.apache.usergrid.persistence.collection.cassandra;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.SystemConfiguration;

import org.apache.usergrid.persistence.collection.archaius.DynamicPropertyNames;
import org.apache.usergrid.persistence.collection.guice.PropertyUtils;

import com.google.inject.AbstractModule;
import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConcurrentMapConfiguration;
import com.netflix.config.ConfigurationManager;

import static org.apache.usergrid.persistence.collection.guice.PropertyUtils.filter;
import static org.apache.usergrid.persistence.collection.guice.PropertyUtils.loadFromClassPath;
import static org.apache.usergrid.persistence.collection.guice.PropertyUtils.loadSystemProperties;


/**
 * This Module is responsible for injecting dynamic properties into a {@link
 * DynamicCassandraConfig} object and injecting a singleton instance of it
 * anywhere an {@link ICassandraConfig} or {@link IDynamicCassandraConfig} is
 * required.
 */
public class CassandraConfigModule extends AbstractModule {
    /** The location of the defaults properties file */
    private static final String CASSANDRA_DEFAULTS_PROPERTIES = "cassandra-defaults.properties";

    /** Additional value overrides (not defaults overrides) to properties */
    private final Map<String,Object> overrides;


    public CassandraConfigModule() {
        overrides = Collections.emptyMap();
    }


    public CassandraConfigModule( Map<String,Object> overrides ) {
        this.overrides = new HashMap<String, Object>();
        this.overrides.putAll( overrides );
    }


    protected void configure() {

        // This loads the overriding values into the configuration
        if ( ConfigurationManager.getConfigInstance() instanceof ConcurrentCompositeConfiguration ) {
            ConcurrentCompositeConfiguration config =
                    ( ConcurrentCompositeConfiguration ) ConfigurationManager.getConfigInstance();
            Map<String,Object> values = new HashMap<String, Object>( filter( ICassandraConfig.OPTIONS, overrides ) );
            //noinspection unchecked
            values.putAll( ( Map ) loadSystemProperties( ICassandraConfig.OPTIONS ) );
            ConcurrentMapConfiguration mapConfiguration = new ConcurrentMapConfiguration( values );
            config.addConfigurationAtFront( mapConfiguration, "CassandraConfigModuleConfig" );
        }

        // Generate the defaults for dynamic properties - config has our
        // values if overrides and config files provided them
        new DynamicPropertyNames().bindProperties( binder(), loadFromClassPath( CASSANDRA_DEFAULTS_PROPERTIES ) );

        bind( ICassandraConfig.class ).to( DynamicCassandraConfig.class ).asEagerSingleton();
        bind( IDynamicCassandraConfig.class ).to( DynamicCassandraConfig.class ).asEagerSingleton();

    }
}
