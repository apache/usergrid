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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.usergrid.persistence.index.SelectFieldMapping;
import org.apache.usergrid.utils.JsonUtils;
import org.apache.usergrid.utils.ListUtils;

import static org.apache.usergrid.utils.ClassUtils.cast;


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


    /**
     * When a query has select fields, parse the results into a result set by the field mappings
     * @param q
     * @param rs
     * @return
     */
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


            Collection<SelectFieldMapping> selects = q.getSelectAssignments();
            for ( SelectFieldMapping select : selects ) {
                Object obj = JsonUtils.select( entity, select.getSourceFieldName(), false );
                if ( obj != null ) {
                    Map<String, Object> result = new LinkedHashMap<String, Object>();
                    result.put( select.getTargetFieldName(), obj );
                    results.add( result );
                }
            }
        }

        if ( results.size() == 0 ) {
            return null;
        }

        return results;
    }
}
