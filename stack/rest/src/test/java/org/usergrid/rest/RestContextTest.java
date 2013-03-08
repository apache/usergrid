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
package org.usergrid.rest;

import org.junit.runner.RunWith;
import org.usergrid.rest.test.resource.TestContext;
import org.usergrid.rest.test.util.Context;
import org.usergrid.rest.test.util.RestRunner;

import com.sun.jersey.test.framework.spi.container.TestContainerException;

/**
 * Abstract class to make running tests easier at the rest tier.
 * 
 * Auto creates and test and context
 * @author tnine
 *
 */
@RunWith(RestRunner.class)
public abstract class RestContextTest extends AbstractRestTest {

  @Context
  protected TestContext context;
  
  /**
   * @throws TestContainerException
   */
  public RestContextTest() throws TestContainerException {
  }

}
