package org.apache.usergrid.persistence.model.field;


import java.util.ArrayList;
import java.util.List;


/** An object field that represents a list of objects.  This can also be used to represent arrays */
public class ListField extends AbstractField<List<Field>>
{

    /** Contructor that intializes with an empty set for adding to later */
    public ListField( String name )
    {
        super( name, new ArrayList<Field>() );
    }

    public ListField(){
        super();
    }


    /** Add the value to the list */
    public void add( Field field )
    {
        value.add( field );
    }
}
