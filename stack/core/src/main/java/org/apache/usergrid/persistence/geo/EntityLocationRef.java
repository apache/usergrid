/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.persistence.geo;


import java.util.UUID;

import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.geo.model.Point;
import org.apache.usergrid.utils.UUIDUtils;

import me.prettyprint.hector.api.beans.DynamicComposite;

import static org.apache.commons.lang.math.NumberUtils.toDouble;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringAfterLast;
import static org.apache.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;


public class EntityLocationRef implements EntityRef {

    private UUID uuid;

    private String type;

    private UUID timestampUuid = UUIDUtils.newTimeUUID();

    private double latitude;

    private double longitude;

    private double distance;


    public EntityLocationRef() {
    }


    public EntityLocationRef( EntityRef entity, double latitude, double longitude ) {
        this( entity.getType(), entity.getUuid(), latitude, longitude );
    }


    public EntityLocationRef( String type, UUID uuid, double latitude, double longitude ) {
        this.type = type;
        this.uuid = uuid;
        this.latitude = latitude;
        this.longitude = longitude;
    }


    public EntityLocationRef( EntityRef entity, UUID timestampUuid, double latitude, double longitude ) {
        this( entity.getType(), entity.getUuid(), timestampUuid, latitude, longitude );
    }


    public EntityLocationRef( String type, UUID uuid, UUID timestampUuid, double latitude, double longitude ) {
        this.type = type;
        this.uuid = uuid;
        this.timestampUuid = timestampUuid;
        this.latitude = latitude;
        this.longitude = longitude;
    }


    public EntityLocationRef( EntityRef entity, UUID timestampUuid, String coord ) {
        this.type = entity.getType();
        this.uuid = entity.getUuid();
        this.timestampUuid = timestampUuid;
        this.latitude = toDouble( stringOrSubstringBeforeFirst( coord, ',' ) );
        this.longitude = toDouble( stringOrSubstringAfterLast( coord, ',' ) );
    }


    @Override
    public UUID getUuid() {
        return uuid;
    }


    public void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }


    @Override
    public String getType() {
        return type;
    }


    public void setType( String type ) {
        this.type = type;
    }


    public UUID getTimestampUuid() {
        return timestampUuid;
    }


    public void setTimestampUuid( UUID timestampUuid ) {
        this.timestampUuid = timestampUuid;
    }


    public double getLatitude() {
        return latitude;
    }


    public void setLatitude( double latitude ) {
        this.latitude = latitude;
    }


    public double getLongitude() {
        return longitude;
    }


    public void setLongitude( double longitude ) {
        this.longitude = longitude;
    }


    public Point getPoint() {
        return new Point( latitude, longitude );
    }


    public DynamicComposite getColumnName() {
        return new DynamicComposite( uuid, type, timestampUuid );
    }


    public DynamicComposite getColumnValue() {
        return new DynamicComposite( latitude, longitude );
    }


    public long getTimestampInMicros() {
        return UUIDUtils.getTimestampInMicros( timestampUuid );
    }


    public long getTimestampInMillis() {
        return UUIDUtils.getTimestampInMillis( timestampUuid );
    }


    public double getDistance() {
        return distance;
    }


    /** Calculate, set and return the distance from this location to the point specified */
    public double calcDistance( Point point ) {
        distance = GeocellUtils.distance( getPoint(), point );
        return distance;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( type == null ) ? 0 : type.hashCode() );
        result = prime * result + ( ( uuid == null ) ? 0 : uuid.hashCode() );
        return result;
    }


    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        EntityLocationRef other = ( EntityLocationRef ) obj;
        if ( type == null ) {
            if ( other.type != null ) {
                return false;
            }
        }
        else if ( !type.equals( other.type ) ) {
            return false;
        }
        if ( uuid == null ) {
            if ( other.uuid != null ) {
                return false;
            }
        }
        else if ( !uuid.equals( other.uuid ) ) {
            return false;
        }
        return true;
    }
}
