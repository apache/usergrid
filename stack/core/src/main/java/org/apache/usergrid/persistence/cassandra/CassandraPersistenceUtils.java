/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.cassandra;


import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.thrift.ColumnDef;
import org.apache.cassandra.thrift.IndexType;
import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.utils.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

import me.prettyprint.cassandra.service.ThriftColumnDef;
import me.prettyprint.hector.api.ClockResolution;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.ddl.ColumnDefinition;
import me.prettyprint.hector.api.ddl.ColumnFamilyDefinition;
import me.prettyprint.hector.api.ddl.ComparatorType;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;
import me.prettyprint.hector.api.factory.HFactory;
import me.prettyprint.hector.api.mutation.MutationResult;
import me.prettyprint.hector.api.mutation.Mutator;

import static java.nio.ByteBuffer.wrap;

import static me.prettyprint.hector.api.factory.HFactory.createClockResolution;
import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static org.apache.commons.beanutils.MethodUtils.invokeStaticMethod;
import static org.apache.commons.lang.StringUtils.removeEnd;
import static org.apache.commons.lang.StringUtils.removeStart;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import static org.apache.usergrid.persistence.Schema.serializeEntityProperty;
import static org.apache.usergrid.persistence.cassandra.Serializers.be;
import static org.apache.usergrid.utils.ClassUtils.isBasicType;
import static org.apache.usergrid.utils.ConversionUtils.bytebuffer;
import static org.apache.usergrid.utils.JsonUtils.toJsonNode;
import static org.apache.usergrid.utils.StringUtils.replaceAll;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

/** @author edanuff */
public class CassandraPersistenceUtils {

    private static final Logger logger = LoggerFactory.getLogger( CassandraPersistenceUtils.class );

    /** Logger for batch operations */
    private static final Logger batch_logger =
            LoggerFactory.getLogger( CassandraPersistenceUtils.class.getPackage().getName() + ".BATCH" );

    /**
     *
     */
    public static final char KEY_DELIM = ':';

    /**
     *
     */
    public static final UUID NULL_ID = new UUID( 0, 0 );

    /**
     * @param operation
     * @param columnFamily
     * @param key
     * @param columnName
     * @param columnValue
     * @param timestamp
     */
    public static void logBatchOperation( String operation, Object columnFamily, Object key, Object columnName,
                                          Object columnValue, long timestamp ) {

        if ( batch_logger.isDebugEnabled() ) {
            batch_logger.debug( "{} cf={} key={} name={} value={}",
                    new Object[] { operation, columnFamily, key, columnName, columnValue } );
        }
    }


    public static void addInsertToMutator( Mutator<ByteBuffer> m, Object columnFamily, Object key, Object columnName,
                                           Object columnValue, long timestamp ) {

        logBatchOperation( "Insert", columnFamily, key, columnName, columnValue, timestamp );

        if ( columnName instanceof List<?> ) {
            columnName = DynamicComposite.toByteBuffer( ( List<?> ) columnName );
        }
        if ( columnValue instanceof List<?> ) {
            columnValue = DynamicComposite.toByteBuffer( ( List<?> ) columnValue );
        }

        HColumn<ByteBuffer, ByteBuffer> column =
                createColumn( bytebuffer( columnName ), bytebuffer( columnValue ), timestamp, be, be );
        m.addInsertion( bytebuffer( key ), columnFamily.toString(), column );
    }


    public static void addInsertToMutator( Mutator<ByteBuffer> m, Object columnFamily, Object key, Map<?, ?> columns,
                                           long timestamp ) throws Exception {

        for ( Entry<?, ?> entry : columns.entrySet() ) {
            addInsertToMutator( m, columnFamily, key, entry.getKey(), entry.getValue(), timestamp );
        }
    }




    public static void addDeleteToMutator( Mutator<ByteBuffer> m, Object columnFamily, Object key, Object columnName,
                                           long timestamp ) throws Exception {

        logBatchOperation( "Delete", columnFamily, key, columnName, null, timestamp );

        if ( columnName instanceof List<?> ) {
            columnName = DynamicComposite.toByteBuffer( ( List<?> ) columnName );
        }

        m.addDeletion( bytebuffer( key ), columnFamily.toString(), bytebuffer( columnName ), be, timestamp );
    }



    public static Map<String, ByteBuffer> getColumnMap( List<HColumn<String, ByteBuffer>> columns ) {
        Map<String, ByteBuffer> column_map = new TreeMap<String, ByteBuffer>( String.CASE_INSENSITIVE_ORDER );
        if ( columns != null ) {
            for ( HColumn<String, ByteBuffer> column : columns ) {
                String column_name = column.getName();
                column_map.put( column_name, column.getValue() );
            }
        }
        return column_map;
    }


    public static <K, V> Map<K, V> asMap( List<HColumn<K, V>> columns ) {
        if ( columns == null ) {
            return null;
        }
        Map<K, V> column_map = new LinkedHashMap<K, V>();
        for ( HColumn<K, V> column : columns ) {
            K column_name = column.getName();
            column_map.put( column_name, column.getValue() );
        }
        return column_map;
    }


    /** @return a composite key */
    public static Object key( Object... objects ) {
        if ( objects.length == 1 ) {
            Object obj = objects[0];
            if ( ( obj instanceof UUID ) || ( obj instanceof ByteBuffer ) ) {
                return obj;
            }
        }
        StringBuilder s = new StringBuilder();
        for ( Object obj : objects ) {
            if ( obj instanceof String ) {
                s.append( ( ( String ) obj ).toLowerCase() );
            }
            else if ( obj instanceof List<?> ) {
                s.append( key( ( ( List<?> ) obj ).toArray() ) );
            }
            else if ( obj instanceof Object[] ) {
                s.append( key( ( Object[] ) obj ) );
            }
            else if ( obj != null ) {
                s.append( obj );
            }
            else {
                s.append( "*" );
            }

            s.append( KEY_DELIM );
        }

        s.deleteCharAt( s.length() - 1 );

        return s.toString();
    }


    /** @return UUID for composite key */
    public static UUID keyID( Object... objects ) {
        if ( objects.length == 1 ) {
            Object obj = objects[0];
            if ( obj instanceof UUID ) {
                return ( UUID ) obj;
            }
        }
        String keyStr = key( objects ).toString();
        if ( keyStr.length() == 0 ) {
            return NULL_ID;
        }
        UUID uuid = UUID.nameUUIDFromBytes( keyStr.getBytes() ); //UUIDUtils.newTimeUUID(); //UUID.nameUUIDFromBytes( keyStr.getBytes() );
        logger.debug( "Key {} equals UUID {}", keyStr, uuid );
        return uuid;
    }

  //No longer does retries
    public static MutationResult batchExecute( Mutator<?> m, int retries ) {
        return m.execute();

    }


    public static Object toStorableValue( Object obj ) {
        if ( obj == null ) {
            return null;
        }

        if ( isBasicType( obj.getClass() ) ) {
            return obj;
        }

        if ( obj instanceof ByteBuffer ) {
            return obj;
        }

        JsonNode json = toJsonNode( obj );
        if ( ( json != null ) && json.isValueNode() ) {
            if ( json.isBigInteger() ) {
                return json.asInt();
            }
            else if ( json.isNumber() || json.isBoolean() ) {
                return BigInteger.valueOf( json.asLong() );
            }
            else if ( json.isTextual() ) {
                return json.asText();
            }
            else if ( json.isBinary() ) {
                try {
                    return wrap( json.binaryValue() );
                }
                catch ( IOException e ) {
                }
            }
        }

        return json;
    }



    public static ByteBuffer toStorableBinaryValue( Object obj, boolean forceJson ) {
        obj = toStorableValue( obj );
        if ( ( obj instanceof JsonNode ) || ( forceJson && ( obj != null ) && !( obj instanceof ByteBuffer ) ) ) {
            return JsonUtils.toByteBuffer( obj );
        }
        else {
            return bytebuffer( obj );
        }
    }


    public static List<ColumnDefinition> getIndexMetadata( String indexes ) {
        if ( indexes == null ) {
            return null;
        }
        String[] index_entries = split( indexes, ',' );
        List<ColumnDef> columns = new ArrayList<ColumnDef>();
        for ( String index_entry : index_entries ) {
            String column_name = stringOrSubstringBeforeFirst( index_entry, ':' ).trim();
            String comparer = substringAfterLast( index_entry, ":" ).trim();
            if ( StringUtils.isBlank( comparer ) ) {
                comparer = "UUIDType";
            }
            if ( StringUtils.isNotBlank( column_name ) ) {
                ColumnDef cd = new ColumnDef( bytebuffer( column_name ), comparer );
                cd.setIndex_name( column_name );
                cd.setIndex_type( IndexType.KEYS );
                columns.add( cd );
            }
        }
        return ThriftColumnDef.fromThriftList( columns );
    }


    public static List<ColumnFamilyDefinition> getCfDefs( Class<? extends CFEnum> cfEnum, String keyspace ) {
        return getCfDefs( cfEnum, null, keyspace );
    }


    public static List<ColumnFamilyDefinition> getCfDefs( Class<? extends CFEnum> cfEnum,
                                                          List<ColumnFamilyDefinition> cf_defs, String keyspace ) {

        if ( cf_defs == null ) {
            cf_defs = new ArrayList<ColumnFamilyDefinition>();
        }

        CFEnum[] values = null;
        try {
            values = ( CFEnum[] ) invokeStaticMethod( cfEnum, "values", null);
        }
        catch ( Exception e ) {
            logger.error( "Couldn't get CFEnum values", e );
        }
        if ( values == null ) {
            return null;
        }

        for ( CFEnum cf : values ) {
            if ( !cf.create() ) {
                continue;
            }
            String defaultValidationClass = cf.getValidator();
            List<ColumnDefinition> metadata = cf.getMetadata();

            ColumnFamilyDefinition cf_def = HFactory.createColumnFamilyDefinition( keyspace, cf.getColumnFamily(),
                    ComparatorType.getByClassName( cf.getComparator() ), metadata );

            if ( defaultValidationClass != null ) {
                cf_def.setDefaultValidationClass( defaultValidationClass );
            }

            cf_defs.add( cf_def );
        }

        return cf_defs;
    }


}
