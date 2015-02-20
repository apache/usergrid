/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */


package org.apache.usergrid.corepersistence.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.SimpleId;
import org.apache.usergrid.persistence.model.field.ArrayField;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.ByteArrayField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.SetField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.UUIDField;
import org.apache.usergrid.persistence.model.field.value.EntityObject;
import org.apache.usergrid.persistence.model.field.value.Location;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Utilities for converting entities to/from maps suitable for Core Persistence.
 * Aware of unique properties via Schema.
 */
public class CpEntityMapUtils {
    private static final Logger logger = LoggerFactory.getLogger( CpEntityMapUtils.class );

    public static ObjectMapper objectMapper = new ObjectMapper(  );


    /**
     * Convert a usergrid 1.0 entity into a usergrid 2.0 entity
     */
    public static org.apache.usergrid.persistence.model.entity.Entity entityToCpEntity(
        org.apache.usergrid.persistence.Entity entity, UUID importId ) {

        UUID uuid = importId != null ? importId : entity.getUuid();

        org.apache.usergrid.persistence.model.entity.Entity cpEntity =
            new org.apache.usergrid.persistence.model.entity.Entity( new SimpleId( uuid, entity.getType() ) );

        cpEntity = CpEntityMapUtils.fromMap( cpEntity, entity.getProperties(), entity.getType(), true );

        cpEntity = CpEntityMapUtils.fromMap( cpEntity, entity.getDynamicProperties(), entity.getType(), true );

        return cpEntity;
    }


    public static Entity fromMap( Map<String, Object> map, String entityType, boolean topLevel ) {
        return fromMap( null, map, entityType, topLevel );
    }

    public static Entity fromMap(
            Entity entity, Map<String, Object> map, String entityType, boolean topLevel ) {

        if ( entity == null ) {
            entity = new Entity();
        }

        for ( String fieldName : map.keySet() ) {

            Object value = map.get( fieldName );
            boolean unique = Schema.getDefaultSchema().isPropertyUnique(entityType, fieldName);

//            if ( unique ) {
//                logger.debug("{} is a unique property", fieldName );
//            }

            if ( value instanceof String ) {
                entity.setField( new StringField( fieldName, (String)value, unique && topLevel ));

            } else if ( value instanceof Boolean ) {
                entity.setField( new BooleanField( fieldName, (Boolean)value, unique && topLevel ));

            } else if ( value instanceof Integer ) {
                entity.setField( new IntegerField( fieldName, (Integer)value, unique && topLevel ));

            } else if ( value instanceof Double ) {
                entity.setField( new DoubleField( fieldName, (Double)value, unique && topLevel ));

		    } else if ( value instanceof Float ) {
                entity.setField( new FloatField( fieldName, (Float)value, unique && topLevel ));

            } else if ( value instanceof Long ) {
                entity.setField( new LongField( fieldName, (Long)value, unique && topLevel ));

            } else if ( value instanceof List) {
                entity.setField( listToListField( fieldName, (List)value, entityType ));

            } else if ( value instanceof UUID) {
                entity.setField( new UUIDField( fieldName, (UUID)value, unique && topLevel ));

            } else if ( value instanceof Map ) {
                processMapValue( value, fieldName, entity, entityType);

            } else if ( value instanceof Enum ) {
                entity.setField( new StringField( fieldName, value.toString(), unique && topLevel ));

			} else if ( value != null ) {
                byte[] valueSerialized;
                try {
                    valueSerialized = objectMapper.writeValueAsBytes( value );
                }
                catch ( JsonProcessingException e ) {
                    throw new RuntimeException( "Can't serialize object ",e );
                }
                catch ( IOException e ) {
                    throw new RuntimeException( "Can't serialize object ",e );
                }
                ByteBuffer byteBuffer = ByteBuffer.wrap( valueSerialized );
                ByteArrayField bf = new ByteArrayField( fieldName, byteBuffer.array(), value.getClass() );
                entity.setField( bf );
            }
        }

        return entity;
    }

    private static void processMapValue(
            Object value, String fieldName, Entity entity, String entityType) {

        // is the map really a location element?
        if ("location" .equals(fieldName.toString().toLowerCase()) ) {

            // get the object to inspect
            Map<String, Object> origMap = (Map<String, Object>) value;
            Map<String, Object> m = new HashMap<String, Object>();

            // Tests expect us to treat "Longitude" the same as "longitude"
            for ( String key : origMap.keySet() ) {
                m.put( key.toLowerCase(), origMap.get(key) );
            }

            // Expect at least two fields in a Location object
            if (m.size() >= 2) {

                Double lat = null;
                Double lon = null;

                // check the properties to make sure they are set and are doubles
                if (m.get("latitude") != null && m.get("longitude") != null) {
                    try {
                        lat = Double.parseDouble(m.get("latitude").toString());
                        lon = Double.parseDouble(m.get("longitude").toString());

                    } catch (NumberFormatException ignored) {
                        throw new IllegalArgumentException(
                                "Latitude and longitude must be doubles (e.g. 32.1234).");
                    }
                } else if (m.get("lat") != null && m.get("lon") != null) {
                    try {
                        lat = Double.parseDouble(m.get("lat").toString());
                        lon = Double.parseDouble(m.get("lon").toString());
                    } catch (NumberFormatException ignored) {
                        throw new IllegalArgumentException(""
                                + "Latitude and longitude must be doubles (e.g. 32.1234).");
                    }
                } else {
                    throw new IllegalArgumentException("Location properties require two fields - "
                            + "latitude and longitude, or lat and lon");
                }

                if (lat != null && lon != null) {
                    entity.setField( new LocationField(fieldName, new Location(lat, lon)));
                } else {
                    throw new IllegalArgumentException( "Unable to parse location field properties "
                            + "- make sure they conform - lat and lon, and should be doubles.");
                }
            } else {
                throw new IllegalArgumentException("Location properties requires two fields - "
                        + "latitude and longitude, or lat and lon.");
            }
        } else {
            // not a location element, process it as map
            entity.setField(new EntityObjectField(fieldName,
                    fromMap((Map<String, Object>) value, entityType, false))); // recursion
        }
    }


    private static ListField listToListField( String fieldName, List list, String entityType ) {

        if (list.isEmpty()) {
            return new ArrayField( fieldName );
        }

        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            return new ArrayField<Entity>( fieldName, processListForField( list, entityType ));

        } else if ( sample instanceof List ) {
            return new ArrayField<List>( fieldName, processListForField( list, entityType ));
        } else if ( sample instanceof String ) {
            return new ArrayField<String>( fieldName, (List<String>)list );

        } else if ( sample instanceof Boolean ) {
            return new ArrayField<Boolean>( fieldName, (List<Boolean>)list );

        } else if ( sample instanceof Integer ) {
            return new ArrayField<Integer>( fieldName, (List<Integer>)list );

        } else if ( sample instanceof Double ) {
            return new ArrayField<Double>( fieldName, (List<Double>)list );

        } else if ( sample instanceof Long ) {
            return new ArrayField<Long>( fieldName, (List<Long>)list );

        } else {
            throw new RuntimeException("Unknown type " + sample.getClass().getName());
        }
    }


    private static List processListForField( List list, String entityType ) {
        if ( list.isEmpty() ) {
            return list;
        }
        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            List<Entity> newList = new ArrayList<Entity>();
            for ( Map<String, Object> map : (List<Map<String, Object>>)list ) {
                newList.add( fromMap( map, entityType, false ) );
            }
            return newList;

        } else if ( sample instanceof List ) {
            return processListForField( list, entityType ); // recursion

        } else {
            return list;
        }
    }


    /**
     * Convert Entity to Map, adding version_ug_field and a {name}_ug_analyzed field for each
     * StringField.
     */
    public static Map toMap(EntityObject entity) {

        Map<String, Object> entityMap = new TreeMap<>();

        for (Object f : entity.getFields().toArray()) {
            Field field = (Field) f;

            if (f instanceof ListField || f instanceof ArrayField) {
                List list = (List) field.getValue();
                entityMap.put(field.getName(),
                        new ArrayList( processCollectionForMap(list)));

            } else if (f instanceof SetField) {
                Set set = (Set) field.getValue();
                entityMap.put(field.getName(),
                        new ArrayList( processCollectionForMap(set)));

            } else if (f instanceof EntityObjectField) {
                EntityObject eo = (EntityObject) field.getValue();
                entityMap.put( field.getName(), toMap(eo)); // recursion

            } else if (f instanceof StringField) {
                entityMap.put(field.getName(), ((String) field.getValue()));

            } else if (f instanceof LocationField) {
                LocationField locField = (LocationField) f;
                Map<String, Object> locMap = new HashMap<String, Object>();

                // field names lat and lon trigger ElasticSearch geo location
                locMap.put("lat", locField.getValue().getLatitude());
                locMap.put("lon", locField.getValue().getLongitude());
                entityMap.put( field.getName(), field.getValue());

            } else if (f instanceof ByteArrayField) {
                    ByteArrayField bf = ( ByteArrayField ) f;

                    byte[] serilizedObj =  bf.getValue();
                    Object o;
                    try {
                        o = objectMapper.readValue( serilizedObj, bf.getClassinfo() );
                    }
                    catch ( IOException e ) {
                        throw new RuntimeException( "Can't deserialize object ",e );
                    }
                    entityMap.put( bf.getName(), o );
            }
            else {
                entityMap.put( field.getName(), field.getValue());
            }
        }

        return entityMap;
    }


    private static Collection processCollectionForMap(Collection c) {
        if (c.isEmpty()) {
            return c;
        }
        List processed = new ArrayList();
        Object sample = c.iterator().next();

        if (sample instanceof Entity) {
            for (Object o : c.toArray()) {
                Entity e = (Entity) o;
                processed.add(toMap(e));
            }

        } else if (sample instanceof List) {
            for (Object o : c.toArray()) {
                List list = (List) o;
                processed.add(processCollectionForMap(list)); // recursion;
            }

        } else if (sample instanceof Set) {
            for (Object o : c.toArray()) {
                Set set = (Set) o;
                processed.add(processCollectionForMap(set)); // recursion;
            }

        } else {
            for (Object o : c.toArray()) {
                processed.add(o);
            }
        }
        return processed;
    }

}
