package org.apache.usergrid.persistence.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.*;
import org.apache.usergrid.persistence.model.field.*;
import org.apache.usergrid.persistence.model.field.value.EntityObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Core persistence Entity Map structure to persist to
 */
public class EntityMap extends HashMap<String,Object> {
    private static EntityToMapConverter entityToMapConverter = new EntityToMapConverter();



    public EntityMap(){
        super();
    }


    public static Optional<EntityMap> fromEntity(Optional<Entity> entity) {
        if(entity.isPresent()){
            EntityMap map =  fromEntity(entity.get());
            return Optional.fromNullable(map);
        }else{
            return Optional.absent();
        }
    }

    public static EntityMap fromEntity(Entity entity) {
        EntityMap map =  entityToMapConverter.toMap(entity);
        return map;
    }

    /**
     * Return true if the value is a location field
     * @param fieldValue
     * @return
     */
    public static boolean isLocationField(Map<String, ?> fieldValue) {
        if (fieldValue.size() != 2) {
            return false;
        }

        return fieldValue.containsKey(EntityToMapConverter.LAT) && fieldValue.containsKey(EntityToMapConverter.LON);
    }

}
