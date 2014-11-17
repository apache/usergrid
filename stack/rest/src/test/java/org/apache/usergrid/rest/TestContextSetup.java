/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.rest;


import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.apache.usergrid.persistence.index.utils.UUIDUtils;
import org.apache.usergrid.rest.management.OrganizationsIT;
import org.apache.usergrid.rest.test.resource.TestContext;
import org.apache.usergrid.rest.test.security.TestAdminUser;

import com.sun.jersey.test.framework.JerseyTest;
import java.io.IOException;


/**
 * A self configuring TestContext which sets itself up it implements TestRule. With a @Rule annotation, an instance of
 * this Class as a public member in any test class or abstract test class will auto svcSetup itself before each test.
 */
public class TestContextSetup extends TestContext implements TestRule {

    public TestContextSetup( JerseyTest test ) {
        super( test );
    }


    public Statement apply( Statement base, Description description ) {
        return statement( base, description );
    }


    private Statement statement( final Statement base, final Description description ) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before( description );
                try {
                    base.evaluate();
                }
                finally {
                    cleanup();
                }
            }
        };
    }


    protected void cleanup() {
        // might want to do something here later
    }


    protected void before( Description description ) throws IOException {
        String testClass = description.getTestClass().getName();
        String methodName = description.getMethodName();
        String name = testClass + "." + methodName;

        TestAdminUser testAdmin = new TestAdminUser( name+UUIDUtils.newTimeUUID(),
                name + "@usergrid.com"+UUIDUtils.newTimeUUID(),
                name + "@usergrid.com"+UUIDUtils.newTimeUUID() );
        withOrg( name+ UUIDUtils.newTimeUUID() ).withApp( methodName + UUIDUtils.newTimeUUID() ).withUser(
                testAdmin ).initAll();
    }
}
