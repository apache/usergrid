package org.apache.usergrid.persistence.model.entity;

import com.google.common.base.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;


/**
 * Core persistence Entity Map structure to persist to
 */
public class EntityMap extends HashMap<String,Object> {

    private static final Logger logger = LoggerFactory.getLogger( EntityMap.class );

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
     * Return true if the value is a location field which contains valid values for latitude and longitude
     * @param fieldValue
     * @return
     */
    public static boolean isLocationField(Map<String, ?> fieldValue) {

        //short circuit since valid location objects contain only 2 properties (latitude and longitude)
        if (fieldValue.size() != 2) {
            return false;
        }

        // we need to make sure that latitude and longitude are numbers or strings that can be parsed as a number
        if (fieldValue.containsKey(EntityToMapConverter.LAT) && fieldValue.containsKey(EntityToMapConverter.LON)){

            for(Map.Entry<String,?> value : fieldValue.entrySet()){

                if(!(value.getValue() instanceof Number) && !isDouble(String.valueOf(value.getValue()))){

                    if(logger.isDebugEnabled()){
                        logger.debug("Field [{}] with value [{}] is not a valid geo coordinate",
                            value.getKey(),
                            value.getValue()
                        );
                    }
                    return false;
                }
            }

            return true;
        }

        return false;

    }

    public static boolean isDouble(String s){

        try{
            Double.valueOf(s);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }

}
