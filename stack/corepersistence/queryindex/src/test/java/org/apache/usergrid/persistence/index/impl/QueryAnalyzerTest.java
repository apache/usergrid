/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.index.impl;


import java.util.List;
import java.util.Map;

import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.QueryAnalyzer;
import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.ParsedQueryBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class QueryAnalyzerTest extends BaseIT {

    private static final Logger logger = LoggerFactory.getLogger(QueryAnalyzerTest.class);

    private IndexFig fig;

    @Before
    public void setup() {

        // the mock will return 0/empty values for all fig items
        fig = mock ( IndexFig.class );

    }

    @Test
    public void testNoViolations() throws Throwable {

        when ( fig.getQueryBreakerErrorSortPredicateCount() ).thenReturn( 5 );
        when ( fig.getQueryBreakerErrorOperandCount() ).thenReturn( 5 );
        when ( fig.getQueryBreakerErrorIndexSizeBytes() ).thenReturn( 5L );
        when ( fig.getQueryBreakerErrorCollectionSizeBytes() ).thenReturn( 5L );

        ParsedQuery parsedQuery;
        List<Map<String,Object>> violations;


        parsedQuery = ParsedQueryBuilder.build("select * order by created asc");
        violations = QueryAnalyzer.analyze(parsedQuery, 1, 1, fig );
        assertEquals(0, violations.size());

        parsedQuery = ParsedQueryBuilder.build("select name='value' order by created asc");
        violations = QueryAnalyzer.analyze(parsedQuery, 1, 1, fig );
        assertEquals(0, violations.size());

        parsedQuery = ParsedQueryBuilder.build("where name='value'");
        violations = QueryAnalyzer.analyze(parsedQuery, 1, 1, fig );
        assertEquals(0, violations.size());


    }


    @Test
    public void IndexSizeViolation() throws Throwable {


        ParsedQuery parsedQuery = ParsedQueryBuilder.build("select created order by created asc");
        List<Map<String,Object>> violations;
        violations = QueryAnalyzer.analyze(parsedQuery, 0, 1, fig );
        boolean violationExists = violationExists(violations, QueryAnalyzer.v_large_index);

        if(!violationExists){
            fail("Index Size Violation should be present");
        }

    }

    @Test
    public void collectionSizeViolation() throws Throwable {

        // the sort violation is only tripped when the collection size warning is tripped
        when ( fig.getQueryBreakerErrorCollectionSizeBytes() ).thenReturn( 0L );

        ParsedQuery parsedQuery = ParsedQueryBuilder.build("select created order by created asc");
        List<Map<String,Object>> violations;
        violations = QueryAnalyzer.analyze(parsedQuery, 1, 1, fig );
        boolean violationExists = violationExists(violations, QueryAnalyzer.v_large_collection);

        if(!violationExists){
            fail("Collection Size Violation should be present");
        }

    }


    @Test
    public void fullSortViolation() throws Throwable {

        ParsedQuery parsedQuery = ParsedQueryBuilder.build("select * order by created asc");
        List<Map<String,Object>> violations;
        violations = QueryAnalyzer.analyze(parsedQuery, 1, 1, fig );
        boolean violationExists = violationExists(violations, QueryAnalyzer.v_full_collection_sort);

        if(!violationExists){
            fail("Full Collection Sort Violation should be present");
        }

    }

    @Test
    public void operandCountViolation() throws Throwable {

        ParsedQuery parsedQuery = ParsedQueryBuilder.build("where name='value'");
        List<Map<String,Object>> violations;
        violations = QueryAnalyzer.analyze(parsedQuery, 0, 0, fig );
        boolean violationExists = violationExists(violations, QueryAnalyzer.v_operand_count);

        if(!violationExists){
            fail("Operand Count Violation should be present");
        }

    }

    @Test
    public void predicateCountViolation() throws Throwable {

        ParsedQuery parsedQuery = ParsedQueryBuilder.build("where name='value'");
        List<Map<String,Object>> violations;
        violations = QueryAnalyzer.analyze(parsedQuery, 0, 0, fig );
        boolean violationExists = violationExists(violations, QueryAnalyzer.v_operand_count);

        if(!violationExists){
            fail("Operand Count Violation should be present");
        }

    }



    private boolean violationExists(final List<Map<String,Object>> violations, final String expectedViolation){
        for ( Map<String, Object> violation : violations ){
            if (violation.get(QueryAnalyzer.k_violation) == expectedViolation){
                return true;
            }

        }
        return false;
    }

}



