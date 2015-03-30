/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.index;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;
import org.apache.usergrid.persistence.model.entity.Id;

/**
 * Get search type
 */
public class SearchType{
    private final String type;

    private SearchType( final String type ) {this.type = type;}

    public static SearchType fromType( final String type ) {
        return new SearchType( type );
    }


    public static SearchType fromId( final Id id ) {
        return new SearchType( id.getType() );
    }

    public String getTypeName(ApplicationScope applicationScope) {
            return  IndexingUtils.getType(applicationScope, type);
    }

    public String[] getTypeNames(ApplicationScope applicationScope) {
        final String[] typeNames =   new String[]{type , IndexingUtils.getType(applicationScope, type)};
        return typeNames;
    }
}
