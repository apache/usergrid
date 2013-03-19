package org.usergrid.batch;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.usergrid.batch.repository.JobDescriptor;

/**
 * @author tnine
 */
public class UsergridJobFactory implements JobFactory {

  @Autowired
  private ApplicationContext context;

  private Logger logger = LoggerFactory.getLogger(UsergridJobFactory.class);

  @Override
  public List<Job> jobsFrom(JobDescriptor descriptor) throws JobNotFoundException {

    Job job = context.getBean(descriptor.getJobName(), Job.class);

    if (job == null) {
      String error = String.format("Could not find job impelmentation for job name %s", descriptor.getJobName());
      logger.error(error);
      throw new JobNotFoundException(error);
    }

    return Collections.singletonList(job);

  }

}
