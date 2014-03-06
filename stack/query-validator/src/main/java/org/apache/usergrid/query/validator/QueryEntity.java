/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.query.validator;

import org.apache.usergrid.persistence.DynamicEntity;

import java.util.Map;

/**
 * @author Sungju Jin
 */
public class QueryEntity extends DynamicEntity {

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if(!(obj instanceof QueryEntity))
            return false;

        QueryEntity other = (QueryEntity)obj;
        Map<String, Object> properties = this.getProperties();
        Map<String, Object> otherProperties = other.getProperties();
        for(String key : properties.keySet()) {
            if( "created".equals(key) || "modified".equals(key) )
                continue;

            Object value = properties.get(key);
            Object otherValue = otherProperties.get(key);

            if(value.getClass() == Boolean.class) {
                value = (Boolean)value ? 1 : 0;
            }

            if(otherValue.getClass() == Boolean.class) {
                otherValue = (Boolean)otherValue ? 1 : 0;
            }

            if( otherValue == null || !value.equals(otherValue) )
                return false;
        }
        return true;
    }
}
