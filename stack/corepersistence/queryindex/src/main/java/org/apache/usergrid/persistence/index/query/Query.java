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


import com.fasterxml.jackson.annotation.JsonIgnore;
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.ClassicToken;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenRewriteStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.usergrid.persistence.index.exceptions.IndexException;
import org.apache.usergrid.persistence.index.exceptions.QueryParseException;
import org.apache.usergrid.persistence.index.impl.EsQueryVistor;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;
import org.apache.usergrid.persistence.index.query.tree.AndOperand;
import org.apache.usergrid.persistence.index.query.tree.ContainsOperand;
import org.apache.usergrid.persistence.index.query.tree.CpQueryFilterLexer;
import org.apache.usergrid.persistence.index.query.tree.CpQueryFilterParser;
import org.apache.usergrid.persistence.index.query.tree.Equal;
import org.apache.usergrid.persistence.index.query.tree.EqualityOperand;
import org.apache.usergrid.persistence.index.query.tree.GreaterThan;
import org.apache.usergrid.persistence.index.query.tree.GreaterThanEqual;
import org.apache.usergrid.persistence.index.query.tree.LessThan;
import org.apache.usergrid.persistence.index.query.tree.LessThanEqual;
import org.apache.usergrid.persistence.index.query.tree.Operand;
import org.apache.usergrid.persistence.index.query.tree.QueryVisitor;
import org.apache.usergrid.persistence.index.utils.ClassUtils;
import org.apache.usergrid.persistence.index.utils.ConversionUtils;
import org.apache.usergrid.persistence.index.utils.ListUtils;
import org.apache.usergrid.persistence.index.utils.MapUtils;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class Query {
    private static final Logger logger = LoggerFactory.getLogger( Query.class );

    public enum Level {
        IDS, REFS, CORE_PROPERTIES, ALL_PROPERTIES, LINKED_PROPERTIES
    }

    public static final int DEFAULT_LIMIT = 10;

    public static final int MAX_LIMIT = 1000;

    public static final String PROPERTY_UUID = "uuid";

    private String type;
    private List<SortPredicate> sortPredicates = new ArrayList<SortPredicate>();
    private Operand rootOperand;
    private UUID startResult;
    private String cursor;
    private int limit = 0;

    private Map<String, String> selectAssignments = new LinkedHashMap<String, String>();
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

        type = q.type;
        sortPredicates = q.sortPredicates != null
                ? new ArrayList<>( q.sortPredicates ) : null;
        startResult = q.startResult;
        cursor = q.cursor;
        limit = q.limit;
        selectAssignments = q.selectAssignments != null
                ? new LinkedHashMap<>( q.selectAssignments ) : null;
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


    public QueryBuilder createQueryBuilder( final String context ) {


        QueryBuilder queryBuilder = null;


        //we have a root operand.  Translate our AST into an ES search
        if ( getRootOperand() != null ) {
            // In the case of geo only queries, this will return null into the query builder.
            // Once we start using tiles, we won't need this check any longer, since a geo query
            // will return a tile query + post filter
            QueryVisitor v = new EsQueryVistor();

            try {
                getRootOperand().visit( v );
            }
            catch ( IndexException ex ) {
                throw new RuntimeException( "Error building ElasticSearch query", ex );
            }


            queryBuilder = v.getQueryBuilder();
        }


         // Add our filter for context to our query for fast execution.
         // Fast because it utilizes bitsets internally. See this post for more detail.
         // http://www.elasticsearch.org/blog/all-about-elasticsearch-filter-bitsets/

        // TODO evaluate performance when it's an all query.
        // Do we need to put the context term first for performance?
        if ( queryBuilder != null ) {
            queryBuilder = QueryBuilders.boolQuery().must( queryBuilder ).must( QueryBuilders
                    .termQuery( IndexingUtils.ENTITY_CONTEXT_FIELDNAME, context ) );
        }

        //nothing was specified ensure we specify the context in the search
        else {
            queryBuilder = QueryBuilders.termQuery( IndexingUtils.ENTITY_CONTEXT_FIELDNAME, context );
        }

        return queryBuilder;
    }


	public FilterBuilder createFilterBuilder() {
	    FilterBuilder filterBuilder = null;

        if ( getRootOperand() != null ) {
            QueryVisitor v = new EsQueryVistor();
            try {
                getRootOperand().visit( v );

            } catch ( IndexException ex ) {
                throw new RuntimeException( "Error building ElasticSearch query", ex );
            }
            filterBuilder = v.getFilterBuilder();
        }

        return filterBuilder;
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
        logger.debug("Processing raw query: " + ql);
        String originalQl = ql;
        ql = ql.trim();

        String qlt = ql.toLowerCase();
        if (       !qlt.startsWith( "select" )
                && !qlt.startsWith( "insert" )
                && !qlt.startsWith( "update" ) && !qlt.startsWith( "delete" ) ) {

            if ( qlt.startsWith( "order by" ) ) {
                ql = "select * " + ql;
            }
            else {
                ql = "select * where " + ql;
            }
        }

        ANTLRStringStream in = new ANTLRStringStream( ql.trim().toLowerCase() );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        CommonTokenStream tokens = new CommonTokenStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );

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

            String message = String.format("The query cannot be parsed. The token '%s' at "
                + "column %d on line %d cannot be " + "parsed", token.getText(), index, lineNumber);

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

        l = params.get( "filter" );

        if ( !ListUtils.isEmpty( l ) ) {
            q = newQueryIfNull( q );
            for ( String s : l ) {
                q.addFilter( decode( s ) );
            }
        }

        l = params.get( "sort" );
        if ( !ListUtils.isEmpty( l ) ) {
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


    public boolean isMergeSelectResults() {
        return mergeSelectResults;
    }


    public Query addSort( SortPredicate sort ) {
        if ( sort == null ) {
            return this;
        }

        for ( SortPredicate s : sortPredicates ) {
            if ( s.getPropertyName().equals( sort.getPropertyName() ) ) {
                throw new QueryParseException( String.format(
                    "Attempted to set sort order for %s more than once", s.getPropertyName() ) );
            }
        }
        sortPredicates.add( sort );
        return this;
    }


    public Query addSort( String propertyName ) {
        if ( StringUtils.isBlank( propertyName ) ) {
            return this;
        }
        propertyName = propertyName.trim();
        if ( propertyName.indexOf( ',' ) >= 0 ) {
            String[] propertyNames = StringUtils.split( propertyName, ',' );
            for ( String s : propertyNames ) {
                addSort( s );
            }
            return this;
        }

        SortDirection direction = SortDirection.ASCENDING;
        if ( propertyName.indexOf( ' ' ) >= 0 ) {
            String[] parts = StringUtils.split( propertyName, ' ' );
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
        if ( StringUtils.isBlank( propertyName ) ) {
            return this;
        }
        propertyName = propertyName.trim();
        for ( SortPredicate s : sortPredicates ) {
            if ( s.getPropertyName().equals( propertyName ) ) {
                logger.error(
                        "Attempted to set sort order for " + s.getPropertyName()
                                + " more than once, discarding..." );
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
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        TokenRewriteStream tokens = new TokenRewriteStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );
        Operand root = null;

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
        Equal equality = new Equal( new ClassicToken( 1, "=" ) );

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
            byte[] cursorBytes = Base64.decodeBase64( cursor );
            if ( ( cursorBytes != null ) && ( cursorBytes.length == 16 ) ) {
                startResult = ConversionUtils.uuid( cursorBytes );
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


        public SortPredicate(@JsonProperty("propertyName")  String propertyName,
                @JsonProperty("direction")  Query.SortDirection direction ) {

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
    public String getQl() {
        return ql;
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
