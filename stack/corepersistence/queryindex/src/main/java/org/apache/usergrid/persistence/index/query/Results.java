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
package org.apache.usergrid.persistence.index.query;


import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;


@XmlRootElement
public class Results implements Iterable<Entity> {

    final List<Id> ids;
    final Query query;

    String cursor = null;
    List<Entity> entities = null;
    List<EntityRef> refs = null;

    public Results(Query query, List<Id> ids, List<Entity> entities) {
        this.query = query;
        this.ids = ids;
        this.entities = entities;
    }


    public boolean hasCursor() {
        return cursor != null;
    }


    public String getCursor() {
        return cursor;
    }


    public void setCursor(String cursor) {
        this.cursor = cursor;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Query getQuery() {
        return query;
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<Id> getIds() {
        return Collections.unmodifiableList( ids );
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    @SuppressWarnings("unchecked")
    public List<EntityRef> getRefs() {
        if ( entities == null ) {
            getEntities();
        }
        return Collections.unmodifiableList( refs );
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<Entity> getEntities() {
        return Collections.unmodifiableList( entities );
    }


    public int size() {
        return ids.size();
    }


    public boolean isEmpty() {
        return ids.isEmpty();
    }


    @Override
    public Iterator<Entity> iterator() {
        return getEntities().iterator();
    }

    public void setIds(List<Id> ids) {
    }
}
