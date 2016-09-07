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
package org.apache.usergrid.tools.export;

import org.apache.usergrid.persistence.Entity;

import java.util.Map;

public class ExportEntity {
    private String organization;
    private String application;
    private Entity entity;
    private Map<String, Object> dictionaries;
    public ExportEntity( String organization, String application, Entity entity, Map<String, Object> dictionaries ) {
        this.organization = organization;
        this.application = application;
        this.entity = entity;
        this.dictionaries = dictionaries;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Map<String, Object> getDictionaries() {
        return dictionaries;
    }

    public void setDictionaries(Map<String, Object> dictionaries) {
        this.dictionaries = dictionaries;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}
