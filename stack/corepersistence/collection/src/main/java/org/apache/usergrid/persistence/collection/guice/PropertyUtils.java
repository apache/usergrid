package org.apache.usergrid.persistence.collection.guice;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


/**
 * Simple Utility class to get properties
 *
 * @author tnine
 */
public class PropertyUtils {


    /**
     * Load the properties file from the classpath.  Throws IOException if they cannot be loaded
     */
    public static Properties loadFromClassPath( String propsFile ) {
        InputStream in = PropertyUtils.class.getClassLoader().getResourceAsStream( propsFile );

        if ( in == null ) {
            throw new RuntimeException( new IOException(
                    String.format( "Could not find properties file on the classpath at location %s", propsFile ) ) );
        }

        Properties props = new Properties();

        try {
            props.load( in );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        return props;
    }


    /**
     * Load each of the defined properties into a system property and return them.  If a system property is not found,
     * it will be ignored
     */
    public static Properties loadSystemProperties( String... properties ) {

        Properties props = new Properties();

        for ( String propName : properties ) {
            String propValue = System.getProperty( propName );

            if ( propValue != null ) {
                props.put( propName, propValue );
            }
        }


        return props;
    }
}
