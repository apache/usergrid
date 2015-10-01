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
package org.apache.usergrid.mq;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.collections.comparators.ComparatorChain;

import org.apache.usergrid.mq.Query.FilterOperator;
import org.apache.usergrid.mq.Query.FilterPredicate;
import org.apache.usergrid.mq.Query.SortPredicate;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.utils.NumberUtils;

import static org.apache.usergrid.mq.Query.SortDirection.DESCENDING;


public class QueryProcessor {

    private static final Logger logger = LoggerFactory.getLogger( QueryProcessor.class );

    Query query;

    String cursor;
    List<QuerySlice> slices;
    List<FilterPredicate> filters;
    List<SortPredicate> sorts;


    public QueryProcessor( Query query ) {
        this.query = query;
        cursor = query.getCursor();
        filters = query.getFilterPredicates();
        sorts = query.getSortPredicates();
        process();
    }


    public Query getQuery() {
        return query;
    }


    public String getCursor() {
        return cursor;
    }


    public List<QuerySlice> getSlices() {
        return slices;
    }


    public List<FilterPredicate> getFilters() {
        return filters;
    }


    public List<SortPredicate> getSorts() {
        return sorts;
    }


    private void process() {
        slices = new ArrayList<QuerySlice>();

        throw new UnsupportedOperationException( "query not supported" );
//        // consolidate all the filters into a set of ranges
//        Set<String> names = getFilterPropertyNames();
//        for ( String name : names ) {
//            FilterOperator operator = null;
//            Object value = null;
//            RangeValue start = null;
//            RangeValue finish = null;
//            for ( FilterPredicate f : filters ) {
//                if ( f.getPropertyName().equals( name ) ) {
//                    operator = f.getOperator();
//                    value = f.getValue();
//                    RangePair r = getRangeForFilter( f );
//                    if ( r.start != null ) {
//                        if ( ( start == null ) || ( r.start.compareTo( start, false ) < 0 ) ) {
//                            start = r.start;
//                        }
//                    }
//                    if ( r.finish != null ) {
//                        if ( ( finish == null ) || ( r.finish.compareTo( finish, true ) > 0 ) ) {
//                            finish = r.finish;
//                        }
//                    }
//                }
//            }
//            slices.add( new QuerySlice( name, operator, value, start, finish, null, false ) );
//        }
//
//        // process sorts
//        if ( ( slices.size() == 0 ) && ( sorts.size() > 0 ) ) {
//            // if no filters, turn first filter into a sort
//            SortPredicate sort = ListUtils.dequeue( sorts );
//            slices.add( new QuerySlice( sort.getPropertyName(), null, null, null, null, null,
//                    sort.getDirection() == DESCENDING ) );
//        }
//        else if ( sorts.size() > 0 ) {
//            // match up sorts with existing filters
//            for ( ListIterator<SortPredicate> iter = sorts.listIterator(); iter.hasNext(); ) {
//                SortPredicate sort = iter.next();
//                QuerySlice slice = getSliceForProperty( sort.getPropertyName() );
//                if ( slice != null ) {
//                    slice.reversed = sort.getDirection() == DESCENDING;
//                    iter.remove();
//                }
//            }
//        }
//
//        // attach cursors to slices
//        if ( ( cursor != null ) && ( cursor.indexOf( ':' ) >= 0 ) ) {
//            String[] cursors = split( cursor, '|' );
//            for ( String c : cursors ) {
//                String[] parts = split( c, ':' );
//                if ( parts.length == 2 ) {
//                    int cursorHashCode = parseInt( parts[0] );
//                    for ( QuerySlice slice : slices ) {
//                        int sliceHashCode = slice.hashCode();
//                        logger.info( "Comparing cursor hashcode " + cursorHashCode + " to " + sliceHashCode );
//                        if ( sliceHashCode == cursorHashCode ) {
//                            if ( isNotBlank( parts[1] ) ) {
//                                ByteBuffer cursorBytes = ByteBuffer.wrap( decodeBase64( parts[1] ) );
//                                slice.setCursor( cursorBytes );
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }


    @SuppressWarnings("unchecked")
    public List<Entity> sort( List<Entity> entities ) {
        throw new UnsupportedOperationException( "Unsupported" );
//
//        if ( ( entities != null ) && ( sorts.size() > 0 ) ) {
//            // Performing in memory sort
//            logger.info( "Performing in-memory sort of {} entities", entities.size() );
//            ComparatorChain chain = new ComparatorChain();
//            for ( SortPredicate sort : sorts ) {
//                chain.addComparator(
//                        new EntityPropertyComparator( sort.getPropertyName(), sort.getDirection() == DESCENDING ) );
//            }
//            Collections.sort( entities, chain );
//        }
//        return entities;
    }


    private Set<String> getFilterPropertyNames() {
        Set<String> names = new LinkedHashSet<String>();
        for ( FilterPredicate f : filters ) {
            names.add( f.getPropertyName() );
        }
        return names;
    }


    public QuerySlice getSliceForProperty( String name ) {
        for ( QuerySlice s : slices ) {
            if ( s.propertyName.equals( name ) ) {
                return s;
            }
        }
        return null;
    }


    public static class RangeValue {
        byte code;
        Object value;
        boolean inclusive;


        public RangeValue( byte code, Object value, boolean inclusive ) {
            this.code = code;
            this.value = value;
            this.inclusive = inclusive;
        }


        public byte getCode() {
            return code;
        }


        public void setCode( byte code ) {
            this.code = code;
        }


        public Object getValue() {
            return value;
        }


        public void setValue( Object value ) {
            this.value = value;
        }


        public boolean isInclusive() {
            return inclusive;
        }


        public void setInclusive( boolean inclusive ) {
            this.inclusive = inclusive;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + code;
            result = prime * result + ( inclusive ? 1231 : 1237 );
            result = prime * result + ( ( value == null ) ? 0 : value.hashCode() );
            return result;
        }


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
            RangeValue other = ( RangeValue ) obj;
            if ( code != other.code ) {
                return false;
            }
            if ( inclusive != other.inclusive ) {
                return false;
            }
            if ( value == null ) {
                if ( other.value != null ) {
                    return false;
                }
            }
            else if ( !value.equals( other.value ) ) {
                return false;
            }
            return true;
        }


        public int compareTo( RangeValue other, boolean finish ) {
            if ( other == null ) {
                return 1;
            }
            if ( code != other.code ) {
                return NumberUtils.sign( code - other.code );
            }
            @SuppressWarnings({ "unchecked", "rawtypes" }) int c = ( ( Comparable ) value ).compareTo( other.value );
            if ( c != 0 ) {
                return c;
            }
            if ( finish ) {
                // for finish values, inclusive means <= which is greater than <
                if ( inclusive != other.inclusive ) {
                    return inclusive ? 1 : -1;
                }
            }
            else {
                // for start values, inclusive means >= which is lest than >
                if ( inclusive != other.inclusive ) {
                    return inclusive ? -1 : 1;
                }
            }
            return 0;
        }


        public static int compare( RangeValue v1, RangeValue v2, boolean finish ) {
            if ( v1 == null ) {
                if ( v2 == null ) {
                    return 0;
                }
                return -1;
            }
            return v1.compareTo( v2, finish );
        }
    }


    public static class RangePair {
        RangeValue start;
        RangeValue finish;


        public RangePair( RangeValue start, RangeValue finish ) {
            this.start = start;
            this.finish = finish;
        }


        public RangeValue getStart() {
            return start;
        }


        public void setStart( RangeValue start ) {
            this.start = start;
        }


        public RangeValue getFinish() {
            return finish;
        }


        public void setFinish( RangeValue finish ) {
            this.finish = finish;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( finish == null ) ? 0 : finish.hashCode() );
            result = prime * result + ( ( start == null ) ? 0 : start.hashCode() );
            return result;
        }


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
            RangePair other = ( RangePair ) obj;
            if ( finish == null ) {
                if ( other.finish != null ) {
                    return false;
                }
            }
            else if ( !finish.equals( other.finish ) ) {
                return false;
            }
            if ( start == null ) {
                if ( other.start != null ) {
                    return false;
                }
            }
            else if ( !start.equals( other.start ) ) {
                return false;
            }
            return true;
        }
    }



    public static class QuerySlice {

        String propertyName;
        FilterOperator operator;
        Object value;
        RangeValue start;
        RangeValue finish;
        ByteBuffer cursor;
        boolean reversed;


        QuerySlice( String propertyName, FilterOperator operator, Object value, RangeValue start, RangeValue finish,
                    ByteBuffer cursor, boolean reversed ) {
            this.propertyName = propertyName;
            this.operator = operator;
            this.value = value;
            this.start = start;
            this.finish = finish;
            this.cursor = cursor;
            this.reversed = reversed;
        }


        public String getPropertyName() {
            return propertyName;
        }


        public void setPropertyName( String propertyName ) {
            this.propertyName = propertyName;
        }


        public RangeValue getStart() {
            return start;
        }


        public void setStart( RangeValue start ) {
            this.start = start;
        }


        public RangeValue getFinish() {
            return finish;
        }


        public void setFinish( RangeValue finish ) {
            this.finish = finish;
        }


        public Object getValue() {
            return value;
        }


        public void setValue( Object value ) {
            this.value = value;
        }


        public ByteBuffer getCursor() {
            return cursor;
        }


        public void setCursor( ByteBuffer cursor ) {
            this.cursor = cursor;
        }


        public boolean isReversed() {
            return reversed;
        }


        public void setReversed( boolean reversed ) {
            this.reversed = reversed;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ( ( finish == null ) ? 0 : finish.hashCode() );
            result = prime * result + ( ( propertyName == null ) ? 0 : propertyName.hashCode() );
            result = prime * result + ( ( start == null ) ? 0 : start.hashCode() );

            //NOTE.  We have explicitly left out direction.  According to IndexTest:testCollectionOrdering,
            // a cursor can be used and change direction
            //of the ordering.
            return result;
        }


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
            QuerySlice other = ( QuerySlice ) obj;
            if ( finish == null ) {
                if ( other.finish != null ) {
                    return false;
                }
            }
            else if ( !finish.equals( other.finish ) ) {
                return false;
            }
            if ( propertyName == null ) {
                if ( other.propertyName != null ) {
                    return false;
                }
            }
            else if ( !propertyName.equals( other.propertyName ) ) {
                return false;
            }

            if ( start == null ) {
                if ( other.start != null ) {
                    return false;
                }
            }
            else if ( !start.equals( other.start ) ) {
                return false;
            }
            return true;
        }
    }
}
