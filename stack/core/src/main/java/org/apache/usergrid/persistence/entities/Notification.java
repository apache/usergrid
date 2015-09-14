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
package org.apache.usergrid.persistence.entities;


import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.usergrid.persistence.*;
import org.apache.usergrid.persistence.annotations.EntityCollection;
import org.apache.usergrid.persistence.annotations.EntityProperty;
import org.apache.usergrid.persistence.entities.Device;
import org.apache.usergrid.persistence.index.query.Identifier;

import static org.apache.usergrid.utils.InflectionUtils.pluralize;

/**
 * The entity class for representing Notifications.
 */
@XmlRootElement
public class Notification extends TypedEntity {

    public static final String ENTITY_TYPE = "notification";

    public static final String RECEIPTS_COLLECTION = "receipts";

    /** Total count */
    @EntityProperty
    protected int expectedCount;

    @EntityProperty
    private PathTokens pathTokens;
    private String pathQuery;

    public static enum State {
        CREATED, FAILED, SCHEDULED, STARTED, FINISHED, CANCELED, EXPIRED
    }

    /** Map Notifier ID -> Payload data */
    @EntityProperty
    protected Map<String, Object> payloads;

    /** Time processed */
    @EntityProperty
    protected Long queued;

    /** Debug logging is on  */
    @EntityProperty
    protected boolean debug;

    /** Time send started */
    @EntityProperty
    protected Long started;

    /** Time processed */
    @EntityProperty
    protected Long finished;

    /** Time to deliver to provider */
    @EntityProperty
    protected Long deliver;

    /** Time to expire the notification */
    @EntityProperty
    protected Long expire;

    /** True if notification is canceled */
    @EntityProperty
    protected Boolean canceled;

    /** Error message */
    @EntityProperty
    protected String errorMessage;

    @EntityCollection(type = "receipt")
    protected List<UUID> receipts;

    /** stats (sent & errors) */
    @EntityProperty
    protected Map<String, Long> statistics;


    public Notification() {
        pathTokens = new PathTokens();
    }

    @JsonIgnore
    public List<UUID> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<UUID> receipts) {
        this.receipts = receipts;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Map<String, Object> getPayloads() {
        return payloads;
    }

    public void setPayloads(Map<String, Object> payloads) {
        this.payloads = payloads;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getFinished() {
        return finished;
    }

    public void setFinished(Long finished) {
        this.finished = finished;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getDeliver() {
        return deliver;
    }

    public void setDeliver(Long deliver) {
        this.deliver = deliver;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getExpire() {
        return expire;
    }

    public void setExpire(Long expire) {
        this.expire = expire;
    }

    @JsonIgnore
    public boolean isExpired() {
        return expire != null && expire < System.currentTimeMillis();
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(Boolean canceled) {
        this.canceled = canceled;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getStarted() {
        return started;
    }

    public void setStarted(Long started) {
        this.started = started;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Map<String, Long> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Long> statistics) {
        this.statistics = statistics;
    }

    public void updateStatistics(long sent, long errors) {
        if (this.statistics == null) {
            this.statistics = new HashMap<String, Long>(2);
            this.statistics.put("sent", sent);
            this.statistics.put("errors", errors);
        } else {
            this.statistics.put("sent", sent + this.statistics.get("sent"));
            this.statistics.put("errors",
                    errors + this.statistics.get("errors"));
        }
    }

    /** don't bother, I will ignore you */
    public void setState(State ignored) {
        // does nothing - state is derived
    }

    @EntityProperty(mutable = true, indexed = true)
    public State getState() {
        if (getErrorMessage() != null) {
            return State.FAILED;
        } else if (getCanceled() == Boolean.TRUE) {
            return State.CANCELED;
        } else if (getFinished() != null) {
            return State.FINISHED;
        } else if (getStarted() != null && getDeliver() == null) {
            return State.STARTED;
        } else if (isExpired()) {
            return State.EXPIRED;
        } else if (getDeliver() != null || getQueued() != null) {
            return State.SCHEDULED;
        }
        return State.CREATED;
    }

    @JsonIgnore
    public long getExpireTimeMillis() {
        return getExpire() != null ? getExpire() : 0;
    }

    @JsonIgnore
    public long getExpireTTLSeconds() {
        long ttlSeconds = (getExpireTimeMillis() - System.currentTimeMillis()) / 1000;
        return ttlSeconds > 0 ? ttlSeconds : 0;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getQueued() {
        return queued;
    }

    public void setQueued(Long queued) {
        this.queued = queued;
    }

    public void setExpectedCount(int expectedCount) {  this.expectedCount = expectedCount;  }

    @org.codehaus.jackson.map.annotate.JsonSerialize(include = org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion.NON_NULL)
    public int getExpectedCount() {  return expectedCount;  }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public PathTokens getPathTokens(){
        return pathTokens;
    }

    public void setPathTokens(PathTokens pathTokens){
        this.pathTokens = pathTokens;
    }

    @JsonIgnore
    public String getPathQuery(){
        return pathQuery;
    }
    public void setPathQuery(String query){
        pathQuery = query;
    }

    public static class PathTokens{
        private  SimpleEntityRef applicationRef;
        private List<PathToken> pathTokens;
        public PathTokens(){
            pathTokens = new ArrayList<>();

        }
        public PathTokens(final SimpleEntityRef applicationRef, final List<PathToken> pathTokens){
            this.applicationRef = applicationRef;
            this.pathTokens = pathTokens;
        }

        public void setPathTokens(final List<PathToken> pathTokens){
            this.pathTokens = pathTokens;
        }
        public List<PathToken> getPathTokens(){
            return pathTokens;
        }
        public EntityRef getApplicationRef() {
            return applicationRef;
        }
        public void setApplicationRef(SimpleEntityRef applicationRef){
            this.applicationRef = applicationRef;
        }

        @JsonIgnore
        public PathQuery<Device> getPathQuery() {
            PathQuery pathQuery = null;
            for (PathToken pathToken : getPathTokens()) {
                String collection = pathToken.getCollection();
                Query query = new Query();
                if (pathToken.getIdentifier()!=null) {
                    query.addIdentifier(pathToken.getIdentifier());
                }
                query.setLimit(100);
                query.setCollection(collection);

                if (pathQuery == null) {
                    pathQuery = new PathQuery(getApplicationRef(), query);
                } else {
                    pathQuery = pathQuery.chain(query);
                }
            }

            return pathQuery;
        }

    }
    public static class PathToken{
        private  String collection;
        private  Identifier identifier;

        public PathToken(){

        }

        public PathToken( final String collection, final Identifier identifier){
            this.collection = collection;

            this.identifier = identifier;

        }

        public String getCollection() {
            return collection;
        }
        public void setCollection(final String collection){
            this.collection = collection;
        }

        public Identifier getIdentifier() {
            return identifier;
        }
        public void setIdentifier(Identifier identifier){
            this.identifier = identifier;
        }


    }
}
