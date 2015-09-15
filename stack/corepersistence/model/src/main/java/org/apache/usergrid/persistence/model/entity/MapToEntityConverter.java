/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence.model.entity;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.apache.usergrid.persistence.model.collection.SchemaManager;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;

public class MapToEntityConverter{
    public static final Logger logger = LoggerFactory.getLogger(MapToEntityConverter.class);

    private final JsonFactory jsonFactory = new JsonFactory();
    private final ObjectMapper objectMapper = new ObjectMapper(jsonFactory).registerModule(new GuavaModule());

    static final String locationKey = "location";
    static final String lat = "latitude";
    static final String lon = "longitude";

    public  Entity fromMap( Map<String, Object> map,  boolean topLevel ) {

        Entity  entity = new Entity();
        return fromMap( entity, map, null, null, topLevel );
    }

    public Entity fromMap(final Entity entity,final  Map<String, Object> map,final  SchemaManager schemaManager, final String entityType, boolean topLevel) {

        for ( String fieldName : map.keySet() ) {
            if(fieldName.equals("size")){
                continue;
            }
            Object value = map.get( fieldName );
            boolean unique = schemaManager == null ? topLevel :  topLevel && schemaManager.isPropertyUnique(entityType, fieldName);
            //cannot store fields that aren't locations

            if ( value instanceof String ) {
                String stringValue =(String)value;
                entity.setField(new StringField(fieldName, stringValue, unique));

            } else if ( value instanceof Boolean ) {
                entity.setField( new BooleanField( fieldName, (Boolean)value, unique  ));

            } else if ( value instanceof Integer ) {
                entity.setField( new IntegerField( fieldName, (Integer)value, unique ));

            } else if ( value instanceof Double ) {
                entity.setField( new DoubleField( fieldName, (Double)value, unique  ));

            } else if ( value instanceof Float ) {
                entity.setField( new FloatField( fieldName, (Float)value, unique ));

            } else if ( value instanceof Long ) {

                entity.setField( new LongField( fieldName, (Long)value, unique ));

            } else if ( value instanceof List) {
                entity.setField( listToListField( fieldName, (List)value ));

            } else if ( value instanceof UUID) {
                entity.setField( new UUIDField( fieldName, (UUID)value, unique ));

            } else if ( value instanceof Map ) {
                processMapValue( value, fieldName, entity);

            } else if ( value instanceof Enum ) {
                entity.setField( new StringField( fieldName, value.toString(), unique ));

            } else if ( value != null ) {
                byte[] valueSerialized;
                try {
                    valueSerialized = objectMapper.writeValueAsBytes( value );
                }
                catch ( JsonProcessingException e ) {
                    throw new RuntimeException( "Can't serialize object ",e );
                }

                ByteBuffer byteBuffer = ByteBuffer.wrap(valueSerialized);
                ByteArrayField bf = new ByteArrayField( fieldName, byteBuffer.array(), value.getClass() );
                entity.setField( bf );
            }
        }


        return entity;
    }


    private  ListField listToListField( String fieldName, List list ) {

        if (list.isEmpty()) {
            return new ArrayField( fieldName );
        }

        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            return new ArrayField( fieldName, processListForField( list ));

        } else if ( sample instanceof List ) {
            return new ArrayField<List>( fieldName, processListForField( list ));

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
    private  List processListForField( List list ) {
        if ( list.isEmpty() ) {
            return list;
        }
        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            List<Entity> newList = new ArrayList<Entity>();
            for ( Map<String, Object> map : (List<Map<String, Object>>)list ) {
                newList.add( fromMap( map, false ) );
            }
            return newList;

        } else if ( sample instanceof List ) {
            List<Object> newList = new ArrayList<Object>();
            for (Object o : list) {
                if (o instanceof List) {
                    newList.add(processListForField((List) o));
                } else {
                    newList.add(o);
                }
            }
            return newList;

        } else {
            return list;
        }
    }


    private  void processMapValue( Object value, String fieldName, Entity entity) {

        // is the map really a location element?
        if ("location" .equals(fieldName.toString().toLowerCase()) ) {
            processLocationField((Map<String, Object>) value, fieldName, entity);
        } else {
            // not a location element, process it as map
            entity.setField(new EntityObjectField(fieldName,
                    fromMap((Map<String, Object>) value, false))); // recursion
        }
    }

    /**
     * for location we need to parse two formats potentially and convert to a typed field
     */
    private void processLocationField(Map<String, Object> value, String fieldName, Entity entity) {
        // get the object to inspect
        Map<String, Object> origMap = value;
        Map<String, Object> m = new HashMap<String, Object>();

        // Tests expect us to treat "Longitude" the same as "longitude"
        for (String key : origMap.keySet()) {
            m.put(key.toLowerCase(), origMap.get(key));
        }

        // Expect at least two fields in a Location object and must have lat lon
        if (m.size() >= 2 && (
            (m.containsKey(lat)  && m.containsKey(lon) )
                || (m.containsKey("lat") && m.containsKey("lon") )
        )) {

            Double latVal, lonVal;

            // check the properties to make sure they are set and are doubles
            if (m.containsKey(lat)  && m.containsKey(lon)) {
                try {
                    latVal = Double.parseDouble(m.get(lat).toString());
                    lonVal = Double.parseDouble(m.get(lon).toString());

                } catch (NumberFormatException ignored) {
                    throw new IllegalArgumentException(
                        "Latitude and longitude must be doubles (e.g. 32.1234).");
                }
            } else if (m.containsKey("lat") && m.containsKey("lon")) {
                logger.warn("Entity contains latitude and longitude in old format location{lat,long}"
                );
                try {
                    latVal = Double.parseDouble(m.get("lat").toString());
                    lonVal = Double.parseDouble(m.get("lon").toString());
                } catch (NumberFormatException ignored) {
                    throw new IllegalArgumentException(""
                        + "Latitude and longitude must be doubles (e.g. 32.1234).");
                }
            } else {
                throw new IllegalArgumentException("Location properties require two fields - "
                    + "latitude and longitude, or lat and lon");
            }

            entity.setField(new LocationField(fieldName, new Location(latVal, lonVal)));
        } else {
            //can't process non enties
            logger.warn(
                "entity cannot process location values that don't have valid location{latitude,longitude} values, changing to generic object"
            );
            entity.setField(new EntityObjectField(fieldName,fromMap( value, false))); // recursion
        }
    }
}
