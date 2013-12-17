package org.apache.usergrid.persistence.collection.archaius;


import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicDoubleProperty;
import com.netflix.config.DynamicFloatProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;


/**
 * Utilities to use for binding Properties to dynamic properties for Guice injection. Data type specific
 * binding can be implicitly or explicitly triggered. Explicitly just append to the end of the property
 * '.' and the primitive type identifier like so:
 *
 * <ul>
 *     <li>.int</li>
 *     <li>.boolean</li>
 *     <li>.long</li>
 *     <li>.double</li>
 *     <li>.float</li>
 * </ul>
 *
 * NOTE: if the key is foobar.long the binding produced is based on the key base. Effectively the key
 * is foobar when accessing the property via Named( "foobar" ).
 *
 * Implicit interpretation runs off of Java type formats. For example any whole number will be interpreted
 * as an int. However if it ends in "L" then it will be interpreted as a long data type. If whole numbers
 * or numbers with decimals end with a "F" they are interpreted as float primitive types. A decimal
 * number value without ending in "F" will be interpreted as a double primitive type.
 *
 * Sometimes you want numbers as strings. To force a Strings even for a number value just use the .String type
 * specifier at the end of the property name.
 */
public class DynamicPropertyNames {
    private static final Logger LOG = LoggerFactory.getLogger( DynamicPropertyNames.class );

    /**
     * Binds property keys to Named type and uses property values as defaults.
     *
     * @param binder the Binder to bind to
     * @param properties the properties to create the bindings on
     */
    public static void bindProperties( Binder binder, Properties properties )
    {
        if ( properties == null ) {
            throw new NullPointerException( "DynamicPropertyNames: " +
                    "properties argument to bindProperties() must not be null" );
        }

        if ( binder == null ) {
            throw new NullPointerException( "DynamicPropertyNames: " +
                    "binder argument to bindProperties() must not be null" );
        }

        // use enumeration to include the default properties
        for ( Enumeration<?> e = properties.propertyNames(); e.hasMoreElements(); )
        {
            String propertyName = ( String ) e.nextElement();
            String propertyValue = properties.getProperty( propertyName );

            LOG.debug( "setting up a dynamic property for key {} with default value {}", propertyName, propertyValue );

            if ( propertyName.endsWith( ".int" ) )
            {
                String namedName = propertyName.substring( 0, propertyName.length() - 4 );
                DynamicIntProperty intProperty = DynamicPropertyFactory.getInstance()
                        .getIntProperty( propertyName, Integer.valueOf( propertyValue ) );
                binder.bind( Key.get( DynamicIntProperty.class,
                        new NamedDynamicProperties( namedName ) ) ).toInstance( intProperty );
            }
            else if ( propertyName.endsWith( ".long" ) )
            {
                String namedName = propertyName.substring( 0, propertyName.length() - 5 );
                DynamicLongProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                        .getLongProperty( propertyName, Long.valueOf( propertyValue ) );
                binder.bind( Key.get( DynamicLongProperty.class,
                        new NamedDynamicProperties( namedName ) ) ).toInstance( dynamicProperty );
            }
            else if ( propertyName.endsWith( ".boolean" ) )
            {
                String namedName = propertyName.substring( 0, propertyName.length() - 8 );
                DynamicBooleanProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                        .getBooleanProperty( propertyName, Boolean.getBoolean( propertyValue ) );
                binder.bind( Key.get( DynamicBooleanProperty.class,
                        new NamedDynamicProperties( namedName ) ) ).toInstance( dynamicProperty );
            }
            else if ( propertyName.endsWith( ".double" ) )
            {
                String namedName = propertyName.substring( 0, propertyName.length() - 7 );
                DynamicDoubleProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                        .getDoubleProperty( propertyName, Double.valueOf( propertyValue ) );
                binder.bind( Key.get( DynamicDoubleProperty.class,
                        new NamedDynamicProperties( namedName ) ) ).toInstance( dynamicProperty );
            }
            else if ( propertyName.endsWith( ".float" ) )
            {
                String namedName = propertyName.substring( 0, propertyName.length() - 6 );
                DynamicFloatProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                        .getFloatProperty( propertyName, Float.valueOf( propertyValue ) );
                binder.bind( Key.get( DynamicFloatProperty.class,
                        new NamedDynamicProperties( namedName ) ) ).toInstance( dynamicProperty );
            }
            else if ( propertyName.endsWith( ".String" ) ) {
                String namedName = propertyName.substring( 0, propertyName.length() - 7 );
                DynamicStringProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                        .getStringProperty( propertyName, propertyValue );
                binder.bind( Key.get( DynamicStringProperty.class,
                        new NamedDynamicProperties( namedName ) ) ).toInstance( dynamicProperty );
            }

            handleUnknownType( binder, propertyName, propertyValue );
        }
    }


    private static void handleUnknownType( Binder binder, String name, String value ) {
        if ( isBoolean( value ) ) {
            DynamicBooleanProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                    .getBooleanProperty( name, Boolean.getBoolean( value ) );
            binder.bind( Key.get( DynamicBooleanProperty.class,
                    new NamedDynamicProperties( name ) ) ).toInstance( dynamicProperty );
        }
        else if ( isFloat( value ) ) {
            if ( value.endsWith( "F" ) || value.endsWith( "f" ) ) {
                value = value.substring( 0, value.length() - 1 );
            }
            DynamicFloatProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                    .getFloatProperty( name, Float.valueOf( value ) );
            binder.bind( Key.get( DynamicFloatProperty.class,
                    new NamedDynamicProperties( name ) ) ).toInstance( dynamicProperty );
        }
        else if ( isLong( value ) ) {
            if ( value.endsWith( "L" ) || value.endsWith( "l" ) ) {
                value = value.substring( 0, value.length() - 1 );
            }
            DynamicLongProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                    .getLongProperty( name, Long.valueOf( value ) );
            binder.bind( Key.get( DynamicLongProperty.class,
                    new NamedDynamicProperties( name ) ) ).toInstance( dynamicProperty );
        }
        else if ( isDouble( value ) ) {
            DynamicDoubleProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                    .getDoubleProperty( name, Double.valueOf( value ) );
            binder.bind( Key.get( DynamicDoubleProperty.class,
                    new NamedDynamicProperties( name ) ) ).toInstance( dynamicProperty );
        }
        else if ( isInteger( value ) ) {
            DynamicIntProperty intProperty = DynamicPropertyFactory.getInstance()
                    .getIntProperty( name, Integer.valueOf( value ) );
            binder.bind( Key.get( DynamicIntProperty.class,
                    new NamedDynamicProperties( name ) ) ).toInstance( intProperty );
        }
        else {
            DynamicStringProperty dynamicProperty = DynamicPropertyFactory.getInstance()
                    .getStringProperty( name, value );
            binder.bind( Key.get( DynamicStringProperty.class,
                    new NamedDynamicProperties( name ) ) ).toInstance( dynamicProperty );
        }
    }


    private static boolean isBoolean( String value ) {
        return value.equalsIgnoreCase( "true" ) || value.equalsIgnoreCase( "false" );
    }


    private static boolean isInteger( String value ) {
        if ( value.contains( "." ) || value.contains( "E" ) || value.contains( "e" ) ||
                value.contains( "/" ) || value.endsWith( "F" ) || value.endsWith( "f" ) ||
                value.endsWith( "L" ) || value.endsWith( "l" ) ) {
            return false;
        }

        try {
            Integer.valueOf( value );
            return true;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }


    private static boolean isLong( String value ) {
        if ( value.contains( "." ) || value.contains( "E" ) || value.contains( "e" ) ||
                value.contains( "/" ) || value.endsWith( "F" ) || value.endsWith( "f" ) ||
                ! ( value.endsWith( "L" ) || value.endsWith( "l" ) ) ) {
            return false;
        }

        try {
            if ( value.endsWith( "L" ) || value.endsWith( "l" ) ) {
                Long.parseLong( value.substring( 0, value.length() - 1 ) );
            }
            else {
                Long.parseLong( value );
            }
            return true;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }


    private static boolean isFloat( String value ) {
        if ( ! ( value.endsWith( "F" ) || value.endsWith( "f" ) ) ) {
            return false;
        }

        try {
            if ( value.endsWith( "F" ) || value.endsWith( "f" ) ) {
                Float.parseFloat( value.substring( 0, value.length() - 1 ) );
            } else {
                Float.parseFloat( value );
            }
            return true;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }


    private static boolean isDouble( String value ) {
        if ( ! value.contains( "." ) || value.endsWith( "F" ) ||
                value.endsWith( "f" ) || value.endsWith( "L" ) ||
                value.endsWith( "l" ) ) {
            return false;
        }

        try {
            Double.parseDouble( value );
            return true;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }
}

