package org.usergrid.batch.repository;

import com.google.common.base.Preconditions;
import org.usergrid.batch.BulkJobFactory;

import java.util.UUID;

/**
 * @author zznate
 */
public class JobDescriptor {

  private final String jobName;
  private final BulkJobFactory bulkJobFactory;
  private final UUID jobId;
  // optional fields
  private UUID orgId;
  private UUID appId;
  private String appName;

  private JobDescriptor(String jobName,
                        BulkJobFactory bulkJobFactory,
                        UUID jobId) {
    this.jobName = jobName;
    this.bulkJobFactory = bulkJobFactory;
    this.jobId = jobId;
  }

  public static JobDescriptor instance(String jobName,
                                       BulkJobFactory bulkJobFactory,
                                       UUID jobId ) {
    Preconditions.checkArgument(jobName != null, "Job name cannot be null");
    Preconditions.checkArgument(bulkJobFactory != null, "BulkJobFactory cannot be null");
    Preconditions.checkArgument(jobId != null, "A JobId is required");
    return new JobDescriptor(jobName, bulkJobFactory, jobId);
  }

  public String getJobName() {
    return jobName;
  }


  public BulkJobFactory getBulkJobFactory() {
    return bulkJobFactory;
  }

  public UUID getOrgId() {
    return orgId;
  }

  public JobDescriptor setOrgId(UUID orgId) {
    this.orgId = orgId;
    return this;
  }

  public UUID getAppId() {
    return appId;
  }

  public JobDescriptor setAppId(UUID appId) {
    this.appId = appId;
    return this;
  }

  public UUID getJobId() {
    return jobId;
  }

  public String getAppName() {
    return appName;
  }

  public JobDescriptor setAppName(String appName) {
    this.appName = appName;
    return this;
  }
}
