package org.usergrid.persistence.entities;


import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityProperty;


/**
 *
 *
 */
@XmlRootElement
public class Export extends TypedEntity {

    public static final String ENTITY_TYPE = "export";

    //Additional states could include CREATED,SCHEDULED,EXPIRED
    public static enum State {
        PENDING, STARTED, FAILED, COMPLETED,
    }

    /** Map Notifier ID -> Properties data provided */
    @EntityProperty
    protected Map<String, Object> properties;

    /** Time processed */
    @EntityProperty
    protected Long queued;

    /** Time send started */
    @EntityProperty
    protected Long started;

    /** Time processed */
    @EntityProperty
    protected Long finished;

    /** True if notification is canceled */
    @EntityProperty
    protected Boolean canceled;

    /** Error message */
    @EntityProperty
    protected String errorMessage;

    /** Contains the Query included with the Path **/

    public Export () {
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String,Object> properties) {
        this.properties = properties;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public Long getCompleted() {
        return finished;
    }

    public void setCompleted(Long finished) {
        this.finished = finished;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public Boolean getCanceled() {
        return canceled;
    }

    public void setCanceled(Boolean canceled) {
        this.canceled = canceled;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public Long getStarted() {
        return started;
    }

    public void setStarted(Long started) {
        this.started = started;
    }

    @JsonSerialize(include = Inclusion.NON_NULL)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setState(State ignored) {
        //state is derived from getState, not set.
    }

    @EntityProperty
    public State getState() {
        if (getErrorMessage() != null) {
            return State.FAILED;
        } else if (getCompleted() != null) {
            return State.COMPLETED;
        } else if (getStarted() != null) {
            return State.STARTED;
        }
        return State.PENDING;
    }
    /* there might need to be queued stuff here.  */
    /*
    * Path Query Ignored for first pass

    //ask scott why these are ignored
    @JsonIgnore
    public PathQuery< What device is set here?> getPathQuery() {

    }
    */
}
