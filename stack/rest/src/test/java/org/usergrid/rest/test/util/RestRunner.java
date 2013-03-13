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
package org.usergrid.rest.test.util;

import java.lang.reflect.Field;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.usergrid.cassandra.CassandraRunner;
import org.usergrid.rest.test.resource.TestContext;
import org.usergrid.rest.test.security.TestAdminUser;

import com.sun.jersey.test.framework.JerseyTest;

/**
 * Helper that creates a test application and test context as well as running
 * cassandra runners
 * 
 * @author tnine
 * 
 */
public class RestRunner extends CassandraRunner {

  private Field cachedField;

  /**
   * @param klass
   * @throws InitializationError
   */
  public RestRunner(Class<?> klass) throws InitializationError {
    super(klass);
  }

  private void setupTest(Class<?> testClass, String methodName, JerseyTest test) {

    Field target = findField(testClass);

    if (target == null) {
      throw new RuntimeException(String.format("You did not specify a %s field with the %s annotation",
          TestContext.class.getName(), Context.class));
    }

    String name = testClass.getName() + "." + methodName;

    TestAdminUser testAdmin = new TestAdminUser(name, name + "@usergrid.com", name + "@usergrid.com");

    // create the text context
    TestContext context = TestContext.create(test).withOrg(name).withApp(methodName).withUser(testAdmin).initAll();

    try {
      boolean accessible = target.isAccessible();

      target.setAccessible(true);

      target.set(test, context);

      target.setAccessible(accessible);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  /**
   * Find the field
   * 
   * @param testClass
   * @return
   */
  private Field findField(final Class<?> testClass) {

    if (cachedField != null) {
      return cachedField;
    }

    Class<?> current = testClass;
    
    do {
      for (Field field : current.getDeclaredFields()) {

        if (field.getAnnotation(Context.class) != null) {
          cachedField = field;
          return cachedField;
        }

      }
      current = current.getSuperclass();
    } while (current != null);

    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.junit.runners.BlockJUnit4ClassRunner#methodInvoker(org.junit.runners
   * .model.FrameworkMethod, java.lang.Object)
   */
  @Override
  protected Statement methodInvoker(FrameworkMethod method, Object test) {

    if (!(test instanceof JerseyTest)) {
      throw new RuntimeException(String.format("This runner can only be used with subclasses of %s", JerseyTest.class));
    }

    setupTest(test.getClass(), method.getName(), (JerseyTest) test);

    return super.methodInvoker(method, test);
  }

}
