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
package org.apache.usergrid.persistence.index;

import org.apache.usergrid.persistence.index.query.ParsedQuery;
import org.apache.usergrid.persistence.index.query.tree.Operand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryAnalyzer {

    public static final String v_predicate_count = "sort_predicate_count_exceeded";
    public static final String v_operand_count = "operand_count_exceeded";
    public static final String v_large_collection = "large_collection_size_bytes";
    public static final String v_large_index = "large_index_size_bytes";
    public static final String v_full_collection_sort = "full_collection_sort";

    public static final String k_violation = "violation";
    public static final String k_limit = "limit";
    public static final String k_actual = "actual";


    public static List<Map<String, Object>> analyze(final ParsedQuery parsedQuery, final long collectionSizeInBytes,
                               final long indexSizeInBytes, final IndexFig indexFig ) {

        List<Map<String, Object>> violations = new ArrayList<>();

        // get configured breaker values
        final int errorPredicateCount = indexFig.getQueryBreakerErrorSortPredicateCount();
        final int errorOperandCount = indexFig.getQueryBreakerErrorOperandCount();
        final long errorCollectionSizeBytes = indexFig.getQueryBreakerErrorCollectionSizeBytes();
        final long errorIndexSizeBytes = indexFig.getQueryBreakerErrorIndexSizeBytes();


        // get the actual values to compare against the configured enforcement values
        int queryPredicatesSize = parsedQuery.getSortPredicates().size();
        int queryOperandCount = getTotalChildCount(parsedQuery.getRootOperand());

        // large indexes can cause issues, this is never returned from the API and only logged
        if( indexSizeInBytes > errorIndexSizeBytes ){
            violations.add(new HashMap<String, Object>(3){{
                put(k_violation, v_large_index);
                put(k_limit, errorIndexSizeBytes);
                put(k_actual, indexSizeInBytes);
            }});
        }

        // large collections mean that sorts and other complex queries can impact the query service (Elasticsearch)
        if (collectionSizeInBytes > errorCollectionSizeBytes ){
            violations.add(new HashMap<String, Object>(3){{
                put(k_violation, v_large_collection);
                put(k_limit, errorCollectionSizeBytes);
                put(k_actual, collectionSizeInBytes);
            }});

            // query like "select * order by created asc"
            if(parsedQuery.getSelectFieldMappings().size() < 1 &&
                !parsedQuery.getOriginalQuery().toLowerCase().contains("where") &&
                parsedQuery.getSortPredicates().size() > 0 ){

                violations.add(new HashMap<String, Object>(3){{
                    put(k_violation, v_full_collection_sort);
                    put(k_limit, null);
                    put(k_actual, null);
                }});
            }

        }

        // complex queries can be determined from the # of operands and sort predicates
        if ( queryPredicatesSize > errorPredicateCount){
            violations.add(new HashMap<String, Object>(3){{
                put(k_violation, v_predicate_count);
                put(k_limit, errorPredicateCount);
                put(k_actual, queryPredicatesSize);
            }});
        }
        if (queryOperandCount > errorOperandCount){
            violations.add(new HashMap<String, Object>(3){{
                put(k_violation, v_operand_count);
                put(k_limit, errorOperandCount);
                put(k_actual, queryOperandCount);
            }});
        }

        return violations;

    }

    public static String violationsAsString(List<Map<String, Object>> violations, String originalQuery){

        final StringBuilder logMessage = new StringBuilder();
        logMessage.append( "QueryAnalyzer Violations Detected [").append(violations.size()).append("]: [" );
        violations.forEach(violation -> {

            final StringBuilder violationMessage = new StringBuilder();
            violation.forEach((k,v) -> {
                violationMessage.append(k).append(":").append(v).append(",");

            });
            violationMessage.deleteCharAt(violationMessage.length()-1);
            logMessage.append(" (").append(violationMessage).append(") ");
        });
        logMessage.append("]");
        logMessage.append(" [Original Query: ").append(originalQuery).append("]");
        return logMessage.toString();

    }

    private static int getTotalChildCount(Operand rootOperand){
        int count = 0;
        if( rootOperand != null) {
            count ++;
            if (rootOperand.getChildren() != null) {
                for (Object child : rootOperand.getChildren()) {
                    if (child instanceof Operand) {
                        count += getTotalChildCount((Operand) child);
                    }
                }
            }
        }
        return count;
    }

}
