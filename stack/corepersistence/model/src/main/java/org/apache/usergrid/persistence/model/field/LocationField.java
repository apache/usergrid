package org.apache.usergrid.persistence.model.field;


import org.apache.usergrid.persistence.model.field.value.Location;


/** Basic field for storing location data */
public class LocationField extends AbstractField<Location>
{

    /** Create a location field with the given point */
    public LocationField( String name, Location value )
    {
        super( name, value );
    }

    public LocationField() {

        }
}
