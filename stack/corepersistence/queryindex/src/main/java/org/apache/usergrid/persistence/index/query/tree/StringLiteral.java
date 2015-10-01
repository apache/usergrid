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

package org.apache.usergrid.persistence.index.query.tree;


import org.antlr.runtime.ClassicToken;
import org.antlr.runtime.Token;

import static org.apache.commons.lang.StringUtils.removeEnd;


/** @author tnine */
public class StringLiteral extends Literal<String> {

    private String value;
    private String finishValue;


    /**
     * @param t
     */
    public StringLiteral( Token t ) {
        super( t );
        String newValue = t.getText();
        newValue = newValue.substring( 1, newValue.length() - 1 );

        parseValue( newValue );
    }


    public StringLiteral( String value ) {
        super( new ClassicToken( 1, value ) );
        parseValue( value );
    }


    /** Parse the value and set the optional end value */
    private void parseValue( String value ) {

        this.value = value.trim().toLowerCase();

        if ( "*".equals( value ) ) {
            this.value = null;
            this.finishValue = null;
            return;
        }

//      removing this because it breaks queries like "select * where name = "fred*"
//
//        if ( value != null && value.endsWith( "*" ) ) {
//            this.value = removeEnd( value.toString(), "*" );
//            finishValue = this.value + "\uFFFF";
//        }
//        // set the end value to the same as the start value
//        else {
//            finishValue = value;
//        }

        finishValue = value;
    }


    /** If this were a string literal */
    public String getEndValue() {
        return this.finishValue;
    }


    public String getValue() {
        return this.value;
    }
}
