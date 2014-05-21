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
package org.apache.usergrid.persistence.index.query;


import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.ClassicToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenRewriteStream;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.CounterResolution;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.Identifier;
import org.apache.usergrid.persistence.QueryUtils;
import org.apache.usergrid.persistence.Results;

import org.apache.usergrid.persistence.Results.Level;
import org.apache.usergrid.persistence.exceptions.QueryParseException;
import org.apache.usergrid.persistence.query.tree.AndOperand;
import org.apache.usergrid.persistence.query.tree.ContainsOperand;
import org.apache.usergrid.persistence.query.tree.Equal;
import org.apache.usergrid.persistence.query.tree.EqualityOperand;
import org.apache.usergrid.persistence.query.tree.GreaterThan;
import org.apache.usergrid.persistence.query.tree.GreaterThanEqual;
import org.apache.usergrid.persistence.query.tree.LessThan;
import org.apache.usergrid.persistence.query.tree.LessThanEqual;
import org.apache.usergrid.persistence.query.tree.Operand;
import org.apache.usergrid.persistence.query.tree.QueryFilterLexer;
import org.apache.usergrid.persistence.query.tree.QueryFilterParser;
import org.apache.usergrid.utils.JsonUtils;

import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.split;
import static org.apache.usergrid.persistence.Schema.PROPERTY_UUID;
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

    public static final int MAX_LIMIT = 1000;

    private String type;
    private List<SortPredicate> sortPredicates = new ArrayList<SortPredicate>();
    private Operand rootOperand;
    private UUID startResult;
    private String cursor;
    private int limit = 0;

    private Map<String, String> selectAssignments = new LinkedHashMap<String, String>();
    private boolean mergeSelectResults = false;
    private Level level = Level.ALL_PROPERTIES;
    private String connection;
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

    List<Operand> filterClauses = new ArrayList<Operand>();

    public Query() {
    }


    public Query( Query q ) {
        if ( q != null ) {
            type = q.type;
            sortPredicates = q.sortPredicates != null ? new ArrayList<SortPredicate>( q.sortPredicates ) : null;
            startResult = q.startResult;
            cursor = q.cursor;
            limit = q.limit;
            selectAssignments =
                    q.selectAssignments != null ? new LinkedHashMap<String, String>( q.selectAssignments ) : null;
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
            rootOperand = q.rootOperand;
            identifiers = q.identifiers != null ? new ArrayList<Identifier>( q.identifiers ) : null;
            counterFilters =
                    q.counterFilters != null ? new ArrayList<CounterFilterPredicate>( q.counterFilters ) : null;
            collection = q.collection;
        }
    }


    public static Query fromQL( String ql ) throws QueryParseException {
        if ( ql == null ) {
            return null;
        }
        String originalQl = ql;
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

        ANTLRStringStream in = new ANTLRStringStream( qlt.trim() );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        CommonTokenStream tokens = new CommonTokenStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        try {
            Query q = parser.ql().query;
            q.setQl( originalQl );
            return q;
        }
        catch ( RecognitionException e ) {
            logger.error( "Unable to parse \"{}\"", ql, e );

            int index = e.index;
            int lineNumber = e.line;
            Token token = e.token;

            String message = String.format(
                    "The query cannot be parsed. The token '%s' at column %d on line %d cannot be " + "parsed",
                    token.getText(), index, lineNumber );

            throw new QueryParseException( message, e );
        }
    }


    private static Query newQueryIfNull( Query query ) {
        if ( query == null ) {
            query = new Query();
        }
        return query;
    }


    public static Query fromJsonString( String json ) throws QueryParseException {
        Object o = JsonUtils.parse( json );
        if ( o instanceof Map ) {
            @SuppressWarnings({ "unchecked", "rawtypes" }) Map<String, List<String>> params =
                    cast( toMapList( ( Map ) o ) );
            return fromQueryParams( params );
        }
        return null;
    }


    public static Query fromQueryParams( Map<String, List<String>> params ) throws QueryParseException {
        Query q = null;
        CounterResolution resolution = null;
        List<Identifier> identifiers = null;
        List<CounterFilterPredicate> counterFilters = null;

        String ql = QueryUtils.queryStrFrom( params );
        String type = first( params.get( "type" ) );
        Boolean reversed = firstBoolean( params.get( "reversed" ) );
        String connection = first( params.get( "connection" ) );
        UUID start = firstUuid( params.get( "start" ) );
        String cursor = first( params.get( "cursor" ) );
        Integer limit = firstInteger( params.get( "limit" ) );
        List<String> permissions = params.get( "permission" );
        Long startTime = firstLong( params.get( "start_time" ) );
        Long finishTime = firstLong( params.get( "end_time" ) );

        List<String> l = params.get( "resolution" );
        if ( !isEmpty( l ) ) {
            resolution = CounterResolution.fromString( l.get( 0 ) );
        }

        l = params.get( "counter" );

        if ( !isEmpty( l ) ) {
            counterFilters = CounterFilterPredicate.fromList( l );
        }

        Boolean pad = firstBoolean( params.get( "pad" ) );

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

        l = params.get( "filter" );

        if ( !isEmpty( l ) ) {
            q = newQueryIfNull( q );
            for ( String s : l ) {
                q.addFilter( decode( s ) );
            }
        }

        l = params.get( "sort" );
        if ( !isEmpty( l ) ) {
            q = newQueryIfNull( q );
            for ( String s : l ) {
                q.addSort( decode( s ) );
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


    public static Query fromIdentifier( Object id ) {
        Query q = new Query();
        q.addIdentifier( Identifier.from( id ) );
        return q;
    }


    public boolean hasQueryPredicates() {
        return rootOperand != null;
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


    public Query addSort( SortPredicate sort ) {
        if ( sort == null ) {
            return this;
        }

        for ( SortPredicate s : sortPredicates ) {
            if ( s.getPropertyName().equals( sort.getPropertyName() ) ) {
                throw new QueryParseException(
                        String.format( "Attempted to set sort order for %s more than once", s.getPropertyName() ) );
            }
        }
        sortPredicates.add( sort );
        return this;
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
        if ( ( selectAssignments.size() == 1 ) && selectAssignments.containsKey( PROPERTY_UUID ) ) {
            level = Level.IDS;
            return true;
        }
        return false;
    }


    private void setIdsOnly( boolean idsOnly ) {
        if ( idsOnly ) {
            selectAssignments = new LinkedHashMap<String, String>();
            selectAssignments.put( PROPERTY_UUID, PROPERTY_UUID );
            level = Level.IDS;
        }
        else if ( isIdsOnly() ) {
            selectAssignments = new LinkedHashMap<String, String>();
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


    public String getConnectionType() {
        return connection;
    }


    public void setConnectionType( String connection ) {
        this.connection = connection;
    }


    public List<String> getPermissions() {
        return permissions;
    }


    public void setPermissions( List<String> permissions ) {
        this.permissions = permissions;
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

        mergeSelectResults = StringUtils.isNotEmpty( output );

        if ( output == null ) {
            output = "";
        }

        selectAssignments.put( select, output );

        return this;
    }


    public boolean hasSelectSubjects() {
        return !selectAssignments.isEmpty();
    }


    @JsonIgnore
    public Set<String> getSelectSubjects() {
        return selectAssignments.keySet();
    }


    public Map<String, String> getSelectAssignments() {
        return selectAssignments;
    }


    boolean isMergeSelectResults() {
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
                        "Attempted to set sort order for " + s.getPropertyName() + " more than once, discarding..." );
                return this;
            }
        }
        sortPredicates.add( new SortPredicate( propertyName, direction ) );
        return this;
    }


    @JsonIgnore
    public boolean isSortSet() {
        return !sortPredicates.isEmpty();
    }


    public List<SortPredicate> getSortPredicates() {
        return sortPredicates;
    }


    public Query addFilter( String filter ) {
        ANTLRStringStream in = new ANTLRStringStream( filter );
        QueryFilterLexer lexer = new QueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        QueryFilterParser parser = new QueryFilterParser( tokens );

        Operand root;

        try {
            root = parser.ql().query.getRootOperand();
        }
        catch ( RecognitionException e ) {
            // todo: should we create a specific Exception for this? checked?
            throw new RuntimeException( "Unknown operation: " + filter, e );
        }

        if ( root != null ) {
            addClause( root );
        }

        return this;
    }


    /** Add a less than filter to this query. && with existing clauses */
    public Query addLessThanFilter( String propName, Object value ) {
        LessThan equality = new LessThan( null );

        addClause( equality, propName, value );

        return this;
    }


    /** Add a less than equal filter to this query. && with existing clauses */
    public Query addLessThanEqualFilter( String propName, Object value ) {
        LessThanEqual equality = new LessThanEqual( null );

        addClause( equality, propName, value );

        return this;
    }


    /** Add a equal filter to this query. && with existing clauses */
    public Query addEqualityFilter( String propName, Object value ) {
        Equal equality = new Equal( new ClassicToken( 0, "=" ) );

        addClause( equality, propName, value );

        return this;
    }


    /** Add a greater than equal filter to this query. && with existing clauses */
    public Query addGreaterThanEqualFilter( String propName, Object value ) {
        GreaterThanEqual equality = new GreaterThanEqual( null );

        addClause( equality, propName, value );

        return this;
    }


    /** Add a less than filter to this query. && with existing clauses */
    public Query addGreaterThanFilter( String propName, Object value ) {
        GreaterThan equality = new GreaterThan( null );

        addClause( equality, propName, value );

        return this;
    }


    public Query addContainsFilter( String propName, String keyword ) {
        ContainsOperand equality = new ContainsOperand( new ClassicToken( 0, "contains" ) );

        equality.setProperty( propName );
        equality.setLiteral( keyword );

        addClause( equality );

        return this;
    }


    private void addClause( EqualityOperand equals, String propertyName, Object value ) {
        equals.setProperty( propertyName );
        equals.setLiteral( value );
        addClause( equals );
    }


    private void addClause( Operand clause ) {
        filterClauses.add(clause);

        if ( rootOperand == null ) {
            rootOperand = clause;
            return;
        }

        AndOperand and = new AndOperand();
        and.addChild( rootOperand );
        and.addChild( clause );


        // redirect the root to new && clause
        rootOperand = and;

    }


    @JsonIgnore
    public Operand getRootOperand() {
        if ( rootOperand == null ) { // attempt deserialization
            if ( ql != null ) {
                try {
                    Query q = Query.fromQL( ql );
                    rootOperand = q.rootOperand;
                }
                catch ( QueryParseException e ) {
                    logger.error( "error parsing sql for rootOperand", e ); // shouldn't happen
                }
            }
        }
        return rootOperand;
    }


    public void setRootOperand( Operand root ) {
        this.rootOperand = root;
    }


    public List<Operand> getFilterClauses() {
        return filterClauses;
    }


    void setStartResult( UUID startResult ) {
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

        //      TODO tnine.  After users have had time to change their query limits,
        // this needs to be uncommented and enforced.
        //        if(limit > MAX_LIMIT){
        //          throw new IllegalArgumentException(String.format("Query limit must be <= to %d", MAX_LIMIT));
        //        }

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


    @Override
    public String toString() {
        if ( ql != null ) {
            return ql;
        }
        StringBuilder s = new StringBuilder( "select " );
        if ( selectAssignments.isEmpty() ) {
            s.append( "*" );
        }
        else {
            if ( mergeSelectResults ) {
                s.append( "{ " );
                boolean first = true;
                for ( Map.Entry<String, String> select : selectAssignments.entrySet() ) {
                    if ( !first ) {
                        s.append( ", " );
                    }
                    s.append( select.getValue() ).append( " : " ).append( select.getKey() );
                    first = false;
                }
                s.append( " }" );
            }
            else {
                boolean first = true;
                for ( String select : selectAssignments.keySet() ) {
                    if ( !first ) {
                        s.append( ", " );
                    }
                    s.append( select );
                    first = false;
                }
            }
        }
        s.append( " from " );
        s.append( type );
        if ( !sortPredicates.isEmpty() ) {
            boolean first = true;
            s.append( " order by " );
            for ( SortPredicate sp : sortPredicates ) {
                if ( !first ) {
                    s.append( ", " );
                }
                s.append( sp );
                first = false;
            }
        }
        //      if (!filterPredicates.isEmpty()) {
        //        s.append(" where ");
        //        boolean first = true;
        //        for (FilterPredicate f : filterPredicates) {
        //          if (!first) {
        //            s.append(" and ");
        //          }
        //          s.append(f.toString());
        //          first = false;
        //        }
        //      }
        return s.toString();
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
                    Object obj = JsonUtils.select( entity, select.getValue(), false );
                    if ( obj != null ) {
                        include = true;
                    }
                    result.put( select.getKey(), obj );
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
                    if ( obj != null ) {
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
    public String getQl() {
        return ql;
    }


    public void setQl( String ql ) {
        this.ql = ql;
    }


    public List<Identifier> getIdentifiers() {
        return identifiers;
    }


    public String getConnection() {
        return connection;
    }


    public String getType() {
        return type;
    }


    public Level getLevel() {
        return level;
    }
}
