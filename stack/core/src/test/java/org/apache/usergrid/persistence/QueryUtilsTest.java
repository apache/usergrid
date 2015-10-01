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


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


/** @author zznate */

public class QueryUtilsTest {

    private static final String FAKE_QL = "select color from cat";

    private Map<String, List<String>> params = new HashMap<String, List<String>>();


    @Test
    public void extractQueryAlias() {
        params.put( QueryUtils.PARAM_QL, Arrays.asList( FAKE_QL ) );
        String query = QueryUtils.queryStrFrom( params );
        assertEquals( FAKE_QL, query );
        params.clear();

        params.put( QueryUtils.PARAM_Q, Arrays.asList( FAKE_QL ) );
        query = QueryUtils.queryStrFrom( params );
        assertEquals( FAKE_QL, query );
        params.clear();

        params.put( QueryUtils.PARAM_QUERY, Arrays.asList( FAKE_QL ) );
        query = QueryUtils.queryStrFrom( params );
        assertEquals( FAKE_QL, query );
        params.clear();
    }
}
