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
package org.apache.usergrid.persistence.index.exceptions;


/**
 * An exception thrown when a query cannot be parsed
 *
 * @author tnine
 */
public class QueryParseException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;


    /**
     *
     */
    public QueryParseException() {
        super();
    }


    /**
     * @param arg0
     * @param arg1
     */
    public QueryParseException( String arg0, Throwable arg1 ) {
        super( arg0, arg1 );
    }


    /**
     * @param arg0
     */
    public QueryParseException( String arg0 ) {
        super( arg0 );
    }


    /**
     * @param arg0
     */
    public QueryParseException( Throwable arg0 ) {
        super( arg0 );
    }
}
