package org.apache.usergrid.persistence.model.field;


import java.util.LinkedHashSet;
import java.util.Set;


/** An object field that represents a set of objects */
public class SetField extends AbstractField<Set<Field>>
{


    /** Contructor that intializes with an empty set for adding to later */
    public SetField( String name )
    {
        super( name, new LinkedHashSet<Field>() );
    }

    public SetField() {

        }

    /** Add an entry to the set */
    public void addEntry( Field field )
    {
        value.add( field );
    }
}
