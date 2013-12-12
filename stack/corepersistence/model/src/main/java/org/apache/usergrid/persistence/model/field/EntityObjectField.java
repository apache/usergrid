package org.apache.usergrid.persistence.model.field;


import org.apache.usergrid.persistence.model.field.value.EntityObject;


/** An object field */
public class EntityObjectField extends AbstractField<EntityObject>
{


    public EntityObjectField( String name, EntityObject value )
    {
        super( name, value );
    }

    public EntityObjectField() {

        }
}
