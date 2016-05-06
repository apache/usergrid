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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.PathQuery;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.SimpleEntityRef;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityCollection;
import org.apache.usergrid.persistence.annotations.EntityProperty;
import org.apache.usergrid.persistence.index.query.Identifier;
import org.apache.usergrid.utils.InflectionUtils;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * The entity class for representing Notifications.
 */
@XmlRootElement
public class Notification extends TypedEntity {

    public static final String ENTITY_TYPE = "notification";

    public static final String RECEIPTS_COLLECTION = "receipts";

    public enum Priority {
        NORMAL, HIGH
    }

    /** The pathQuery/query that Usergrid used to idenitfy the devices to send the notification to */
    @EntityProperty
    private PathTokens pathQuery;

    public static enum State {
        CREATED, FAILED, SCHEDULED, STARTED, FINISHED, CANCELED, EXPIRED
    }

    /** Map Notifier ID -> Payload data */
    @EntityProperty
    protected Map<String, Object> payloads;

    /** Timestamp (ms) when the notification was processed */
    @EntityProperty
    protected Long queued;

    /** Timestamp (ms) when the notification was processed */
    @EntityProperty
    protected Long processingFinished;

    /** Timestamp (ms) when send notification started */
    @EntityProperty
    protected Long started;

    /** Timestamp (ms) when send notification finished */
    @EntityProperty
    protected Long finished;

    /** Timestamp (ms) to deliver to provider */
    @EntityProperty
    protected Long deliver;

    /** Timestamp (ms) to expire the notification*/
    @EntityProperty
    protected Long expire;

    /** Stores the number of devices processed */
    @EntityProperty
    protected int deviceProcessedCount;

    /** True if notification is canceled */
    @EntityProperty
    protected Boolean canceled;

    /** Flag to enable/disable verbose logging of states  */
    @EntityProperty
    protected boolean debug;

    /** Flag to set the notification priority. Valid values "normal" and "high"  */
    @EntityProperty
    protected String priority;

    /** Flag to signal Usergrid to use graph traversal + filtering to find devices  */
    @EntityProperty
    protected boolean useGraph;

    /** Error messages that may have been encountered by Usergrid when trying to process the notification */
    @EntityProperty
    protected String errorMessage;

    /** Flag to disable the creation, saving, connecting of receipt entities for a notification.  */
    @EntityProperty
    protected boolean saveReceipts;

    @EntityCollection(type = "receipt")
    protected List<UUID> receipts;

    /** Map containing a count for "sent" and "errors" */
    @EntityProperty
    protected Map<String, Object> statistics;

    @EntityProperty
    protected Map<String, Object> filters;


    public Notification() {
        pathQuery = new PathTokens();
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public PathTokens getPathQuery(){
        return pathQuery;
    }

    public void setPathQuery(PathTokens pathQuery){
        this.pathQuery = pathQuery;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Map<String, Object> getPayloads() {
        return payloads;
    }

    public void setPayloads(Map<String, Object> payloads) {
        this.payloads = payloads;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getQueued() {
        return queued;
    }

    public void setQueued(Long queued) {
        this.queued = queued;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getFinished() {
        return finished;
    }

    public void setFinished(Long finished) {
        this.finished = finished;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Long getProcessingFinished() {
        return processingFinished;
    }

    public void setProcessingFinished(Long processingFinished) {
        this.processingFinished = processingFinished;
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

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public int getDeviceProcessedCount() {
        return deviceProcessedCount;
    }

    public void setDeviceProcessedCount(int deviceProcessedCount) {
        this.deviceProcessedCount = deviceProcessedCount;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(Boolean canceled) {
        this.canceled = canceled;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public boolean getSaveReceipts() {
        return saveReceipts;
    }

    public void setSaveReceipts(boolean saveReceipts) {
        this.saveReceipts = saveReceipts;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public boolean getUseGraph() {
        return useGraph;
    }

    public void setUseGraph(boolean useGraph) {
        this.useGraph = useGraph;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getPriority() {

        // simply default the priority to normal so it's never null
        if (priority == null || priority.isEmpty()){
            priority = "normal";
        }

        // validate that the priority available is a valid Usergrid notification priority
        String notificationPriority;
        try{
            notificationPriority = Priority.valueOf(priority.toUpperCase()).toString();
        }catch(IllegalArgumentException e){
            // it's not a valid Usergrid notification priority, default to normal
            notificationPriority = Priority.NORMAL.toString();
        }

        setPriority(notificationPriority.toLowerCase());

        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
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
    public Map<String, Object> getStatistics() {
        return statistics;
    }

    public void setStatistics(Map<String, Object> statistics) {
        this.statistics = statistics;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }

    public void updateStatistics(long sent, long errors) {
        if (this.statistics == null) {
            this.statistics = new HashMap<String, Object>(2);
            this.statistics.put("sent", sent);
            this.statistics.put("errors", errors);
        } else {
            if(this.statistics.get( "sent" ) instanceof Integer){
                this.statistics.put( "sent", sent + (Integer) this.statistics.get( "sent" ) );
                this.statistics.put( "errors", errors + (Integer) this.statistics.get( "errors" ) );
            }
            else if (this.statistics.get( "sent" ) instanceof Long ) {
                this.statistics.put( "sent", sent + (Long) this.statistics.get( "sent" ) );
                this.statistics.put( "errors", errors + (Long) this.statistics.get( "errors" ) );
            }
        }
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

    /** don't bother, I will ignore you */
    public void setState(State ignored) {
        // does nothing - state is derived
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

    @JsonIgnore
    public boolean isExpired() {
        return expire != null && expire < System.currentTimeMillis();
    }

    @JsonIgnore
    public List<UUID> getReceipts() {
        return receipts;
    }

    public void setReceipts(List<UUID> receipts) {
        this.receipts = receipts;
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
        public PathQuery<Device> buildPathQuery() {
            PathQuery pathQuery = null;
            List<PathToken> pathTokens = getPathTokens();
            for (PathToken pathToken : pathTokens) {
                String collection = pathToken.getCollection();
                Query query = new Query();
                if(pathToken.getQl() != null){

                    // if a query is already present, use it's QL
                    query.setQl(pathToken.getQl());

                }else if (pathToken.getIdentifier()!=null) {

                    // users collection is special case and uses "username" instaed of "name"
                    // build a query using QL with "username" as Identifier.Type.USERNAME doesn't exist
                    if (collection.equals("users") && pathToken.getIdentifier().getType() == Identifier.Type.NAME){
                        query.setQl("select * where username ='"+pathToken.getIdentifier().getName()+"'");
                    }else{
                        query.addIdentifier(pathToken.getIdentifier());
                    }

                }
                query.setLimit(100);
                query.setCollection(collection);

                if (pathQuery == null) {
                    pathQuery = new PathQuery(getApplicationRef(), query);

                    if ( pathTokens.size() == 1 && collection.equals(InflectionUtils.pluralize(Group.ENTITY_TYPE) )){

                        final Query usersQuery = new Query();
                        usersQuery.setQl("select *");
                        usersQuery.setCollection("users");
                        usersQuery.setLimit(100);

                        final Query devicesQuery = new Query();
                        devicesQuery.setQl("select *");
                        devicesQuery.setCollection("devices");
                        devicesQuery.setLimit(100);


                        // build up the chain so the proper iterators can be used later
                        pathQuery = pathQuery.chain( usersQuery );//.chain( devicesQuery );

                    }else if(pathTokens.size() == 1 && collection.equals(InflectionUtils.pluralize(User.ENTITY_TYPE))){

                        final Query devicesQuery = new Query();
                        devicesQuery.setQl("select *");
                        devicesQuery.setCollection("devices");
                        devicesQuery.setLimit(100);

                        pathQuery = pathQuery.chain( devicesQuery );
                    }

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
        private String ql;

        public PathToken(){

        }

        public PathToken( final String collection, final Identifier identifier){
            this.collection = collection;
            this.identifier = identifier;
            this.ql = null;

        }

        public PathToken( final String collection, final String ql){
            this.collection = collection;
            this.ql = ql;
            this.identifier = null;

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

        public String getQl() {
            return ql;
        }
        public void setQl(String ql){
            this.ql = ql;
        }


    }
}
