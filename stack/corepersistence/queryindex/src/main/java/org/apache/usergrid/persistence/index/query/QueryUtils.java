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


import java.util.List;
import java.util.Map;
import org.apache.usergrid.persistence.index.utils.ListUtils;

 
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
}
