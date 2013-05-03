package org.usergrid.batch;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;
import org.usergrid.batch.JobExecution.Status;
import org.usergrid.batch.repository.JobDescriptor;
import org.usergrid.persistence.entities.JobData;
import org.usergrid.persistence.entities.JobStat;

/**
 * @author zznate
 * @author tnine
 */
public class BulkJobExecutionUnitTest {

  @Test
  public void transitionsOk() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);
    assertEquals(JobExecution.Status.NOT_STARTED, bje.getStatus());
    bje.start(1);
    assertEquals(JobExecution.Status.IN_PROGRESS, bje.getStatus());
    bje.completed();
    assertEquals(JobExecution.Status.COMPLETED, bje.getStatus());
  }

  @Test
  public void transitionsDead() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);
    assertEquals(JobExecution.Status.NOT_STARTED, bje.getStatus());
    bje.start(1);
    assertEquals(JobExecution.Status.IN_PROGRESS, bje.getStatus());
    bje.killed();
    assertEquals(JobExecution.Status.DEAD, bje.getStatus());
  }

  @Test
  public void transitionsRetry() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);
    assertEquals(JobExecution.Status.NOT_STARTED, bje.getStatus());
    bje.start(JobExecution.FOREVER);
    assertEquals(JobExecution.Status.IN_PROGRESS, bje.getStatus());
    bje.failed();
    assertEquals(JobExecution.Status.FAILED, bje.getStatus());
  }

  @Test
  public void transitionFail() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);
    try {
      bje.completed();
      fail("Should have throw ISE on NOT_STARTED to IN_PROGRESS");
    } catch (IllegalStateException ise) {
    }

    try {
      bje.failed();
      fail("Should have thrown ISE on NOT_STARTED to FAILED");
    } catch (IllegalStateException ise) {
    }
    bje.start(1);

    bje.completed();
    try {
      bje.failed();
      fail("Should have failed failed after complete call");
    } catch (IllegalStateException ise) {
    }

  }

  @Test
  public void transitionFailOnDeath() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);
    try {
      bje.completed();
      fail("Should have throw ISE on NOT_STARTED to IN_PROGRESS");
    } catch (IllegalStateException ise) {
    }

    try {
      bje.failed();
      fail("Should have thrown ISE on NOT_STARTED to FAILED");
    } catch (IllegalStateException ise) {
    }
    bje.start(1);

    bje.killed();
    try {
      bje.killed();
      fail("Should have failed failed after complete call");
    } catch (IllegalStateException ise) {
    }

  }

  @Test
  public void failureTriggerCount() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);

    bje.start(1);

    assertEquals(Status.IN_PROGRESS, bje.getStatus());
    assertEquals(1, stat.getRunCount());
    

    bje.failed();
    
    assertEquals(Status.FAILED, bje.getStatus());
    assertEquals(1, stat.getRunCount());
  

    // now fail again, we should trigger a state change
    bje = new JobExecutionImpl(jobDescriptor);
    bje.start(1);

    assertEquals(Status.DEAD, bje.getStatus());
    assertEquals(2, stat.getRunCount());

  }

  @Test
  public void failureTriggerNoTrip() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);

    bje.start(JobExecution.FOREVER);
    
    assertEquals(Status.IN_PROGRESS, bje.getStatus());
    assertEquals(1, stat.getRunCount());
    
    bje.failed();

    assertEquals(Status.FAILED, bje.getStatus());
    assertEquals(1, stat.getRunCount());

    // now fail again, we should trigger a state change
    bje = new JobExecutionImpl(jobDescriptor);
    bje.start(JobExecution.FOREVER);
    
    assertEquals(Status.IN_PROGRESS, bje.getStatus());
    assertEquals(2, stat.getRunCount());
    
    bje.failed();

    assertEquals(Status.FAILED, bje.getStatus());
    assertEquals(2, stat.getRunCount());
  

  }

  @Test
  public void doubleInvokeFail() {
    JobData data = new JobData();
    JobStat stat = new JobStat();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, stat, null);
    JobExecution bje = new JobExecutionImpl(jobDescriptor);
    bje.start(1);
    try {
      bje.start(1);
      fail("Should have failed on double start() call");
    } catch (IllegalStateException ise) {
    }

    bje.completed();
    try {
      bje.completed();
      fail("Should have failed on double complete call");
    } catch (IllegalStateException ise) {
    }

  }

}
