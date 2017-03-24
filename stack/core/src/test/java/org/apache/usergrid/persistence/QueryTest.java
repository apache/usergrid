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


import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class QueryTest {

    @Test
    public void testQueryParamsWithPlus(){

        String qlString = "select * where email='test+value@usergrid.com'";

        Map<String,List<String>> queryParams = new HashMap<>();
        queryParams.put("ql", Collections.singletonList(qlString) );
        Query query = Query.fromQueryParams(queryParams);

        assertEquals(qlString, query.getQl().get());

    }

    @Test
    public void testQueryParamsWithUrlEncodedPlus(){

        String qlString = "select * where email='test+value@usergrid.com'";
        Map<String,List<String>> queryParams = new HashMap<>();
        queryParams.put("ql", Collections.singletonList(qlString.replace("+", "%2b")));
        Query query = Query.fromQueryParams(queryParams);

        assertEquals(qlString, query.getQl().get());

    }

}
