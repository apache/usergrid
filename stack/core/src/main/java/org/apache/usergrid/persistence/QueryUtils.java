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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.usergrid.persistence.index.query.Query;
import static org.apache.usergrid.utils.ClassUtils.cast;
import org.apache.usergrid.utils.JsonUtils;

import org.apache.usergrid.utils.ListUtils;


/**
 * Utilities to deal with query extraction and generation
 *
 * @author zznate
 */
public class QueryUtils {

    public static final String PARAM_QL = "ql";
    public static final String PARAM_Q = "q";
    public static final String PARAM_QUERY = "query";


    public static String queryStrFrom( Map<String, List<String>> params ) {
        if ( params.containsKey( PARAM_QL ) ) {
            return ListUtils.first( params.get( PARAM_QL ) );
        }
        else if ( params.containsKey( PARAM_Q ) ) {
            return ListUtils.first( params.get( PARAM_Q ) );
        }
        else if ( params.containsKey( PARAM_QUERY ) ) {
            return ListUtils.first( params.get( PARAM_QUERY ) );
        }
        return null;
    }

    public static List<Object> getSelectionResults( Query q, Results rs ) {

        List<Entity> entities = rs.getEntities();
        if ( entities == null ) {
            return null;
        }

        if ( !q.hasSelectSubjects() ) {
            return cast( entities );
        }

        List<Object> results = new ArrayList<Object>();

        for ( Entity entity : entities ) {
            if ( q.isMergeSelectResults() ) {
                boolean include = false;
                Map<String, Object> result = new LinkedHashMap<String, Object>();
                Map<String, String> selects = q.getSelectAssignments();
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
                Set<String> selects = q.getSelectSubjects();
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


    public static Object getSelectionResult( Query q, Results rs ) {
        List<Object> r = QueryUtils.getSelectionResults( q, rs );
        if ( ( r != null ) && ( r.size() > 0 ) ) {
            return r.get( 0 );
        }
        return null;
    }

}
