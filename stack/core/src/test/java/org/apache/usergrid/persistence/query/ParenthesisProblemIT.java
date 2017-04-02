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

package org.apache.usergrid.persistence.query;

import org.apache.usergrid.AbstractCoreIT;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Results;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;


// Created for https://issues.apache.org/jira/browse/USERGRID-1099
public class ParenthesisProblemIT extends AbstractCoreIT {
    private static final Logger logger = LoggerFactory.getLogger( ParenthesisProblemIT.class );

    @Test
    public void parenthesisProblem1() throws Exception {
        // C = c and ( A > a or B = false )
        parenthesisProblem(1, "color = 'tabby' and (age > 7 or large = true)");
    }

    @Test
    public void parenthesisProblem2() throws Exception {
        // same as #1 except for order of things listed in 'and' operation
        // ( A > a or B = false ) and C = c
        parenthesisProblem(1, "select * where (age > 7 or large = true) and color = 'tabby'");
    }

    private void parenthesisProblem( int expect, String query ) throws Exception {

        EntityManager em =app.getEntityManager();

        em.create( "cat", new HashMap<String, Object>() {{
            put("name","enzo");
            put("color","orange");
            put("large", true);
            put("age",6);
        }});

        em.create( "cat", new HashMap<String, Object> () {{
            put("name","marquee");
            put("color","grey");
            put("large",false);
            put("age",8);
        }});

        em.create( "cat", new HashMap<String, Object> () {{
            put("name","bertha");
            put("color","tabby");
            put("large",true);
            put("age",1);
        }});

        app.waitForQueueDrainAndRefreshIndex();

        final Results entities = em.searchCollection( em.getApplicationRef(), "cats", Query.fromQL(query));

        assertEquals(expect, entities.size());
    }
}
