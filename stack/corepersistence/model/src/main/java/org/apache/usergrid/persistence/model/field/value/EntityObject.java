package org.apache.usergrid.persistence.model.field.value;


import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.usergrid.persistence.model.field.Field;


/** Simple wrapper for holding nested objects */
public class EntityObject implements Serializable
{


    /** Fields the users can set */
    private Map<String, Field> fields = new HashMap<String, Field>();


    /** Add the field, return the old one if it existed */
    public <T extends java.lang.Object> Field<T> setField( Field<T> value )
    {
        return fields.put( value.getName(), value );
    }


    /** Get the field by name the user has set into the entity */
    public <T extends java.lang.Object> Field<T> getField( String name )
    {
        return fields.get( name );
    }


    /** Get all fields in the entity */
    public Collection<Field> getFields()
    {
        return fields.values();
    }
}
