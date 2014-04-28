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


package org.apache.usergrid.persistence.collection.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.BooleanField;
import org.apache.usergrid.persistence.model.field.DoubleField;
import org.apache.usergrid.persistence.model.field.EntityObjectField;
import org.apache.usergrid.persistence.model.field.Field;
import org.apache.usergrid.persistence.model.field.FloatField;
import org.apache.usergrid.persistence.model.field.IntegerField;
import org.apache.usergrid.persistence.model.field.ListField;
import org.apache.usergrid.persistence.model.field.LocationField;
import org.apache.usergrid.persistence.model.field.LongField;
import org.apache.usergrid.persistence.model.field.StringField;
import org.apache.usergrid.persistence.model.field.value.Location;


public class EntityBuilder {

    public static Entity fromMap( String scope, Map<String, Object> item ) {
        return fromMap( scope, null, item );
    }

    public static Entity fromMap( String scope, Entity entity, Map<String, Object> map ) {

        if ( entity == null ) {
            entity = new Entity();
        }

        for ( String fieldName : map.keySet() ) {

            Object value = map.get( fieldName );

            if ( value instanceof String ) {
                entity.setField( new StringField( fieldName, (String)value ));

            } else if ( value instanceof Boolean ) {
                entity.setField( new BooleanField( fieldName, (Boolean)value ));
                        
            } else if ( value instanceof Integer ) {
                entity.setField( new IntegerField( fieldName, (Integer)value ));

            } else if ( value instanceof Double ) {
                entity.setField( new DoubleField( fieldName, (Double)value ));

		    } else if ( value instanceof Float ) {
                entity.setField( new FloatField( fieldName, (Float)value ));
				
            } else if ( value instanceof Long ) {
                entity.setField( new LongField( fieldName, (Long)value ));

            } else if ( value instanceof List) {
                entity.setField( listToListField( scope, fieldName, (List)value ));

            } else if ( value instanceof Map ) {

				Field field = null;

				// is the map really a location element?
				Map<String, Object> m = (Map<String, Object>)value;
				if ( m.size() == 2) {
					Double lat = null;
					Double lon = null;
					try {
						if ( m.get("latitude") != null && m.get("longitude") != null ) {
							lat = Double.parseDouble( m.get("latitude").toString() );
							lon = Double.parseDouble( m.get("longitude").toString() );

						} else if ( m.get("lat") != null && m.get("lon") != null ) { 
							lat = Double.parseDouble( m.get("lat").toString() );
							lon = Double.parseDouble( m.get("lon").toString() );
						}
					} catch ( NumberFormatException ignored ) {}

					if ( lat != null && lon != null ) {
						field = new LocationField( fieldName, new Location( lat, lon ));
					}
				}

				if ( field == null ) { 

					// not a location element, process it as map
					entity.setField( new EntityObjectField( fieldName, 
						fromMap( scope, (Map<String, Object>)value ))); // recursion

				} else {
					entity.setField( field );
				}
	
			} else {
                throw new RuntimeException("Unknown type " + value.getClass().getName());
            }
        }

        return entity;
    }

    
    private static ListField listToListField( String scope, String fieldName, List list ) {

        if (list.isEmpty()) {
            return new ListField( fieldName );
        }

        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            return new ListField<Entity>( fieldName, processListForField( scope, list ));

        } else if ( sample instanceof List ) {
            return new ListField<List>( fieldName, processListForField( scope, list ));
            
        } else if ( sample instanceof String ) {
            return new ListField<String>( fieldName, (List<String>)list );
                    
        } else if ( sample instanceof Boolean ) {
            return new ListField<Boolean>( fieldName, (List<Boolean>)list );
                    
        } else if ( sample instanceof Integer ) {
            return new ListField<Integer>( fieldName, (List<Integer>)list );

        } else if ( sample instanceof Double ) {
            return new ListField<Double>( fieldName, (List<Double>)list );

        } else if ( sample instanceof Long ) {
            return new ListField<Long>( fieldName, (List<Long>)list );

        } else {
            throw new RuntimeException("Unknown type " + sample.getClass().getName());
        }
    }

    
    private static List processListForField( String scope, List list ) {
        if ( list.isEmpty() ) {
            return list;
        }
        Object sample = list.get(0);

        if ( sample instanceof Map ) {
            List<Entity> newList = new ArrayList<Entity>();
            for ( Map<String, Object> map : (List<Map<String, Object>>)list ) {
                newList.add( fromMap( scope, map ) );
            }
            return newList;

        } else if ( sample instanceof List ) {
            return processListForField( scope, list ); // recursion
            
        } else { 
            return list;
        } 
    }


}
