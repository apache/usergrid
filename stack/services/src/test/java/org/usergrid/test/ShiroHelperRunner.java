/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.test;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.cassandra.DataControl;
import org.usergrid.security.shiro.utils.SubjectUtils;

/**
 * @author tnine
 * 
 */
public class ShiroHelperRunner extends CassandraRunner {

  
  private SubjectThreadState subjectThreadState;
  
  /**
   * @param klass
   * @throws InitializationError
   */
  public ShiroHelperRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  /* (non-Javadoc)
   * @see org.usergrid.cassandra.CassandraRunner#preTest(org.junit.runner.notification.RunNotifier)
   */
  @Override
  protected DataControl preTest(RunNotifier notifier) {
    return super.preTest(notifier);
    
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.cassandra.CassandraRunner#postTest(org.junit.runner.notification
   * .RunNotifier, org.usergrid.cassandra.DataControl)
   */
  @Override
  protected void postTest(RunNotifier notifier, DataControl control) {
    Subject subject = SecurityUtils.getSubject();

    if (subject == null) {
      return;
    }

    new SubjectThreadState(subject).clear();

    super.postTest(notifier, control);
  }

}
