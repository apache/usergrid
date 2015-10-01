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

package org.apache.usergrid.rest.test.resource.model;

import org.apache.usergrid.persistence.index.utils.MapUtils;

import java.util.Map;

/**
 * Provide Guidance on ActivityEntity
 */
public class ActivityEntity extends Entity {
    public ActivityEntity(String email, String verb, String content){
        this.chainPut("content",content).chainPut("verb",verb).chainPut("email",email);
    }
    public ActivityEntity() {
        this.putAll(new MapUtils.HashMapBuilder<String, Object>());
    }
    public ActivityEntity(Map<String,Object> map){
        this.putAll(map);
    }

    public ActivityEntity putActor(Map<String, Object> actorPost) {
        this.put("actor",actorPost);
        return this;
    }

    public Map<String, Object> getActor() {
        return (Map<String, Object>) this.get("actor");
    }
}
