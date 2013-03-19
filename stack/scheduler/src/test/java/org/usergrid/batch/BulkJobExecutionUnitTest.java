package org.usergrid.batch;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;
import org.usergrid.batch.JobExecution.Status;
import org.usergrid.batch.repository.JobDescriptor;
import org.usergrid.batch.service.JobData;

/**
 * @author zznate
 */
public class BulkJobExecutionUnitTest {

  @Test
  public void transitionsOk() {
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), null, null);
    JobExecution bje = new JobExecution(jobDescriptor);
    assertEquals(JobExecution.Status.NOT_STARTED, bje.getStatus());
    bje.start();
    assertEquals(JobExecution.Status.IN_PROGRESS, bje.getStatus());
    bje.completed();
    assertEquals(JobExecution.Status.COMPLETED, bje.getStatus());
  }

  @Test
  public void transitionFail() {
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), null, null);
    JobExecution bje = new JobExecution(jobDescriptor);
    try {
      bje.completed();
      fail("Should have throw ISE on NOT_STARTED to IN_PROGRESS");
    } catch (IllegalStateException ise) {
    }

    try {
      bje.failed(0);
      fail("Should have thrown ISE on NOT_STARTED to FAILED");
    } catch (IllegalStateException ise) {
    }
    bje.start();

    bje.completed();
    try {
      bje.failed(0);
      fail("Should have failed failed after complete call");
    } catch (IllegalStateException ise) {
    }

  }
  
  @Test
  public void failureTriggerCount(){
    JobData data = new JobData();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, null);
    JobExecution bje = new JobExecution(jobDescriptor);
    
    bje.start();
    bje.failed(1);
    
    assertEquals(Status.FAILED, bje.getStatus());
    assertEquals(1, data.getFailCount());
    
    //now fail again, we should trigger a state change
    bje = new JobExecution(jobDescriptor);
    bje.start();
    bje.failed(1);
    
    assertEquals(Status.DEAD, bje.getStatus());
    assertEquals(2, data.getFailCount());
    
    
  }
  
  
  @Test
  public void failureTriggerNoTrip(){
    JobData data = new JobData();
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), data, null);
    JobExecution bje = new JobExecution(jobDescriptor);
    
    bje.start();
    bje.failed(JobExecution.FOREVER);
    
    assertEquals(Status.FAILED, bje.getStatus());
    assertEquals(1, data.getFailCount());
    
    //now fail again, we should trigger a state change
    bje = new JobExecution(jobDescriptor);
    bje.start();
    bje.failed(JobExecution.FOREVER);
    
    assertEquals(Status.FAILED, bje.getStatus());
    assertEquals(2, data.getFailCount());
    
    
  }

  @Test
  public void doubleInvokeFail() {
    JobDescriptor jobDescriptor = new JobDescriptor("", UUID.randomUUID(), UUID.randomUUID(), null, null);
    JobExecution bje = new JobExecution(jobDescriptor);
    bje.start();
    try {
      bje.start();
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
