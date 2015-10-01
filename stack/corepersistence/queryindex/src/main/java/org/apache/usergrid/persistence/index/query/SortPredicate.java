/*
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.apache.usergrid.persistence.index.query;


import java.io.Serializable;

import org.elasticsearch.search.sort.SortOrder;


/**
 * An object that represents a sort predicate
 */
public final class SortPredicate implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String propertyName;
    private final SortDirection direction;


    public SortPredicate( String propertyName, SortDirection direction ) {

        if ( propertyName == null ) {
            throw new NullPointerException( "Property name was null" );
        }

        if ( direction == null ) {
            direction = SortDirection.ASCENDING;
        }

        this.propertyName = propertyName.trim();
        this.direction = direction;
    }


    public SortPredicate( String propertyName, String direction ) {
        this( propertyName, SortDirection.find( direction ) );
    }


    public String getPropertyName() {
        return propertyName;
    }


    public SortDirection getDirection() {
        return direction;
    }


    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( ( o == null ) || ( super.getClass() != o.getClass() ) ) {
            return false;
        }

        SortPredicate that = ( SortPredicate ) o;

        if ( direction != that.direction ) {
            return false;
        }

        return ( propertyName.equals( that.propertyName ) );
    }


    @Override
    public int hashCode() {
        int result = propertyName.hashCode();
        result = ( 31 * result ) + direction.hashCode();
        return result;
    }


    @Override
    public String toString() {
        return propertyName + ( ( direction == SortDirection.DESCENDING ) ? " DESC" : "" );
    }


    public enum SortDirection {
        ASCENDING(SortOrder.ASC), DESCENDING(SortOrder.DESC);

        private final SortOrder esOrder;


        SortDirection( final SortOrder esOrder ) {this.esOrder = esOrder;}


        /**
         * Get the ES sort direction
         * @return
         */
        public SortOrder toEsSort(){
            return esOrder;
        }




        public static SortDirection find( String s ) {
            if ( s == null ) {
                return ASCENDING;
            }
            s = s.toLowerCase();
            if ( s.startsWith( "asc" ) ) {
                return ASCENDING;
            }
            if ( s.startsWith( "des" ) ) {
                return DESCENDING;
            }
            if ( s.equals( "+" ) ) {
                return ASCENDING;
            }
            if ( s.equals( "-" ) ) {
                return DESCENDING;
            }
            return ASCENDING;
        }
    }
}
