package org.apache.usergrid.persistence.model.field;


import java.util.ArrayList;
import java.util.List;


/** A marker to signal array handling.  Just delegates to list field for easier handling internally */
public class ArrayField extends ListField
{

    /** Contructor that intializes with an empty set for adding to later */
    public ArrayField( String name )
    {
        super(name);
    }

    public ArrayField(){
        super();
    }

    /** Add the value to the list */
    public void add( Field field )
    {
        value.add( field );
    }
}
