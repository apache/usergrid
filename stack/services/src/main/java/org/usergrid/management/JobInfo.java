package org.usergrid.management;

import java.util.UUID;

/**
 * Created by ApigeeCorporation on 1/31/14.
 */
//TODO: Documentation on this class
public class JobInfo {
    private UUID jobId;

    public enum StatusType{
        PENDING,STARTED,FAILED,COMPLETED,FAIL1, FAIL2
    }
    private StatusType jobStatusType;

    public JobInfo (UUID jobId, StatusType jobStatusType){
        jobId = this.jobId;
        jobStatusType = this.jobStatusType;
    }

    public StatusType getStatusType () {
        return jobStatusType;
    }

    public UUID getJobId () {
        return jobId;
    }
}
