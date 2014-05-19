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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.index.impl.CandidateResult;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.entity.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@XmlRootElement
public class Results implements Iterable<Entity> {

    private static final Logger log = LoggerFactory.getLogger(Results.class);

    private String cursor = null;

    private final Query query;
    private final List<Id> ids = new ArrayList<Id>();

    private Entity entity = null;
    private List<Entity> entities = null;

    private final List<CandidateResult> candidates;

    final EntityCollectionManagerFactory ecmf;


    public Results( Query query, List<CandidateResult> candidates, EntityCollectionManagerFactory ecmf ) {
        this.query = query;
        this.candidates = candidates;
        this.ecmf = ecmf;
        for ( CandidateResult candidate : candidates ) {
            ids.add( candidate.getId() );
        }
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
        return Collections.unmodifiableList(ids);
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<Entity> getEntities() {
        return getEntities(false);
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public List<Entity> getEntities(Boolean takeAllVersions) {

        if ( entities == null ) {

            entities = new ArrayList<Entity>();

            EntityCollectionManager ecm = null;

            for ( CandidateResult candidate : candidates ) {

                Entity entity = ecm.load( candidate.getId() ).toBlockingObservable().last();
                if ( !takeAllVersions && candidate.getVersion().compareTo(entity.getVersion()) == -1) {
                    log.debug("   Stale hit {} version {}", entity.getId(), entity.getVersion() );
                    continue;
                }

                entities.add(entity);
            }
        }

        return Collections.unmodifiableList( entities );
    }


    @JsonSerialize(include = Inclusion.NON_NULL)
    public Entity getEntity() {
        if ( size() > 0 ) {
            return getEntities().get(0);
        }
        return null;
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
}
