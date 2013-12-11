package org.apache.usergrid.persistence.collection.util;


import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.lang3.reflect.FieldUtils;

import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


/**
 * @author tnine
 */
public class EntityUtils {


    private static final Field VERSION = FieldUtils.getField( Entity.class, "version", true );

    private static final Field ID = FieldUtils.getField( Entity.class, "id", true );


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
}
