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
package org.apache.usergrid.persistence;


import java.util.Comparator;

import static org.apache.usergrid.persistence.cassandra.IndexUpdate.compareIndexedValues;


public class EntityPropertyComparator implements Comparator<Entity> {

    final String propertyName;
    final int reverse;


    public EntityPropertyComparator( String propertyName, boolean reverse ) {
        this.propertyName = propertyName;
        this.reverse = reverse ? -1 : 1;
    }


    @Override
    public int compare( Entity e1, Entity e2 ) {

        if ( e1 == null ) {
            //second one is not null and first is, second is larger
            if ( e2 != null ) {
                return 1;
            }
            else {
                return 0;
            }
        }
        //first one is not null, second is
        else if ( e2 == null ) {
            return -1;
        }

        return compareIndexedValues( e1.getProperty( propertyName ), e2.getProperty( propertyName ) ) * reverse;
    }
}
