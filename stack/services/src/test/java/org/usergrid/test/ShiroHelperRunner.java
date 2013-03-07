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

import org.apache.shiro.subject.Subject;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.security.shiro.utils.SubjectUtils;

/**
 * @author tnine
 * 
 */
public class ShiroHelperRunner extends CassandraRunner {

  /**
   * @param klass
   * @throws InitializationError
   */
  public ShiroHelperRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.usergrid.cassandra.CassandraRunner#runChild(org.junit.runners.model
   * .FrameworkMethod, org.junit.runner.notification.RunNotifier)
   */
  @Override
  protected void runChild(FrameworkMethod method, RunNotifier notifier) {
    Subject subject = SubjectUtils.getSubject();
    if (subject != null) {
      subject.logout();
    }

    super.runChild(method, notifier);
  }

}
