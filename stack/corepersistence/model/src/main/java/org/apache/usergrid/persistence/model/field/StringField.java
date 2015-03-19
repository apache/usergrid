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
package org.apache.usergrid.persistence.model.field;

/**
 * A String field
 */
public class StringField extends AbstractField<String> {


    public static final int MAX_LENGTH = 500;


    public StringField(String name, String value) {
        super(name, value);
    }

    public StringField(String name, String value, boolean unique) {
        super(name, value, unique);
    }

    public StringField() {
    }


    @Override
    public FieldTypeName getTypeName() {
                return FieldTypeName.STRING;
            }


    @Override
    public void validate() {
        //not unique, don't care
        if(!unique){
            return;
        }

        if(value != null && value.length() > MAX_LENGTH ){
            throw new IllegalArgumentException( "Your unique field '" + name + "' cannot be longer than " + MAX_LENGTH + " characters" );
        }



    }
}
