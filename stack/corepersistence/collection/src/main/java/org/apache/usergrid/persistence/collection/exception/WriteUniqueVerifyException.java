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
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.field.Field;


/**
 * Indicates that one or more unique field checks failed.
 */
public class WriteUniqueVerifyException extends CollectionRuntimeException {
    private Map<String, Field> violations;

    
    public WriteUniqueVerifyException( Entity entity, CollectionScope scope, Map<String, Field> violations ) {
        super( entity, scope, "Error: one or more duplicate fields detected");
        this.violations = violations;
    }


    public WriteUniqueVerifyException( Entity entity, CollectionScope scope, 
            final String message, final Throwable cause ) {
        super( entity, scope, message, cause );
    }


    public WriteUniqueVerifyException( Entity entity, CollectionScope scope, final Throwable cause ) {
        super( entity, scope, cause );
    }


    public WriteUniqueVerifyException( Entity entity, CollectionScope scope, 
            final String message, final Throwable cause, final boolean enableSuppression,
            final boolean writableStackTrace ) {
        super( entity, scope, message, cause, enableSuppression, writableStackTrace );
    }

    /**
     * Get map of Fields in violation, keyed by field name.
     */
    public Map<String, Field> getVioliations() {
        return violations;
    }
}
