package org.apache.usergrid.persistence.model.field;


import java.util.UUID;


/** A String field */
public class UUIDField extends AbstractField<UUID>
{


    public UUIDField( String name, UUID value )
    {
        super( name, value );
    }

    public UUIDField() {

        }
}
