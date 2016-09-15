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


    public Entity fromMap(final Entity entity,final  Map<String, Object> map,final
        SchemaManager schemaManager, final String entityType, boolean topLevel) {

        for ( String fieldName : map.keySet() ) {

            Object value = map.get( fieldName );
            boolean unique = schemaManager == null ? topLevel :  topLevel && schemaManager.isPropertyUnique(entityType, fieldName);
            entity.setField( processField( fieldName, value, unique));

        }

        return entity;
    }


    private  ListField processListValue(String fieldName, List list ) {

        if (list.isEmpty()) {
            return new ArrayField( fieldName );
        }

        final List<Object> returnList = new ArrayList<>();


        list.forEach( sample -> {

            if ( sample instanceof Map ) {

                returnList.add( fromMap( (Map<String, Object>) sample, false ) );

            } else if ( sample instanceof List ) {

                returnList.add( processListForListField( fieldName, (List) sample ) );

            } else {

                returnList.add( sample );

            }

        });


        return new ArrayField<>( fieldName, returnList);

    }



    private  List processListForListField(String fieldName, List list ) {
        if ( list.isEmpty() ) {
            return list;
        }

        List<Object> newList = new ArrayList<>();

        list.forEach( sample -> {

            if ( sample instanceof Map ) {

                newList.add( processMapValue( sample, fieldName) );

            } else if ( sample instanceof List ) {

                for (Object o : list) {

                    newList.add(o);

                }

            } else {

                newList.add( sample );

            }

        });

        return newList;

    }


    private Field processMapValue( Object value, String fieldName) {

        // check to see if the map is truly a location object
        if ( locationKey.equalsIgnoreCase(fieldName) ) {

            return processLocationField((Map<String, Object>) value, fieldName);

        } else {

            // not a location element, process it as a normal map
            return processMapField( value, fieldName);
        }
    }


    private Field processMapField ( Object value, String fieldName) {

        return new EntityObjectField( fieldName, fromMap( (Map<String, Object>)value, false));
    }


    /**
     * for location we need to parse two formats potentially and convert to a typed field
     */
    private Field processLocationField(Map<String, Object> value, String fieldName) {
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

                if(logger.isDebugEnabled()){
                    logger.debug("Entity contains latitude and longitude in old format location{lat,long}");
                }

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

           return new LocationField(fieldName, new Location(latVal, lonVal));
        } else {

            if(logger.isDebugEnabled()){
                logger.debug(
                    "entity cannot process location values that don't have valid " +
                        "location{latitude,longitude} values, changing to generic object");
            }

           return new EntityObjectField(fieldName,fromMap( value, false)); // recursion
        }
    }



    private Field processField(final String fieldName, final Object value, final boolean unique) {

        Field processedField;

        if ( value instanceof String ) {

            String stringValue =(String)value;
            processedField = new StringField(fieldName, stringValue, unique);

        } else if ( value instanceof Boolean ) {

            processedField = new BooleanField( fieldName, (Boolean)value, unique  );

        } else if ( value instanceof Integer ) {

            processedField = new IntegerField( fieldName, (Integer)value, unique );

        } else if ( value instanceof Double ) {

            processedField = new DoubleField( fieldName, (Double)value, unique  );

        } else if ( value instanceof Float ) {

            processedField = new FloatField( fieldName, (Float)value, unique );

        } else if ( value instanceof Long ) {

            processedField = new LongField( fieldName, (Long)value, unique );

        } else if ( value instanceof List) {

            processedField = processListValue( fieldName, (List)value );

        } else if ( value instanceof UUID) {

            processedField = new UUIDField( fieldName, (UUID)value, unique );

        } else if ( value instanceof Map ) {

            processedField = processMapValue( value, fieldName);

        } else if ( value instanceof Enum ) {

            processedField = new StringField( fieldName, value.toString(), unique );

        } else if ( value == null ){

            // not supported from outside API yet, but let's keep it in serialization it's a handled in this logic
            processedField = new NullField( fieldName, unique );

        } else {

            byte[] valueSerialized;
            try {

                valueSerialized = objectMapper.writeValueAsBytes( value );

            }
            catch ( JsonProcessingException e ) {

                throw new RuntimeException( "Can't serialize object ",e );

            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(valueSerialized);
            processedField = new ByteArrayField( fieldName, byteBuffer.array(), value.getClass() );

        }

        return processedField;

    }


}
