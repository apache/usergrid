package org.usergrid.batch;

import org.junit.Test;
import org.usergrid.batch.repository.JobDescriptor;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author zznate
 */
public class BulkJobExecutionUnitTest {

  @Test
  public void transitionsOk() {
    JobDescriptor jobDescriptor = JobDescriptor.instance("",
            BulkTestUtils.getBulkJobFactory(),
            UUID.randomUUID());
    BulkJobExecution bje = BulkJobExecution.instance(jobDescriptor);
    assertEquals(BulkJobExecution.Status.NOT_STARTED, bje.getStatus());
    bje.start();
    assertEquals(BulkJobExecution.Status.IN_PROGRESS, bje.getStatus());
    bje.completed();
    assertEquals(BulkJobExecution.Status.COMPLETED, bje.getStatus());
  }

  @Test
  public void transitionFail() {
    JobDescriptor jobDescriptor = JobDescriptor.instance("",
                BulkTestUtils.getBulkJobFactory(),
                UUID.randomUUID());
    BulkJobExecution bje = BulkJobExecution.instance(jobDescriptor);
    try {
      bje.completed();
      fail("Should have throw ISE on NOT_STARTED to IN_PROGRESS");
    } catch (IllegalStateException ise) {}

    try {
      bje.failed();
      fail("Should have thrown ISE on NOT_STARTED to FAILED");
    } catch (IllegalStateException ise) {}
    bje.start();

    bje.completed();
    try {
      bje.failed();
      fail("Should have failed failed after complete call");
    } catch (IllegalStateException ise) {}

  }

  @Test
  public void doubleInvokeFail() {
    JobDescriptor jobDescriptor = JobDescriptor.instance("",
                    BulkTestUtils.getBulkJobFactory(),
                    UUID.randomUUID());
    BulkJobExecution bje = BulkJobExecution.instance(jobDescriptor);
    bje.start();
    try {
      bje.start();
      fail("Should have failed on double start() call");
    } catch (IllegalStateException ise) {}

    bje.completed();
    try {
      bje.completed();
      fail("Should have failed on double complete call");
    } catch (IllegalStateException ise) {}

  }

}
