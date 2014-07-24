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
package org.apache.usergrid.persistence;


import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

//import org.codehaus.jackson.map.annotate.JsonSerialize;
//import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;


public class AggregateCounterSet {
    private String name;
    private UUID user;
    private UUID group;
    private UUID queue;
    private String category;
    private List<AggregateCounter> values;


    public AggregateCounterSet( String name, UUID user, UUID group, String category, List<AggregateCounter> values ) {
        this.name = name;
        this.user = user;
        this.group = group;
        this.category = category;
        this.values = values;
    }


    public AggregateCounterSet( String name, UUID queue, String category, List<AggregateCounter> values ) {
        this.name = name;
        setQueue( queue );
        this.category = category;
        this.values = values;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public UUID getUser() {
        return user;
    }


    public void setUser( UUID user ) {
        this.user = user;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public UUID getGroup() {
        return group;
    }


    public void setGroup( UUID group ) {
        this.group = group;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getCategory() {
        return category;
    }


    public void setCategory( String category ) {
        this.category = category;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getName() {
        return name;
    }


    public void setName( String name ) {
        this.name = name;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public List<AggregateCounter> getValues() {
        return values;
    }


    public void setValues( List<AggregateCounter> values ) {
        this.values = values;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public UUID getQueue() {
        return queue;
    }


    public void setQueue( UUID queue ) {
        this.queue = queue;
    }
}
