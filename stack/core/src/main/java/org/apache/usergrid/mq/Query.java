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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.utils.JsonUtils;
import org.springframework.util.Assert;

import org.apache.commons.lang.StringUtils;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.usergrid.persistence.Schema.PROPERTY_TYPE;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
import org.apache.usergrid.persistence.index.query.CounterResolution;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.persistence.Query.Level;
import static org.apache.usergrid.utils.ClassUtils.cast;
import static org.apache.usergrid.utils.ConversionUtils.uuid;
import static org.apache.usergrid.utils.ListUtils.first;
import static org.apache.usergrid.utils.ListUtils.firstBoolean;
import static org.apache.usergrid.utils.ListUtils.firstInteger;
import static org.apache.usergrid.utils.ListUtils.firstLong;
import static org.apache.usergrid.utils.ListUtils.firstUuid;
import static org.apache.usergrid.utils.ListUtils.isEmpty;
import static org.apache.usergrid.utils.MapUtils.toMapList;


public class Query {

    private static final Logger logger = LoggerFactory.getLogger( Query.class );

    public static final int DEFAULT_LIMIT = 10;

    protected String type;
    protected List<SortPredicate> sortPredicates = new ArrayList<SortPredicate>();
    protected List<FilterPredicate> filterPredicates = new ArrayList<FilterPredicate>();
    protected UUID startResult;
    protected String cursor;
    protected int limit = 0;
    protected boolean limitSet = false;

    protected Map<String, String> selectSubjects = new LinkedHashMap<String, String>();
    protected boolean mergeSelectResults = false;
    protected Level level = Level.ALL_PROPERTIES;
    protected String connection;
    protected List<String> permissions;
    protected boolean reversed;
    protected boolean reversedSet = false;
    protected Long startTime;
    protected Long finishTime;
    protected boolean pad;
    protected CounterResolution resolution = CounterResolution.ALL;
    protected List<Identifier> users;
    protected List<Identifier> groups;
    protected List<Identifier> identifiers;
    protected List<String> categories;
    protected List<CounterFilterPredicate> counterFilters;


    public Query() {
    }


    public Query( String type ) {
        this.type = type;
    }


    public Query( Query q ) {
        if ( q != null ) {
            type = q.type;
            sortPredicates = q.sortPredicates != null ? new ArrayList<SortPredicate>( q.sortPredicates ) : null;
            filterPredicates = q.filterPredicates != null ? new ArrayList<FilterPredicate>( q.filterPredicates ) : null;
            startResult = q.startResult;
            cursor = q.cursor;
            limit = q.limit;
            limitSet = q.limitSet;
            selectSubjects = q.selectSubjects != null ? new LinkedHashMap<String, String>( q.selectSubjects ) : null;
            mergeSelectResults = q.mergeSelectResults;
            level = q.level;
            connection = q.connection;
            permissions = q.permissions != null ? new ArrayList<String>( q.permissions ) : null;
            reversed = q.reversed;
            reversedSet = q.reversedSet;
            startTime = q.startTime;
            finishTime = q.finishTime;
            resolution = q.resolution;
            pad = q.pad;
            users = q.users != null ? new ArrayList<Identifier>( q.users ) : null;
            groups = q.groups != null ? new ArrayList<Identifier>( q.groups ) : null;
            identifiers = q.identifiers != null ? new ArrayList<Identifier>( q.identifiers ) : null;
            categories = q.categories != null ? new ArrayList<String>( q.categories ) : null;
            counterFilters =
                    q.counterFilters != null ? new ArrayList<CounterFilterPredicate>( q.counterFilters ) : null;
        }
    }


    public static Query fromQL( String ql ) {
        if ( ql == null ) {
            return null;
        }
        ql = ql.trim();

        String qlt = ql.toLowerCase();
        if ( !qlt.startsWith( "select" ) && !qlt.startsWith( "insert" ) && !qlt.startsWith( "update" ) && !qlt
                .startsWith( "delete" ) ) {
            if ( qlt.startsWith( "order by" ) ) {
                ql = "select * " + ql;
            }
            else {
                ql = "select * where " + ql;
            }
        }

        try {
            ANTLRStringStream in = new ANTLRStringStream( ql.trim() );
            QueryFilterLexer lexer = new QueryFilterLexer( in );
            CommonTokenStream tokens = new CommonTokenStream( lexer );
            QueryFilterParser parser = new QueryFilterParser( tokens );
            Query q = parser.ql();
            return q;
        }
        catch ( Exception e ) {
            logger.error( "Unable to parse \"" + ql + "\"", e );
        }
        return null;
    }


    public static Query newQueryIfNull( Query query ) {
        if ( query == null ) {
            query = new Query();
        }
        return query;
    }


    public static Query fromJsonString( String json ) {
        Object o = JsonUtils.parse( json );
        if ( o instanceof Map ) {
            @SuppressWarnings({ "unchecked", "rawtypes" }) Map<String, List<String>> params =
                    cast( toMapList( ( Map ) o ) );
            return fromQueryParams( params );
        }
        return null;
    }


    public static Query fromQueryParams( Map<String, List<String>> params ) {
        String type = null;
        Query q = null;
        String ql = null;
        String connection = null;
        UUID start = null;
        String cursor = null;
        Integer limit = null;
        List<String> permissions = null;
        Boolean reversed = null;
        Long startTime = null;
        Long finishTime = null;
        Boolean pad = null;
        CounterResolution resolution = null;
        List<Identifier> users = null;
        List<Identifier> groups = null;
        List<Identifier> identifiers = null;
        List<String> categories = null;
        List<CounterFilterPredicate> counterFilters = null;

        List<String> l = null;

        ql = first( params.get( "ql" ) );
        type = first( params.get( "type" ) );
        reversed = firstBoolean( params.get( "reversed" ) );
        connection = first( params.get( "connection" ) );
        start = firstUuid( params.get( "start" ) );
        cursor = first( params.get( "cursor" ) );
        limit = firstInteger( params.get( "limit" ) );
        permissions = params.get( "permission" );
        startTime = firstLong( params.get( "start_time" ) );
        finishTime = firstLong( params.get( "end_time" ) );

        l = params.get( "resolution" );
        if ( !isEmpty( l ) ) {
            resolution = CounterResolution.fromString( l.get( 0 ) );
        }

        users = Identifier.fromList( params.get( "user" ) );
        groups = Identifier.fromList( params.get( "group" ) );

        categories = params.get( "category" );

        l = params.get( "counter" );
        if ( !isEmpty( l ) ) {
            counterFilters = CounterFilterPredicate.fromList( l );
        }

        pad = firstBoolean( params.get( "pad" ) );

        for ( Entry<String, List<String>> param : params.entrySet() ) {
            if ( ( param.getValue() == null ) || ( param.getValue().size() == 0 ) ) {
                Identifier identifier = Identifier.from( param.getKey() );
                if ( identifier != null ) {
                    if ( identifiers == null ) {
                        identifiers = new ArrayList<Identifier>();
                    }
                    identifiers.add( identifier );
                }
            }
        }

        if ( ql != null ) {
            q = Query.fromQL( ql );
        }

        l = params.get( "filter" );
        if ( !isEmpty( l ) ) {
            q = newQueryIfNull( q );
            for ( String s : l ) {
                q.addFilter( s );
            }
        }

        l = params.get( "sort" );
        if ( !isEmpty( l ) ) {
            q = newQueryIfNull( q );
            for ( String s : l ) {
                q.addSort( s );
            }
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

        if ( categories != null ) {
            q = newQueryIfNull( q );
            q.setCategories( categories );
        }

        if ( counterFilters != null ) {
            q = newQueryIfNull( q );
            q.setCounterFilters( counterFilters );
        }

        if ( pad != null ) {
            q = newQueryIfNull( q );
            q.setPad( pad );
        }

        if ( users != null ) {
            q = newQueryIfNull( q );
            q.setUsers( users );
        }

        if ( groups != null ) {
            q = newQueryIfNull( q );
            q.setGroups( groups );
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


    public static Query searchForProperty( String propertyName, Object propertyValue ) {
        Query q = new Query();
        q.addEqualityFilter( propertyName, propertyValue );
        return q;
    }


    public static Query findForProperty( String propertyName, Object propertyValue ) {
        Query q = new Query();
        q.addEqualityFilter( propertyName, propertyValue );
        q.setLimit( 1 );
        return q;
    }


    public static Query fromUUID( UUID uuid ) {
        Query q = new Query();
        q.addIdentifier( Identifier.fromUUID( uuid ) );
        return q;
    }


    public static Query fromName( String name ) {
        Query q = new Query();
        q.addIdentifier( Identifier.fromName( name ) );
        return q;
    }


    public static Query fromEmail( String email ) {
        Query q = new Query();
        q.addIdentifier( Identifier.fromEmail( email ) );
        return q;
    }


    public static Query fromIdentifier( Object id ) {
        Query q = new Query();
        q.addIdentifier( Identifier.from( id ) );
        return q;
    }


    public boolean isIdsOnly() {
        if ( ( selectSubjects.size() == 1 ) && selectSubjects.containsKey( PROPERTY_UUID ) ) {
            level = Level.IDS;
            return true;
        }
        return false;
    }


    public void setIdsOnly( boolean idsOnly ) {
        if ( idsOnly ) {
            selectSubjects = new LinkedHashMap<String, String>();
            selectSubjects.put( PROPERTY_UUID, PROPERTY_UUID );
            level = Level.IDS;
        }
        else if ( isIdsOnly() ) {
            selectSubjects = new LinkedHashMap<String, String>();
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


    public String getEntityType() {
        return type;
    }


    public void setEntityType( String type ) {
        this.type = type;
    }


    public Query withEntityType( String type ) {
        this.type = type;
        return this;
    }


    public String getConnectionType() {
        return connection;
    }


    public void setConnectionType( String connection ) {
        this.connection = connection;
    }


    public Query withConnectionType( String connection ) {
        this.connection = connection;
        return this;
    }


    public List<String> getPermissions() {
        return permissions;
    }


    public void setPermissions( List<String> permissions ) {
        this.permissions = permissions;
    }


    public Query withPermissions( List<String> permissions ) {
        this.permissions = permissions;
        return this;
    }


    public Query addSelect( String select ) {

        return addSelect( select, null );
    }


    public Query addSelect( String select, String output ) {
        // be paranoid with the null checks because
        // the query parser sometimes flakes out
        if ( select == null ) {
            return this;
        }
        select = select.trim();

        if ( select.equals( "*" ) ) {
            return this;
        }

        mergeSelectResults = StringUtils.isNotEmpty(output);

        if ( output == null ) {
            output = "";
        }

        selectSubjects.put( select, output );

        return this;
    }


    public boolean hasSelectSubjects() {
        return !selectSubjects.isEmpty();
    }


    public Set<String> getSelectSubjects() {
        return selectSubjects.keySet();
    }


    public Map<String, String> getSelectAssignments() {
        return selectSubjects;
    }


    public void setMergeSelectResults( boolean mergeSelectResults ) {
        this.mergeSelectResults = mergeSelectResults;
    }


    public Query withMergeSelectResults( boolean mergeSelectResults ) {
        this.mergeSelectResults = mergeSelectResults;
        return this;
    }


    public boolean isMergeSelectResults() {
        return mergeSelectResults;
    }


    public Query addSort( String propertyName ) {
        if ( isBlank( propertyName ) ) {
            return this;
        }
        propertyName = propertyName.trim();
        if ( propertyName.indexOf( ',' ) >= 0 ) {
            String[] propertyNames = split( propertyName, ',' );
            for ( String s : propertyNames ) {
                addSort( s );
            }
            return this;
        }

        SortDirection direction = SortDirection.ASCENDING;
        if ( propertyName.indexOf( ' ' ) >= 0 ) {
            String[] parts = split( propertyName, ' ' );
            if ( parts.length > 1 ) {
                propertyName = parts[0];
                direction = SortDirection.find( parts[1] );
            }
        }
        else if ( propertyName.startsWith( "-" ) ) {
            propertyName = propertyName.substring( 1 );
            direction = SortDirection.DESCENDING;
        }
        else if ( propertyName.startsWith( "+" ) ) {
            propertyName = propertyName.substring( 1 );
            direction = SortDirection.ASCENDING;
        }

        return addSort( propertyName, direction );
    }


    public Query addSort( String propertyName, SortDirection direction ) {
        if ( isBlank( propertyName ) ) {
            return this;
        }
        propertyName = propertyName.trim();
        for ( SortPredicate s : sortPredicates ) {
            if ( s.getPropertyName().equals( propertyName ) ) {
                logger.error(
                        "Attempted to set sort order for " + s.getPropertyName() + " more than once, discardng..." );
                return this;
            }
        }
        sortPredicates.add( new SortPredicate( propertyName, direction ) );
        return this;
    }


    public Query addSort( SortPredicate sort ) {
        if ( sort == null ) {
            return this;
        }
        for ( SortPredicate s : sortPredicates ) {
            if ( s.getPropertyName().equals( sort.getPropertyName() ) ) {
                logger.error(
                        "Attempted to set sort order for " + s.getPropertyName() + " more than once, discardng..." );
                return this;
            }
        }
        sortPredicates.add( sort );
        return this;
    }


    public List<SortPredicate> getSortPredicates() {
        return sortPredicates;
    }


    public boolean hasSortPredicates() {
        return !sortPredicates.isEmpty();
    }


    public Query addEqualityFilter( String propertyName, Object value ) {
        return addFilter( propertyName, FilterOperator.EQUAL, value );
    }


    public Query addFilter( String propertyName, FilterOperator operator, Object value ) {
        if ( ( propertyName == null ) || ( operator == null ) || ( value == null ) ) {
            return this;
        }
        if ( PROPERTY_TYPE.equalsIgnoreCase( propertyName ) && ( value != null ) ) {
            if ( operator == FilterOperator.EQUAL ) {
                type = value.toString();
            }
        }
        else if ( "connection".equalsIgnoreCase( propertyName ) && ( value != null ) ) {
            if ( operator == FilterOperator.EQUAL ) {
                connection = value.toString();
            }
        }
        else {
            for ( FilterPredicate f : filterPredicates ) {
                if ( f.getPropertyName().equals( propertyName ) && f.getValue().equals( value ) && "*"
                        .equals( value ) ) {
                    logger.error( "Attempted to set wildcard wilder for " + f.getPropertyName()
                            + " more than once, discardng..." );
                    return this;
                }
            }
            filterPredicates.add( FilterPredicate.normalize( new FilterPredicate( propertyName, operator, value ) ) );
        }
        return this;
    }


    public Query addFilter( String filterStr ) {
        if ( filterStr == null ) {
            return this;
        }
        FilterPredicate filter = FilterPredicate.valueOf( filterStr );
        if ( ( filter != null ) && ( filter.propertyName != null ) && ( filter.operator != null ) && ( filter.value
                != null ) ) {

            if ( PROPERTY_TYPE.equalsIgnoreCase( filter.propertyName ) ) {
                if ( filter.operator == FilterOperator.EQUAL ) {
                    type = filter.value.toString();
                }
            }
            else if ( "connection".equalsIgnoreCase( filter.propertyName ) ) {
                if ( filter.operator == FilterOperator.EQUAL ) {
                    connection = filter.value.toString();
                }
            }
            else {
                for ( FilterPredicate f : filterPredicates ) {
                    if ( f.getPropertyName().equals( filter.getPropertyName() ) && f.getValue()
                                                                                    .equals( filter.getValue() ) && "*"
                            .equals( filter.getValue() ) ) {
                        logger.error( "Attempted to set wildcard wilder for " + f.getPropertyName()
                                + " more than once, discardng..." );
                        return this;
                    }
                }
                filterPredicates.add( filter );
            }
        }
        else {
            logger.error( "Unable to add filter to query: " + filterStr );
        }
        return this;
    }


    public Query addFilter( FilterPredicate filter ) {
        filter = FilterPredicate.normalize( filter );
        if ( ( filter != null ) && ( filter.propertyName != null ) && ( filter.operator != null ) && ( filter.value
                != null ) ) {

            if ( PROPERTY_TYPE.equalsIgnoreCase( filter.propertyName ) ) {
                if ( filter.operator == FilterOperator.EQUAL ) {
                    type = filter.value.toString();
                }
            }
            else if ( "connection".equalsIgnoreCase( filter.propertyName ) ) {
                if ( filter.operator == FilterOperator.EQUAL ) {
                    connection = filter.value.toString();
                }
            }
            else {
                filterPredicates.add( filter );
            }
        }
        return this;
    }


    public List<FilterPredicate> getFilterPredicates() {
        return filterPredicates;
    }


    public boolean hasFilterPredicates() {
        return !filterPredicates.isEmpty();
    }


    public Map<String, Object> getEqualityFilters() {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        for ( FilterPredicate f : filterPredicates ) {
            if ( f.operator == FilterOperator.EQUAL ) {
                Object val = f.getStartValue();
                if ( val != null ) {
                    map.put( f.getPropertyName(), val );
                }
            }
        }
        return map.size() > 0 ? map : null;
    }


    public boolean hasFiltersForProperty( String name ) {
        return hasFiltersForProperty( FilterOperator.EQUAL, name );
    }


    public boolean hasFiltersForProperty( FilterOperator operator, String name ) {
        return getFilterForProperty( operator, name ) != null;
    }


    public FilterPredicate getFilterForProperty( FilterOperator operator, String name ) {
        if ( name == null ) {
            return null;
        }
        ListIterator<FilterPredicate> iterator = filterPredicates.listIterator();
        while ( iterator.hasNext() ) {
            FilterPredicate f = iterator.next();
            if ( f.propertyName.equalsIgnoreCase( name ) ) {
                if ( operator != null ) {
                    if ( operator == f.operator ) {
                        return f;
                    }
                }
                else {
                    return f;
                }
            }
        }
        return null;
    }


    public void removeFiltersForProperty( String name ) {
        if ( name == null ) {
            return;
        }
        ListIterator<FilterPredicate> iterator = filterPredicates.listIterator();
        while ( iterator.hasNext() ) {
            FilterPredicate f = iterator.next();
            if ( f.propertyName.equalsIgnoreCase( name ) ) {
                iterator.remove();
            }
        }
    }


    public void setStartResult( UUID startResult ) {
        this.startResult = startResult;
    }


    public Query withStartResult( UUID startResult ) {
        this.startResult = startResult;
        return this;
    }


    public UUID getStartResult() {
        if ( ( startResult == null ) && ( cursor != null ) ) {
            byte[] cursorBytes = decodeBase64( cursor );
            if ( ( cursorBytes != null ) && ( cursorBytes.length == 16 ) ) {
                startResult = uuid( cursorBytes );
            }
        }
        return startResult;
    }


    public String getCursor() {
        return cursor;
    }


    public void setCursor( String cursor ) {
        if ( cursor != null ) {
            if ( cursor.length() == 22 ) {
                byte[] cursorBytes = decodeBase64( cursor );
                if ( ( cursorBytes != null ) && ( cursorBytes.length == 16 ) ) {
                    startResult = uuid( cursorBytes );
                    cursor = null;
                }
            }
        }
        this.cursor = cursor;
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
            if ( defaultLimit > 0 ) {
                return defaultLimit;
            }
            else {
                return DEFAULT_LIMIT;
            }
        }
        return limit;
    }


    public void setLimit( int limit ) {
        limitSet = true;
        this.limit = limit;
    }


    public Query withLimit( int limit ) {
        limitSet = true;
        this.limit = limit;
        return this;
    }


    public boolean isLimitSet() {
        return limitSet;
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


    public List<Identifier> getUsers() {
        return users;
    }


    public void addUser( Identifier user ) {
        if ( users == null ) {
            users = new ArrayList<Identifier>();
        }
        users.add( user );
    }


    public void setUsers( List<Identifier> users ) {
        this.users = users;
    }


    public List<Identifier> getGroups() {
        return groups;
    }


    public void addGroup( Identifier group ) {
        if ( groups == null ) {
            groups = new ArrayList<Identifier>();
        }
        groups.add( group );
    }


    public void setGroups( List<Identifier> groups ) {
        this.groups = groups;
    }


    public List<Identifier> getIdentifiers() {
        return identifiers;
    }


    public void addIdentifier( Identifier identifier ) {
        if ( identifiers == null ) {
            identifiers = new ArrayList<Identifier>();
        }
        identifiers.add( identifier );
    }


    public void setIdentifiers( List<Identifier> identifiers ) {
        this.identifiers = identifiers;
    }


    public boolean containsUuidIdentifersOnly() {
        if ( hasFilterPredicates() ) {
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


    public boolean containsSingleUuidIdentifier() {
        return containsUuidIdentifersOnly() && ( identifiers.size() == 1 );
    }


    public List<UUID> getUuidIdentifiers() {
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return null;
        }
        List<UUID> ids = new ArrayList<UUID>();
        for ( Identifier identifier : identifiers ) {
            if ( identifier.isUUID() ) {
                ids.add( identifier.getUUID() );
            }
        }
        return ids;
    }


    public UUID getSingleUuidIdentifier() {
        if ( !containsSingleUuidIdentifier() ) {
            return null;
        }
        return ( identifiers.get( 0 ).getUUID() );
    }


    public boolean containsNameIdentifiersOnly() {
        if ( hasFilterPredicates() ) {
            return false;
        }
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return false;
        }
        for ( Identifier identifier : identifiers ) {
            if ( !identifier.isName() ) {
                return false;
            }
        }
        return true;
    }


    public boolean containsSingleNameIdentifier() {
        return containsNameIdentifiersOnly() && ( identifiers.size() == 1 );
    }


    public List<String> getNameIdentifiers() {
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return null;
        }
        List<String> names = new ArrayList<String>();
        for ( Identifier identifier : identifiers ) {
            if ( identifier.isName() ) {
                names.add( identifier.getName() );
            }
        }
        return names;
    }


    public String getSingleNameIdentifier() {
        if ( !containsSingleNameIdentifier() ) {
            return null;
        }
        return ( identifiers.get( 0 ).toString() );
    }


    public boolean containsEmailIdentifiersOnly() {
        if ( hasFilterPredicates() ) {
            return false;
        }
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return false;
        }
        for ( Identifier identifier : identifiers ) {
            if ( identifier.isEmail() ) {
                return false;
            }
        }
        return true;
    }


    public boolean containsSingleEmailIdentifier() {
        return containsEmailIdentifiersOnly() && ( identifiers.size() == 1 );
    }


    public List<String> getEmailIdentifiers() {
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return null;
        }
        List<String> emails = new ArrayList<String>();
        for ( Identifier identifier : identifiers ) {
            if ( identifier.isEmail() ) {
                emails.add( identifier.getEmail() );
            }
        }
        return emails;
    }


    public String getSingleEmailIdentifier() {
        if ( !containsSingleEmailIdentifier() ) {
            return null;
        }
        return ( identifiers.get( 0 ).toString() );
    }


    public boolean containsNameOrEmailIdentifiersOnly() {
        if ( hasFilterPredicates() ) {
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


    public boolean containsSingleNameOrEmailIdentifier() {
        return containsNameOrEmailIdentifiersOnly() && ( identifiers.size() == 1 );
    }


    public List<String> getNameAndEmailIdentifiers() {
        if ( ( identifiers == null ) || identifiers.isEmpty() ) {
            return null;
        }
        List<String> ids = new ArrayList<String>();
        for ( Identifier identifier : identifiers ) {
            if ( identifier.isEmail() ) {
                ids.add( identifier.getEmail() );
            }
            else if ( identifier.isName() ) {
                ids.add( identifier.getName() );
            }
        }
        return ids;
    }


    public String getSingleNameOrEmailIdentifier() {
        if ( !containsSingleNameOrEmailIdentifier() ) {
            return null;
        }
        return ( identifiers.get( 0 ).toString() );
    }


    public List<String> getCategories() {
        return categories;
    }


    public void addCategory( String category ) {
        if ( categories == null ) {
            categories = new ArrayList<String>();
        }
        categories.add( category );
    }


    public void setCategories( List<String> categories ) {
        this.categories = categories;
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


    public void setCounterFilters( List<CounterFilterPredicate> counterFilters ) {
        this.counterFilters = counterFilters;
    }


    @Override
    public String toString() {
        if ( selectSubjects.isEmpty() && filterPredicates.isEmpty() ) {
            return "";
        }

        StringBuilder s = new StringBuilder( "select " );
        if ( type == null ) {
            if ( selectSubjects.isEmpty() ) {
                s.append( "*" );
            }
            else {
                if ( mergeSelectResults ) {
                    s.append( "{ " );
                    boolean first = true;
                    for ( Map.Entry<String, String> select : selectSubjects.entrySet() ) {
                        if ( !first ) {
                            s.append( ", " );
                        }
                        s.append( select.getValue() + " : " + select.getKey() );
                        first = false;
                    }
                    s.append( " }" );
                }
                else {
                    boolean first = true;
                    for ( String select : selectSubjects.keySet() ) {
                        if ( !first ) {
                            s.append( ", " );
                        }
                        s.append( select );
                        first = false;
                    }
                }
            }
        }
        else {
            s.append( type );
        }
        if ( !filterPredicates.isEmpty() ) {
            s.append( " where " );
            boolean first = true;
            for ( FilterPredicate f : filterPredicates ) {
                if ( !first ) {
                    s.append( " and " );
                }
                s.append( f.toString() );
                first = false;
            }
        }
        return s.toString();
    }


    public static enum FilterOperator {
        LESS_THAN( "<", "lt" ), LESS_THAN_OR_EQUAL( "<=", "lte" ), GREATER_THAN( ">", "gt" ),
        GREATER_THAN_OR_EQUAL( ">=", "gte" ), EQUAL( "=", "eq" ), NOT_EQUAL( "!=", "ne" ), IN( "in", null ),
        CONTAINS( "contains", null ), WITHIN( "within", null );

        private final String shortName;
        private final String textName;


        FilterOperator( String shortName, String textName ) {
            this.shortName = shortName;
            this.textName = textName;
        }


        static Map<String, FilterOperator> nameMap = new ConcurrentHashMap<String, FilterOperator>();


        static {
            for ( FilterOperator op : EnumSet.allOf( FilterOperator.class ) ) {
                if ( op.shortName != null ) {
                    nameMap.put( op.shortName, op );
                }
                if ( op.textName != null ) {
                    nameMap.put( op.textName, op );
                }
            }
        }


        public static FilterOperator find( String s ) {
            if ( s == null ) {
                return null;
            }
            return nameMap.get( s );
        }


        @Override
        public String toString() {
            return shortName;
        }
    }


    public static enum SortDirection {
        ASCENDING, DESCENDING;


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


    public static final class SortPredicate implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final Query.SortDirection direction;


        public SortPredicate( String propertyName, Query.SortDirection direction ) {
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


        public Query.SortDirection getDirection() {
            return direction;
        }


        public FilterPredicate toFilter() {
            return new FilterPredicate( propertyName, FilterOperator.EQUAL, "*" );
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
            return propertyName + ( ( direction == Query.SortDirection.DESCENDING ) ? " DESC" : "" );
        }
    }


    public static final class FilterPredicate implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String propertyName;
        private final Query.FilterOperator operator;
        private final Object value;
        private String cursor;


        @SuppressWarnings({ "rawtypes", "unchecked" })
        public FilterPredicate( String propertyName, Query.FilterOperator operator, Object value ) {
            Assert.notNull(propertyName, "Property name was null");
            Assert.notNull(operator, "Operator was null");
            if ( ( operator == Query.FilterOperator.IN ) || ( operator == Query.FilterOperator.WITHIN ) ) {
                if ( ( !( value instanceof Collection ) ) && ( value instanceof Iterable ) ) {
                    List newValue = new ArrayList();
                    for (Object val : ((Iterable) value)) {
                        newValue.add(val);
                    }
                    value = newValue;
                }
                // DataTypeUtils.checkSupportedValue(propertyName, value, true,
                // true);
            }
            else {
                // DataTypeUtils.checkSupportedValue(propertyName, value, false,
                // false);
            }
            this.propertyName = propertyName;
            this.operator = operator;
            this.value = value;
        }


        public FilterPredicate( String propertyName, String operator, String value, String secondValue,
                                String thirdValue ) {
            this.propertyName = propertyName;
            this.operator = FilterOperator.find( operator );
            Object first_obj = parseValue( value, 0 );
            Object second_obj = parseValue( secondValue, 0 );
            Object third_obj = parseValue( thirdValue, 0 );
            if ( second_obj != null ) {
                if ( third_obj != null ) {
                    this.value = Arrays.asList( first_obj, second_obj, third_obj );
                }
                else {
                    this.value = Arrays.asList( first_obj, second_obj );
                }
            }
            else {
                this.value = first_obj;
            }
        }


        public FilterPredicate( String propertyName, String operator, String value, int valueType, String secondValue,
                                int secondValueType, String thirdValue, int thirdValueType ) {
            this.propertyName = propertyName;
            this.operator = FilterOperator.find( operator );
            Object first_obj = parseValue( value, valueType );
            Object second_obj = parseValue( secondValue, secondValueType );
            Object third_obj = parseValue( thirdValue, thirdValueType );
            if ( second_obj != null ) {
                if ( third_obj != null ) {
                    this.value = Arrays.asList( first_obj, second_obj, third_obj );
                }
                else {
                    this.value = Arrays.asList( first_obj, second_obj );
                }
            }
            else {
                this.value = first_obj;
            }
        }


        private static Object parseValue( String val, int valueType ) {
            if ( val == null ) {
                return null;
            }

            if ( val.startsWith( "'" ) && ( val.length() > 1 ) ) {
                return val.substring( 1, val.length() - 1 );
            }

            if ( val.equalsIgnoreCase( "true" ) || val.equalsIgnoreCase( "false" ) ) {
                return Boolean.valueOf( val );
            }

            if ( val.length() == 36 ) {
                try {
                    return UUID.fromString( val );
                }
                catch ( IllegalArgumentException e ) {
                }
            }

            try {
                return Long.valueOf( val );
            }
            catch ( NumberFormatException e ) {
            }

            try {
                return Float.valueOf( val );
            }
            catch ( NumberFormatException e ) {

            }

            return null;
        }


        public static FilterPredicate valueOf( String str ) {
            if ( str == null ) {
                return null;
            }
            try {
                ANTLRStringStream in = new ANTLRStringStream( str.trim() );
                QueryFilterLexer lexer = new QueryFilterLexer( in );
                CommonTokenStream tokens = new CommonTokenStream( lexer );
                QueryFilterParser parser = new QueryFilterParser( tokens );
                FilterPredicate filter = parser.filter();
                return normalize( filter );
            }
            catch ( Exception e ) {
                logger.error( "Unable to parse \"" + str + "\"", e );
            }
            return null;
        }


        public static FilterPredicate normalize( FilterPredicate p ) {
            if ( p == null ) {
                return null;
            }
            if ( p.operator == FilterOperator.CONTAINS ) {
                String propertyName = appendSuffix( p.propertyName, "keywords" );
                return new FilterPredicate( propertyName, FilterOperator.EQUAL, p.value );
            }
            else if ( p.operator == FilterOperator.WITHIN ) {
                String propertyName = appendSuffix( p.propertyName, "coordinates" );
                return new FilterPredicate( propertyName, FilterOperator.WITHIN, p.value );
            }

            return p;
        }


        private static String appendSuffix( String str, String suffix ) {
            if ( StringUtils.isNotEmpty( str ) ) {
                if ( !str.endsWith( "." + suffix ) ) {
                    str += "." + suffix;
                }
            }
            else {
                str = suffix;
            }
            return str;
        }


        public String getPropertyName() {
            return propertyName;
        }


        public Query.FilterOperator getOperator() {
            return operator;
        }


        public Object getValue() {
            return value;
        }


        @SuppressWarnings("unchecked")
        public Object getStartValue() {
            if ( value instanceof List ) {
                List<Object> l = ( List<Object> ) value;
                return l.get( 0 );
            }
            if ( ( operator == FilterOperator.GREATER_THAN ) || ( operator == FilterOperator.GREATER_THAN_OR_EQUAL )
                    || ( operator == FilterOperator.EQUAL ) ) {
                return value;
            }
            else {
                return null;
            }
        }


        @SuppressWarnings("unchecked")
        public Object getFinishValue() {
            if ( value instanceof List ) {
                List<Object> l = ( List<Object> ) value;
                if ( l.size() > 1 ) {
                    return l.get( 1 );
                }
                return null;
            }
            if ( ( operator == FilterOperator.LESS_THAN ) || ( operator == FilterOperator.LESS_THAN_OR_EQUAL ) || (
                    operator == FilterOperator.EQUAL ) ) {
                return value;
            }
            else {
                return null;
            }
        }


        public void setCursor( String cursor ) {
            this.cursor = cursor;
        }


        public String getCursor() {
            return cursor;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = ( prime * result ) + ( ( operator == null ) ? 0 : operator.hashCode() );
            result = ( prime * result ) + ( ( propertyName == null ) ? 0 : propertyName.hashCode() );
            result = ( prime * result ) + ( ( value == null ) ? 0 : value.hashCode() );
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
            FilterPredicate other = ( FilterPredicate ) obj;
            if ( operator != other.operator ) {
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


        @Override
        public String toString() {
            String valueStr = "\'\'";
            if ( value != null ) {
                if ( value instanceof String ) {
                    valueStr = "\'" + value + "\'";
                }
                else {
                    valueStr = value.toString();
                }
            }
            return propertyName + " " + operator.toString() + " " + valueStr;
        }
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
            String[] l = split( s, ':' );

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

            if ( ( user == null ) && ( group == null ) && ( category == null ) && ( name == null ) ) {
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


    public List<Object> getSelectionResults( Results rs ) {

        List<Entity> entities = rs.getEntities();
        if ( entities == null ) {
            return null;
        }

        if ( !hasSelectSubjects() ) {
            return cast( entities );
        }

        List<Object> results = new ArrayList<Object>();

        for ( Entity entity : entities ) {
            if ( isMergeSelectResults() ) {
                boolean include = false;
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                Map<String, String> selects = getSelectAssignments();
                for ( Map.Entry<String, String> select : selects.entrySet() ) {
                    Object obj = JsonUtils.select( entity, select.getKey(), false );
                    if ( obj == null ) {
                        obj = "";
                    }
                    else {
                        include = true;
                    }
                    result.put( select.getValue(), obj );
                }
                if ( include ) {
                    results.add( result );
                }
            }
            else {
                boolean include = false;
                List<Object> result = new ArrayList<Object>();
                Set<String> selects = getSelectSubjects();
                for ( String select : selects ) {
                    Object obj = JsonUtils.select( entity, select );
                    if ( obj == null ) {
                        obj = "";
                    }
                    else {
                        include = true;
                    }
                    result.add( obj );
                }
                if ( include ) {
                    results.add( result );
                }
            }
        }

        if ( results.size() == 0 ) {
            return null;
        }

        return results;
    }


    public Object getSelectionResult( Results rs ) {
        List<Object> r = getSelectionResults( rs );
        if ( ( r != null ) && ( r.size() > 0 ) ) {
            return r.get( 0 );
        }
        return null;
    }
}
