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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.services.ServiceParameter;


/**
 * Classy class class.
 */
public class QueryParameters {
    private String query;
    private String cursor;
    private UUID start;
    private Integer limit;
    private String connections;
    private Map<String,String> formPostData = new HashMap<String,String>(  );

    public QueryParameters() {
    }

    public UUID getStart() {
        return start;
    }

    public QueryParameters setStart(UUID start) {
        this.start = start;
        return this;
    }

    public String getCursor() {
        return cursor;
    }

    public QueryParameters setCursor(String cursor) {
        this.cursor = cursor;
        return this;
    }

    public String getQuery() {
        return query;
    }

    public QueryParameters setQuery(String query) {
        this.query = query;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public QueryParameters setLimit(int limit) {
        this.limit = new Integer(limit);
        return this;
    }

    public String getConnections() { return connections; }

    public QueryParameters setConnections(String connections) {
        this.connections = connections;
        return this;
    }

    public Map<String,String> getFormPostData(){
        return formPostData;
    }

    public QueryParameters setKeyValue(String key, String value){
        this.formPostData.put(key,value);
        return this;
    }


    public QueryParameters addParam(String key, String value) {
        formPostData.put(key,value);
        return this;
    }
}
