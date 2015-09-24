/*
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.apache.usergrid.persistence.index.query;


import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;

import org.apache.usergrid.persistence.index.exceptions.QueryParseException;
import org.apache.usergrid.persistence.index.query.tree.CpQueryFilterLexer;
import org.apache.usergrid.persistence.index.query.tree.CpQueryFilterParser;


/**
 * A utility class that will parse our query, then return it's parsed representation
 */
public class ParsedQueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger( ParsedQueryBuilder.class );


    /**
     * Generate a parsedQuery from the ql
     */
    public static ParsedQuery build( final String ql ) throws QueryParseException {
        if ( StringUtils.isEmpty( ql ) ) {
            return null;
        }
        logger.debug( "Processing raw query: " + ql );

        final String trimmedLowercaseQuery = ql.trim().toLowerCase();


        //the output query after post processing
        final String outputQuery;

        //it doesn't start with select, rewrite it to be a correct query grammar
        if ( !trimmedLowercaseQuery.startsWith( "select" ) ) {

            //just an order by, add the select
            //just starts with a where, add the select
            if ( trimmedLowercaseQuery.startsWith( "order by" ) || trimmedLowercaseQuery.startsWith( "where" )) {
                outputQuery = "select * " + trimmedLowercaseQuery;
            }


            //junk, bail
            else {
               outputQuery = "select * where " + trimmedLowercaseQuery;
            }
        }
        else {
            outputQuery = trimmedLowercaseQuery;
        }

        ANTLRStringStream in = new ANTLRStringStream( outputQuery );
        CpQueryFilterLexer lexer = new CpQueryFilterLexer( in );
        CommonTokenStream tokens = new CommonTokenStream( lexer );
        CpQueryFilterParser parser = new CpQueryFilterParser( tokens );


        try {
            final ParsedQuery query = parser.ql().parsedQuery;
            query.setOriginalQuery( ql );
            return query;
        }
        catch ( RecognitionException e ) {
            logger.error( "Unable to parse \"{}\"", ql, e );

            int index = e.index;
            int lineNumber = e.line;
            Token token = e.token;

            String message = String.format(
                    "The query cannot be parsed. The token '%s' at " + "column %d on line %d cannot be " + "parsed",
                    token.getText(), index, lineNumber );

            throw new QueryParseException( message, e );
        }
    }
}
