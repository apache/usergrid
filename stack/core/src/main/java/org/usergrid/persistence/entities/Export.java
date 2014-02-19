package org.usergrid.persistence.entities;


import javax.xml.bind.annotation.XmlRootElement;

import org.usergrid.persistence.TypedEntity;
import org.usergrid.persistence.annotations.EntityProperty;


/**
 *
 *
 */

/**
 *
 *the sch system doesn't make any assumes about the job or how it works.
 * and so if I need additional information to be persistant.
 *
 * The way to save data between queue and storing it.
 *
 * in my case, create a export entity. before I schedule the job and it'll have the pending state in it and
 * all the information I need to run. Then I'll pass the ID of the export info I saved in a collection and i'll put that in the jbo
 * data.
 *
 * persist the state in mechanisum that they can all access.
 *
 * I could make it a class and I can make it an entity. That way I can get it in and out.
 * doesn't get exposed to the user.
 */
@XmlRootElement
public class Export extends TypedEntity {

    public static enum State {
        //CREATED, FAILED, SCHEDULED, STARTED, FINISHED, CANCELED, EXPIRED
        PENDING,STARTED,FAILED,COMPLETED
    }

    @EntityProperty
    public State curState;

    @EntityProperty
    protected Long queued;

    /** Time send started */
    @EntityProperty
    protected Long started;

    /** Time processed */
    @EntityProperty
    protected Long finished;


    /** Time to expire the exportJob */
    @EntityProperty
    protected Long expire;

    /** True if exportJob is canceled */
    @EntityProperty
    protected Boolean canceled;

    /** Error message */
    @EntityProperty
    protected String errorMessage;

    @EntityProperty
    public State getState() {
        if (getErrorMessage() != null) {
            return State.FAILED;
//        } else if (getCanceled() == Boolean.TRUE) {
//            return State.CANCELED;
        } else if (getFinished() != null) {
            return State.COMPLETED;
        } else if (getStarted() != null) {
            return State.STARTED;
        }
//        } else if (isExpired()) {
//            return State.EXPIRED;
//        } else if (getQueued() != null) {
//            return State.SCHEDULED;
//        }
        return State.PENDING;
    }

    public Export() {
    }

    public boolean isExpired () {
        return (expire != null && expire > System.currentTimeMillis());
    }

    public Long getStarted() {
        return started;
    }


    public void setStarted( final Long started ) {
        this.started = started;
    }


    public Long getFinished() {
        return finished;
    }


    public void setFinished( final Long finished ) {
        this.finished = finished;
    }


    public Long getExpire() {
        return expire;
    }


    public void setExpire( final Long expire ) {
        this.expire = expire;
    }


    public Boolean getCanceled() {
        return canceled;
    }

    //state should moved to a derived state, but it is not there yet.
    public void setState(State setter) {
        curState = setter;
    }


    public void setCanceled( final Boolean canceled ) {
        this.canceled = canceled;
    }


    public String getErrorMessage() {
        return errorMessage;
    }


    public void setErrorMessage( final String errorMessage ) {
        this.errorMessage = errorMessage;
    }


    public Long getQueued() {
        return queued;
    }


    public void setQueued( final Long queued ) {
        this.queued = queued;
    }
}
