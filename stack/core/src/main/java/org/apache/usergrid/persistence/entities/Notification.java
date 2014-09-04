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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.usergrid.persistence.PathQuery;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityCollection;
import org.apache.usergrid.persistence.annotations.EntityProperty;
import org.apache.usergrid.persistence.entities.Device;

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

    public static enum State {
        CREATED, FAILED, SCHEDULED, STARTED, FINISHED, CANCELED, EXPIRED
    }

    /** Map Notifier ID -> Payload data */
    @EntityProperty
    protected Map<String, Object> payloads;

    /** Time processed */
    @EntityProperty
    protected Long queued;

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

    /** stats (sent & errors) */
    @EntityProperty
    @JsonIgnore
    protected PathQuery<Device> pathQuery;

    public Notification() {
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
        return expire != null && expire > System.currentTimeMillis();
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(Boolean canceled) {
        this.canceled = canceled;
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
    public PathQuery<Device> getPathQuery() {
        return pathQuery;
    }

    public void setPathQuery(PathQuery<Device> pathQuery) {
        this.pathQuery = pathQuery;
    }

    @JsonIgnore
    public int getExpireTimeInSeconds() {
        long expirySeconds = getExpire() != null ? getExpire() * 1000 : 0;
        return (expirySeconds > Integer.MAX_VALUE) ? Integer.MAX_VALUE
                : (int) expirySeconds;
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
}
