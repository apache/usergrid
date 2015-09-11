/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.persistence;


import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.persistence.index.SelectFieldMapping;
import org.apache.usergrid.persistence.index.exceptions.QueryParseException;
import org.apache.usergrid.persistence.index.query.CounterResolution;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.index.query.tree.Operand;
import org.apache.usergrid.persistence.index.utils.ClassUtils;
import org.apache.usergrid.persistence.index.utils.ConversionUtils;
import org.apache.usergrid.persistence.index.utils.ListUtils;
import org.apache.usergrid.persistence.index.utils.MapUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;


public class Query {



    public enum Level {
        IDS, REFS, CORE_PROPERTIES, ALL_PROPERTIES, LINKED_PROPERTIES
    }

    public static final int DEFAULT_LIMIT = 10;

    public static final int MAX_LIMIT = 1000;

    public static final String PROPERTY_UUID = "uuid";

    private String type;
    private Operand rootOperand;
    private UUID startResult;
    private Optional<String> cursor = Optional.absent();
    private int limit = 0;

    private boolean mergeSelectResults = false;
    private Level level = Level.ALL_PROPERTIES;
    private String connectionType;
    private List<String> permissions;
    private boolean reversed;
    private boolean reversedSet = false;
    private Long startTime;
    private Long finishTime;
    private boolean pad;
    private CounterResolution resolution = CounterResolution.ALL;
    private List<Identifier> identifiers;
    private List<CounterFilterPredicate> counterFilters;
    private String collection;
    private String ql;
    private Collection<SelectFieldMapping> selectFields;


    private static ObjectMapper mapper = new ObjectMapper();


    List<Operand> filterClauses = new ArrayList<Operand>();

    public Query() {
    }


    /**
     * Creates a deep copy of a query from another query
     * @param q
     */
    public Query( Query q ) {
        if ( q == null ) {
            return;
        }

        ql = q.ql;
        type = q.type;
        startResult = q.startResult;
        cursor = q.cursor;
        limit = q.limit;
        mergeSelectResults = q.mergeSelectResults;
        //level = q.level;
        connectionType = q.connectionType;
        permissions = q.permissions != null ? new ArrayList<>( q.permissions ) : null;
        reversed = q.reversed;
        reversedSet = q.reversedSet;
        startTime = q.startTime;
        finishTime = q.finishTime;
        resolution = q.resolution;
        pad = q.pad;
        rootOperand = q.rootOperand;
        identifiers = q.identifiers != null
                ? new ArrayList<>( q.identifiers ) : null;
        counterFilters = q.counterFilters != null
                ? new ArrayList<>( q.counterFilters ) : null;
        collection = q.collection;
        level = q.level;

    }



    /**
     * Create a query instance from the QL.  If the string is null, return an empty query
     * @param ql
     * @return
     */
    public static Query fromQLNullSafe(final String ql){
        final Query query = fromQL(ql);

        if(query != null){
            return query;
        }

        return new Query();
    }

    public static Query fromQL( String ql ) throws QueryParseException {
        if ( StringUtils.isEmpty(ql) ) {
            return null;
        }

        Query query = new Query(  );
        query.setQl( ql );

        return query;
    }
    public static Query all( ){
        return fromQL("select *");
    }

    /**
     * Create a query from a property equals
     * @param propertyName
     * @param value
     * @return
     */
    public static Query fromEquals(final String propertyName, final String value){
        return fromQL( propertyName + " = '" + value + "'" );
    }


    private static Query newQueryIfNull( Query query ) {
        if ( query == null ) {
            query = new Query();
        }
        return query;
    }


    public static Query fromJsonString( String json ) throws QueryParseException {

        Object o;
        try {
            o = mapper.readValue( json, Object.class );
        } catch (IOException ex) {
            throw new QueryParseException("Error parsing JSON query string " + json, ex);
        }

        if ( o instanceof Map ) {
            @SuppressWarnings({ "unchecked", "rawtypes" }) Map<String, List<String>> params =
                    ClassUtils.cast( MapUtils.toMapList( ( Map ) o ) );
            return fromQueryParams( params );
        }
        return null;
    }


    public static Query fromQueryParams( Map<String, List<String>> params )
            throws QueryParseException {
        Query q = null;
        CounterResolution resolution = null;
        List<Identifier> identifiers = null;
        List<CounterFilterPredicate> counterFilters = null;

        String ql = QueryUtils.queryStrFrom( params );
        String type = ListUtils.first( params.get( "type" ) );
        Boolean reversed = ListUtils.firstBoolean( params.get( "reversed" ) );
        String connection = ListUtils.first( params.get( "connectionType" ) );
        UUID start = ListUtils.firstUuid( params.get( "start" ) );
        String cursor = ListUtils.first( params.get( "cursor" ) );
        Integer limit = ListUtils.firstInteger( params.get( "limit" ) );
        List<String> permissions = params.get( "permission" );
        Long startTime = ListUtils.firstLong( params.get( "start_time" ) );
        Long finishTime = ListUtils.firstLong( params.get( "end_time" ) );

        List<String> l = params.get( "resolution" );
        if ( !ListUtils.isEmpty( l ) ) {
            resolution = CounterResolution.fromString( l.get( 0 ) );
        }

        l = params.get( "counter" );

        if ( !ListUtils.isEmpty( l ) ) {
            counterFilters = CounterFilterPredicate.fromList( l );
        }

        Boolean pad = ListUtils.firstBoolean( params.get( "pad" ) );

        for ( Entry<String, List<String>> param : params.entrySet() ) {
            Identifier identifier = Identifier.from( param.getKey() );
            if ( ( param.getValue() == null ) || ( param.getValue().size() == 0 ) || identifier.isUUID() ) {
                if ( identifier != null ) {
                    if ( identifiers == null ) {
                        identifiers = new ArrayList<Identifier>();
                    }
                    identifiers.add( identifier );
                }
            }
        }

        if ( ql != null ) {
            q = Query.fromQL( decode( ql ) );
        }


        if ( type != null ) {
            q = newQueryIfNull( q );
            q.setEntityType( type );
        }

        if ( connection != null ) {
            q = newQueryIfNull( q );
            q.setConnectionType( connection );
        }

        if ( permissions != null ) {
            q = newQueryIfNull( q );
            q.setPermissions( permissions );
        }

        if ( start != null ) {
            q = newQueryIfNull( q );
            q.setStartResult( start );
        }

        if ( cursor != null ) {
            q = newQueryIfNull( q );
            q.setCursor( cursor );
        }

        if ( limit != null ) {
            q = newQueryIfNull( q );
            q.setLimit( limit );
        }

        if ( startTime != null ) {
            q = newQueryIfNull( q );
            q.setStartTime( startTime );
        }

        if ( finishTime != null ) {
            q = newQueryIfNull( q );
            q.setFinishTime( finishTime );
        }

        if ( resolution != null ) {
            q = newQueryIfNull( q );
            q.setResolution( resolution );
        }

        if ( counterFilters != null ) {
            q = newQueryIfNull( q );
            q.setCounterFilters( counterFilters );
        }

        if ( pad != null ) {
            q = newQueryIfNull( q );
            q.setPad( pad );
        }

        if ( identifiers != null ) {
            q = newQueryIfNull( q );
            q.setIdentifiers( identifiers );
        }

        if ( reversed != null ) {
            q = newQueryIfNull( q );
            q.setReversed( reversed );
        }

        return q;
    }


    public static Query fromUUID( UUID uuid ) {
        Query q = new Query();
        q.addIdentifier( Identifier.fromUUID( uuid ) );
        return q;
    }


    public static Query fromIdentifier( Object id ) {
        Query q = new Query();
        q.addIdentifier( Identifier.from(id) );
        return q;
    }


    public boolean hasQueryPredicates() {
        return rootOperand != null;
    }


    /**
     * Return true if the query generated select subjects
     * @return
     */
    public boolean hasSelectSubjects() {
        if ( this.selectFields != null )
            if ( this.selectFields.size()>0 )
                return true;

        return false;
    }


    /**
     * Set the select subjects from our query results
     */
    public void setSelectSubjects( final Collection<SelectFieldMapping> selectFields ) {
        this.selectFields = selectFields;
    }


    /**
     * Get the select assignments from our resetus if they were set
     */
    public Collection<SelectFieldMapping> getSelectAssignments() {
        return this.selectFields;
    }


    public boolean containsNameOrEmailIdentifiersOnly() {
        if ( hasQueryPredicates() ) {
            return false;
        }
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return false;
        }
        for ( Identifier identifier : identifiers ) {
            if ( !identifier.isEmail() && !identifier.isName() ) {
                return false;
            }
        }
        return true;
    }


    @JsonIgnore
    public String getSingleNameOrEmailIdentifier() {
        if ( !containsSingleNameOrEmailIdentifier() ) {
            return null;
        }
        return ( identifiers.get( 0 ).toString() );
    }


    public boolean containsSingleNameOrEmailIdentifier() {
        return containsNameOrEmailIdentifiersOnly() && ( identifiers.size() == 1 );
    }


    @JsonIgnore
    public Identifier getSingleIdentifier() {
        return identifiers != null && identifiers.size() == 1 ? identifiers.get( 0 ) : null;
    }


    public boolean containsSingleUuidIdentifier() {
        return containsUuidIdentifiersOnly() && ( identifiers.size() == 1 );
    }


    boolean containsUuidIdentifiersOnly() {
        if ( hasQueryPredicates() ) {
            return false;
        }
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return false;
        }

        for ( Identifier identifier : identifiers ) {
            if ( !identifier.isUUID() ) {
                return false;
            }
        }
        return true;
    }


    @JsonIgnore
    public UUID getSingleUuidIdentifier() {
        if ( !containsSingleUuidIdentifier() ) {
            return null;
        }
        return ( identifiers.get( 0 ).getUUID() );
    }


    @JsonIgnore
    boolean isIdsOnly() {
        return false;
    }

    private void setIdsOnly( boolean idsOnly ) {
        if ( idsOnly ) {
            level = Level.IDS;
        }
        else if ( isIdsOnly() ) {
            level = Level.ALL_PROPERTIES;
        }
    }


    public Level getResultsLevel() {
        isIdsOnly();
        return level;
    }


    public void setResultsLevel( Level level ) {
        setIdsOnly( level == Level.IDS );
        this.level = level;
    }


    public Query withResultsLevel( Level level ) {
        setIdsOnly( level == Level.IDS );
        this.level = level;
        return this;
    }


    public Query withReversed( boolean reversed ) {
        setReversed( reversed );
        return this;
    }


    public String getEntityType() {
        return type;
    }


    public void setEntityType( String type ) {
        this.type = type;
    }

    public List<String> getPermissions() {
        return permissions;
    }


    public void setPermissions( List<String> permissions ) {
        this.permissions = permissions;
    }


    public boolean isMergeSelectResults() {
        return mergeSelectResults;
    }



    void setStartResult( UUID startResult ) {
        this.startResult = startResult;
    }


    public Query withStartResult( UUID startResult ) {
        this.startResult = startResult;
        return this;
    }


    public UUID getStartResult() {
        return startResult;
    }


    public Optional<String> getCursor() {
        return cursor;
    }


    public void setCursor( String cursor ) {
        this.cursor = Optional.fromNullable( cursor );
    }


    public Query withCursor( String cursor ) {
        setCursor( cursor );
        return this;
    }


    public int getLimit() {
        return getLimit( DEFAULT_LIMIT );
    }


    public int getLimit( int defaultLimit ) {
        if ( limit <= 0 ) {
            return  defaultLimit > 0 ? defaultLimit : DEFAULT_LIMIT;
        }
        return limit;
    }


    public void setLimit( int limit ) {

        // TODO tnine.  After users have had time to change their query limits,
        // this needs to be uncommented and enforced.
        //    if(limit > MAX_LIMIT){
        //        throw new IllegalArgumentException(
        //            String.format("Query limit must be <= to %d", MAX_LIMIT));
        //    }

        if ( limit > MAX_LIMIT ) {
            limit = MAX_LIMIT;
        }

        this.limit = limit;
    }


    public Query withLimit( int limit ) {
        setLimit( limit );
        return this;
    }


    public boolean isReversed() {
        return reversed;
    }


    public void setReversed( boolean reversed ) {
        reversedSet = true;
        this.reversed = reversed;
    }


    public boolean isReversedSet() {
        return reversedSet;
    }


    public Long getStartTime() {
        return startTime;
    }


    public void setStartTime( Long startTime ) {
        this.startTime = startTime;
    }


    public Long getFinishTime() {
        return finishTime;
    }


    public void setFinishTime( Long finishTime ) {
        this.finishTime = finishTime;
    }


    public boolean isPad() {
        return pad;
    }


    public void setPad( boolean pad ) {
        this.pad = pad;
    }


    public void setResolution( CounterResolution resolution ) {
        this.resolution = resolution;
    }


    public CounterResolution getResolution() {
        return resolution;
    }


    public void addIdentifier( Identifier identifier ) {
        if ( identifiers == null ) {
            identifiers = new ArrayList<Identifier>();
        }
        identifiers.add( identifier );
    }


    void setIdentifiers( List<Identifier> identifiers ) {
        this.identifiers = identifiers;
    }


    public List<CounterFilterPredicate> getCounterFilters() {
        return counterFilters;
    }


    public void addCounterFilter( String counter ) {
        CounterFilterPredicate p = CounterFilterPredicate.fromString( counter );
        if ( p == null ) {
            return;
        }
        if ( counterFilters == null ) {
            counterFilters = new ArrayList<CounterFilterPredicate>();
        }
        counterFilters.add( p );
    }


    void setCounterFilters( List<CounterFilterPredicate> counterFilters ) {
        this.counterFilters = counterFilters;
    }



    public static final class CounterFilterPredicate implements Serializable {

        private static final long serialVersionUID = 1L;
        private final String name;
        private final Identifier user;
        private final Identifier group;
        private final String queue;
        private final String category;


        public CounterFilterPredicate( String name, Identifier user, Identifier group, String queue, String category ) {
            this.name = name;
            this.user = user;
            this.group = group;
            this.queue = queue;
            this.category = category;
        }


        public Identifier getUser() {
            return user;
        }


        public Identifier getGroup() {
            return group;
        }


        public String getQueue() {
            return queue;
        }


        public String getCategory() {
            return category;
        }


        public String getName() {
            return name;
        }


        public static CounterFilterPredicate fromString( String s ) {
            Identifier user = null;
            Identifier group = null;
            String category = null;
            String name = null;
            String[] l = StringUtils.split( s, ':' );

            if ( l.length > 0 ) {
                if ( !"*".equals( l[0] ) ) {
                    name = l[0];
                }
            }

            if ( l.length > 1 ) {
                if ( !"*".equals( l[1] ) ) {
                    user = Identifier.from( l[1] );
                }
            }

            if ( l.length > 2 ) {
                if ( !"*".equals( l[2] ) ) {
                    group = Identifier.from( l[3] );
                }
            }

            if ( l.length > 3 ) {
                if ( !"*".equals( l[3] ) ) {
                    category = l[3];
                }
            }

            if ( ( user == null ) && ( group == null ) && ( category == null ) && ( name == null)) {
                return null;
            }

            return new CounterFilterPredicate( name, user, group, null, category );
        }


        public static List<CounterFilterPredicate> fromList( List<String> l ) {
            if ( ( l == null ) || ( l.size() == 0 ) ) {
                return null;
            }
            List<CounterFilterPredicate> counterFilters = new ArrayList<CounterFilterPredicate>();
            for ( String s : l ) {
                CounterFilterPredicate filter = CounterFilterPredicate.fromString( s );
                if ( filter != null ) {
                    counterFilters.add( filter );
                }
            }
            if ( counterFilters.size() == 0 ) {
                return null;
            }
            return counterFilters;
        }
    }


//    public List<Object> getSelectionResults( Results rs ) {
//
//        List<Entity> entities = rs.getEntities();
//        if ( entities == null ) {
//            return null;
//        }
//
//        if ( !hasSelectSubjects() ) {
//            return cast( entities );
//        }
//
//        List<Object> results = new ArrayList<Object>();
//
//        for ( Entity entity : entities ) {
//            if ( isMergeSelectResults() ) {
//                boolean include = false;
//                Map<String, Object> result = new LinkedHashMap<String, Object>();
//                Map<String, String> selects = getSelectAssignments();
//                for ( Map.Entry<String, String> select : selects.entrySet() ) {
//                    Object obj = JsonUtils.select( entity, select.getValue(), false );
//                    if ( obj != null ) {
//                        include = true;
//                    }
//                    result.put( select.getKey(), obj );
//                }
//                if ( include ) {
//                    results.add( result );
//                }
//            }
//            else {
//                boolean include = false;
//                List<Object> result = new ArrayList<Object>();
//                Set<String> selects = getSelectSubjects();
//                for ( String select : selects ) {
//                    Object obj = JsonUtils.select( entity, select );
//                    if ( obj != null ) {
//                        include = true;
//                    }
//                    result.add( obj );
//                }
//                if ( include ) {
//                    results.add( result );
//                }
//            }
//        }
//
//        if ( results.size() == 0 ) {
//            return null;
//        }
//
//        return results;
//    }


//    public Object getSelectionResult( Results rs ) {
//        List<Object> r = getSelectionResults( rs );
//        if ( ( r != null ) && ( r.size() > 0 ) ) {
//            return r.get( 0 );
//        }
//        return null;
//    }


    private static String decode( String input ) {
        try {
            return URLDecoder.decode( input, "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            // shouldn't happen, but just in case
            throw new RuntimeException( e );
        }
    }


    // note: very likely to be null
    public String getCollection() {
        return collection;
    }


    public void setCollection( String collection ) {
        this.collection = collection;
    }


    // may be null
    public Optional<String> getQl() {

        //if a query exists, but with no ql, we select all
       return Optional.fromNullable( ql );
    }


    /**
     * Return true if no query is present and we should perform a graph search
     * @return
     */
    @JsonIgnore
    public boolean isGraphSearch(){
        return ql == null || ql.trim().toLowerCase().equals("select *");
    }


    public Query setQl( String ql ) {
        this.ql = ql;
        return this;
    }


    /**
     * Get the connection type
     * @return
     */
    public String getConnectionType() {
        return connectionType;
    }


    /**
     * Set the connection type
     * @param connection
     * @return
     */
    public Query setConnectionType( final String connection ) {
        this.connectionType = connection;
        return this;
    }


    public String getType() {
        return type;
    }


    public Level getLevel() {
        return level;
    }
}
