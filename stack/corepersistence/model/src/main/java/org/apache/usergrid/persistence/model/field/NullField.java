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
 * Created by russo on 1/14/16.
 */
public final class NullField extends AbstractField<Object>{


    public NullField(String name) {
        super(name, null);
    }

    public NullField(String name, boolean unique) {
        super(name, null, unique);
    }

    public NullField() {
    }

    /**
     * This type of field is never unique
     */
    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public final FieldTypeName getTypeName() {
        return FieldTypeName.NULL;
    }

}
