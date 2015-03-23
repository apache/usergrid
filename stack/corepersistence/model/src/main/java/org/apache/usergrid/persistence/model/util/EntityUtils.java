package org.apache.usergrid.persistence.model.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.UUID;
import org.apache.usergrid.persistence.model.field.Field;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * @author tnine
 */
public class EntityUtils {


    private static final java.lang.reflect.Field VERSION = FieldUtils.getField( Entity.class, "version", true );

    private static final java.lang.reflect.Field ID = FieldUtils.getField( Entity.class, "id", true );


    /**
     * Set the version into the entity
     */
    public static void setVersion( Entity entity, UUID version ) {

        try {
            FieldUtils.writeField( VERSION, entity, version, true );
        }
        catch ( IllegalAccessException e ) {
            throw new RuntimeException( "Unable to set the field " + VERSION + " into the entity", e );
        }
    }


    /**
     * Set the id into the entity
     */
    public static void setId( Entity entity, Id id ) {
        try {
            FieldUtils.writeField( ID, entity, id, true );
        }
        catch ( IllegalAccessException e ) {
            throw new RuntimeException( "Unable to set the field " + ID + " into the entity", e );
        }
    }


    /**
     * Get all unique fields on an entity
     * @param entity
     * @return
     */
    public static List<Field> getUniqueFields( Entity entity ) {
        final Collection<Field> entityFields = entity.getFields();

        //preallocate to max possible for more efficient runtime
        final List<Field> possibleFields = new ArrayList<>( entityFields.size() );

        for ( Field field : entity.getFields() ) {
            if ( field.isUnique() ) {
                possibleFields.add( field );
            }
        }
        return possibleFields;
    }
}
