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
package org.apache.usergrid.services;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.index.query.Identifier;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.usergrid.utils.ListUtils.dequeue;
import static org.apache.usergrid.utils.ListUtils.dequeueCopy;
import static org.apache.usergrid.utils.ListUtils.isEmpty;
import static org.apache.usergrid.utils.ListUtils.queue;


public abstract class ServiceParameter {

    private static final Logger logger = LoggerFactory.getLogger( ServiceParameter.class );


    ServiceParameter() {

    }


    public UUID getId() {
        return null;
    }


    public String getName() {
        return null;
    }


    public Query getQuery() {
        return null;
    }


    @Override
    public String toString() {
        return "";
    }


    public boolean isQuery() {
        return false;
    }


    public boolean isId() {
        return false;
    }


    public boolean isName() {
        return false;
    }


    public abstract Identifier getIdentifier();


    public static List<ServiceParameter> addParameter( List<ServiceParameter> parameters, UUID entityId ) {

        if ( parameters == null ) {
            parameters = new ArrayList<ServiceParameter>();
        }

        if ( entityId == null ) {
            return parameters;
        }

        ServiceParameter p = new IdParameter( entityId );
        parameters.add( p );
        return parameters;
    }


    public static List<ServiceParameter> addParameter( List<ServiceParameter> parameters, String name ) {

        if ( parameters == null ) {
            parameters = new ArrayList<ServiceParameter>();
        }

        if ( name == null ) {
            return parameters;
        }

        if ( "all".equals( name ) ) {
            Query query = new Query();
            ServiceParameter p = new QueryParameter( query );
            parameters.add( p );
            return parameters;
        }

        ServiceParameter p = new NameParameter( name );
        parameters.add( p );
        return parameters;
    }


    public static List<ServiceParameter> addParameter( List<ServiceParameter> parameters, Query query ) {

        if ( parameters == null ) {
            parameters = new ArrayList<ServiceParameter>();
        }

        if ( query == null ) {
            return parameters;
        }

        if ( lastParameterIsQuery( parameters ) ) {
            logger.error( "Adding two queries in a row" );
        }

        ServiceParameter p = new QueryParameter( query );
        parameters.add( p );
        return parameters;
    }


    public static List<ServiceParameter> addParameters( List<ServiceParameter> parameters, Object... params )
            throws Exception {

        if ( parameters == null ) {
            parameters = new ArrayList<ServiceParameter>();
        }

        if ( params == null ) {
            return parameters;
        }

        for ( Object param : params ) {
            if ( param instanceof UUID ) {
                addParameter( parameters, ( UUID ) param );
            }
            else if ( param instanceof String ) {
                addParameter( parameters, ( String ) param );
            }
            else if ( param instanceof Query ) {
                addParameter( parameters, ( Query ) param );
            }
        }

        return parameters;
    }


    public static List<ServiceParameter> parameters( Object... params ) throws Exception {
        return addParameters( null, params );
    }


    public static boolean firstParameterIsName( List<ServiceParameter> parameters ) {
        if ( !isEmpty( parameters ) ) {
            return parameters.get( 0 ).isName();
        }
        return false;
    }


    public static boolean lastParameterIsName( List<ServiceParameter> parameters ) {
        if ( !isEmpty( parameters ) ) {
            return parameters.get( parameters.size() - 1 ).isName();
        }
        return false;
    }


    public static boolean firstParameterIsQuery( List<ServiceParameter> parameters ) {
        if ( !isEmpty( parameters ) ) {
            return parameters.get( 0 ).isQuery();
        }
        return false;
    }


    public static boolean lastParameterIsQuery( List<ServiceParameter> parameters ) {
        if ( !isEmpty( parameters ) ) {
            return parameters.get( parameters.size() - 1 ).isQuery();
        }
        return false;
    }


    public static boolean firstParameterIsId( List<ServiceParameter> parameters ) {
        if ( !isEmpty( parameters ) ) {
            return parameters.get( 0 ).isId();
        }
        return false;
    }


    public static boolean lastParameterIsId( List<ServiceParameter> parameters ) {
        if ( !isEmpty( parameters ) ) {
            return parameters.get( parameters.size() - 1 ).isId();
        }
        return false;
    }


    public static ServiceParameter firstParameter( List<ServiceParameter> parameters ) {
        if ( !isEmpty( parameters ) ) {
            return parameters.get( 0 );
        }

        return null;
    }


    public static boolean moreParameters( List<ServiceParameter> parameters ) {
        return moreParameters( parameters, true );
    }


    public static boolean moreParameters( List<ServiceParameter> parameters, boolean ignoreTrailingQueries ) {
        if ( isEmpty( parameters ) ) {
            return false;
        }
        if ( ignoreTrailingQueries ) {
            for ( ServiceParameter parameter : parameters ) {
                if ( !( parameter instanceof QueryParameter ) ) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }


    public static int parameterCount( List<ServiceParameter> parameters ) {
        return parameterCount( parameters, false );
    }


    public static int parameterCount( List<ServiceParameter> parameters, boolean ignoreTrailingQueries ) {
        if ( isEmpty( parameters ) ) {
            return 0;
        }
        int count = parameters.size();
        if ( ignoreTrailingQueries ) {
            count = 0;
            for ( ServiceParameter parameter : parameters ) {
                if ( !( parameter instanceof QueryParameter ) ) {
                    count++;
                }
                else {
                    return count;
                }
            }
        }
        return count;
    }


    public static ServiceParameter dequeueParameter( List<ServiceParameter> parameters ) {
        return dequeue( parameters );
    }


    public static void queueParameter( List<ServiceParameter> parameters, ServiceParameter parameter ) {
        parameters = queue( parameters, parameter );
    }


    public static List<ServiceParameter> mergeQueries( Query query, List<ServiceParameter> parameters ) {
        while ( firstParameterIsQuery( parameters ) ) {
            parameters = dequeueCopy( parameters );
        }
        return parameters;
    }


    public static List<ServiceParameter> filter( List<ServiceParameter> parameters,
                                                 Map<List<String>, List<String>> replaceParameters ) {
        if ( replaceParameters == null ) {
            return parameters;
        }
        if ( ( parameters == null ) || ( parameters.size() == 0 ) ) {
            return parameters;
        }
        for ( Entry<List<String>, List<String>> replaceSet : replaceParameters.entrySet() ) {
            if ( parameters.size() < replaceSet.getKey().size() ) {
                continue;
            }
            boolean found = true;
            for ( int i = 0; i < replaceSet.getKey().size(); i++ ) {
                String matchStr = replaceSet.getKey().get( i );
                ServiceParameter param = parameters.get( i );
                if ( matchStr.equals( "$id" ) && ( ( param instanceof IdParameter )
                        || ( param instanceof NameParameter ) ) ) {
                    continue;
                }
                else if ( matchStr.equals( "$query" ) && ( param instanceof QueryParameter ) ) {
                    continue;
                }
                else if ( matchStr.equalsIgnoreCase( param.getName() ) ) {
                    continue;
                }
                found = false;
                break;
            }
            if ( !found ) {
                continue;
            }
            ArrayList<ServiceParameter> p = new ArrayList<ServiceParameter>();
            for ( String name : replaceSet.getValue() ) {
                if ( name.startsWith( "\\" ) ) {
                    int i = Integer.parseInt( name.substring( 1 ) );
                    p.add( parameters.get( i ) );
                }
                else {
                    p.add( new NameParameter( name ) );
                }
            }
            p.addAll( parameters.subList( replaceSet.getKey().size(), parameters.size() ) );
            return p;
        }
        return parameters;
    }


    public static class IdParameter extends ServiceParameter {
        UUID entityId;


        public IdParameter( UUID entityId ) {
            this.entityId = entityId;
        }


        @Override
        public UUID getId() {
            return entityId;
        }


        @Override
        public boolean isId() {
            return true;
        }


        @Override
        public String toString() {
            return entityId.toString();
        }


        @Override
        public Identifier getIdentifier() {
            return Identifier.from( entityId );
        }
    }


    public static class NameParameter extends ServiceParameter {
        String name;


        public NameParameter( String name ) {
            name = name.trim().toLowerCase();
            this.name = name;
        }


        @Override
        public String getName() {
            return name;
        }


        @Override
        public boolean isName() {
            return true;
        }


        @Override
        public String toString() {
            return name;
        }


        @Override
        public Identifier getIdentifier() {
            return Identifier.from( name );
        }
    }


    public static class QueryParameter extends ServiceParameter {
        Query query;


        public QueryParameter( Query query ) {
            this.query = query;
        }


        @Override
        public Query getQuery() {
            return query;
        }


        @Override
        public boolean isQuery() {
            return true;
        }


        @Override
        public String toString() {
            String queryStr = query.toString();
            if ( isNotBlank( queryStr ) ) {
                return queryStr;
            }
            return "";
        }


        @Override
        public Identifier getIdentifier() {
            return null;
        }
    }
}
