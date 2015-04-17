package org.apache.usergrid.persistence.model.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.Location;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

public class MapToEntityConverter{

    private ObjectMapper objectMapper = new ObjectMapper();

    public  Entity fromMap( Map<String, Object> map,  boolean topLevel ) {

        Entity entity ;
        if(map instanceof EntityMap){
            EntityMap entityMap = (EntityMap) map;
            Id id = entityMap.getId();
            UUID version = entityMap.getVersion();
            entityMap.clearFields();
            entity =  id!=null ? new Entity(id,version) : new Entity();
        }else{
            entity = new Entity();
        }

        return fromMap( entity, map, topLevel );
    }

    public Entity fromMap( Entity entity, Map<String, Object> map,  boolean topLevel ) {

        for ( String fieldName : map.keySet() ) {

            Object value = map.get(fieldName);

            if ( value instanceof String ) {
                entity.setField( new StringField( fieldName, (String)value, topLevel ));

            } else if ( value instanceof Boolean ) {
                entity.setField( new BooleanField( fieldName, (Boolean)value, topLevel ));

            } else if ( value instanceof Integer ) {
                entity.setField( new IntegerField( fieldName, (Integer)value, topLevel ));

            } else if ( value instanceof Double ) {
                entity.setField( new DoubleField( fieldName, (Double)value, topLevel ));

            } else if ( value instanceof Float ) {
                entity.setField( new FloatField( fieldName, (Float)value, topLevel ));

            } else if ( value instanceof Long ) {
                entity.setField( new LongField( fieldName, (Long)value, topLevel ));

            } else if ( value instanceof List) {
                entity.setField( listToListField( fieldName, (List)value ));

            } else if ( value instanceof UUID) {
                entity.setField( new UUIDField( fieldName, (UUID)value, topLevel ));

            } else if ( value instanceof Map ) {
                processMapValue( value, fieldName, entity);

            } else if ( value instanceof Enum ) {
                entity.setField( new StringField( fieldName, value.toString(), topLevel ));

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
            return processListForField( list ); // recursion

        } else {
            return list;
        }
    }


    private  void processMapValue(
            Object value, String fieldName, Entity entity) {

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
                    fromMap((Map<String, Object>) value, false))); // recursion
        }
    }
}
