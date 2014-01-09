package org.apache.usergrid.persistence.model.field.value;


import java.io.Serializable;


/** Geographic point.  Should be used when we want to store geo information */
public class Location  implements Serializable
{

    private final double latitude;
    private final double longtitude;


    public Location( double latitude, double longtitude )
    {
        this.latitude = latitude;
        this.longtitude = longtitude;
    }


    public double getLatitude()
    {
        return latitude;
    }


    public double getLongtitude()
    {
        return longtitude;
    }
}
