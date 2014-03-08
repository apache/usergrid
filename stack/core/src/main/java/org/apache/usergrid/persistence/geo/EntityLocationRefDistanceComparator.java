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


import java.util.Comparator;

import org.apache.usergrid.utils.UUIDUtils;


/**
 * Compares 2 entity location refs by distance.  The one with the larger distance is considered greater than one with a
 * smaller distance.  If the distances are the same they time uuids are compared based on the UUIDUtils.compare for time
 * uuids.  The one with a larger time is considered greater
 *
 * @author tnine
 */
public class EntityLocationRefDistanceComparator implements Comparator<EntityLocationRef> {

    /**
     *
     */
    public EntityLocationRefDistanceComparator() {
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    @Override
    public int compare( EntityLocationRef o1, EntityLocationRef o2 ) {

        if ( o1 == null ) {

            //second is not null
            if ( o2 != null ) {
                return 1;
            }
            //both null
            return 0;
        }
        //second is null, first isn't
        else if ( o2 == null ) {
            return -1;
        }

        double o1Distance = o1.getDistance();
        double o2Distance = o2.getDistance();


        int doubleCompare = Double.compare( o1Distance, o2Distance );


        //    int doubleCompare = Double.compare(o1.getDistance(), o2.getDistance());

        if ( doubleCompare != 0 ) {
            return doubleCompare;
        }

        return UUIDUtils.compare( o1.getUuid(), o2.getUuid() );
    }
}
