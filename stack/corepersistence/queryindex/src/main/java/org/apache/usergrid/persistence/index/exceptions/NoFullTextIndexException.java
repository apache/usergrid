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
 * Thrown when the user attempts to perform a "contains" operation on a field that isn't full text indexed
 *
 * @author tnine
 */
public class NoFullTextIndexException extends IndexException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    final String entityType;
    final String propertyName;


    public NoFullTextIndexException( String entityType, String propertyName ) {
        super( "Entity '" + entityType + "' with property named '" + propertyName
                + "' is not full text indexed.  You cannot use the 'contains' operand on this field" );
        this.entityType = entityType;
        this.propertyName = propertyName;
    }


    public String getEntityType() {
        return entityType;
    }


    public String getPropertyName() {
        return propertyName;
    }
}
