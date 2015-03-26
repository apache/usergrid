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
package org.apache.usergrid.persistence.collection.exception;


import java.util.Map;

import org.apache.usergrid.persistence.collection.MvccEntity;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.model.field.Field;


/**
 * Indicates that one or more unique field checks failed.
 */
public class WriteUniqueVerifyException extends CollectionRuntimeException {
    private Map<String, Field> violations;


    public WriteUniqueVerifyException( MvccEntity entity, ApplicationScope scope, Map<String, Field> violations ) {
        super( entity, scope, "Error: one or more duplicate fields detected");
        this.violations = violations;
    }

    /**
     * Get map of Fields in violation, keyed by field name.
     */
    public Map<String, Field> getVioliations() {
        return violations;
    }
}
