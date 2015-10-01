/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.core.guicyfig;


import java.lang.annotation.Annotation;

import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.Env;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Option;


public class SetConfigTestBypass {


    /**
     * Set the value bypass on the guicyfig
     * @param guicyFig
     * @param methodName
     * @param valueToSet
     */
    public static void setValueByPass(final GuicyFig guicyFig, final String methodName, final String valueToSet){
        guicyFig.setBypass( new TestByPass( methodName, valueToSet ) );
    }

    /**
     * Test bypass that sets all environments to use the timeout of 1 second
     */
    public static final class TestByPass implements Bypass {

        private Option[] options;


        public TestByPass( final String methodName, final String value ) {
            options = new Option[] { new TestOption( methodName, value ) };
        }


        @Override
        public Option[] options() {
            return options;
        }


        @Override
        public Env[] environments() {
            return new Env[] { Env.ALL, Env.UNIT };
        }


        @Override
        public Class<? extends Annotation> annotationType() {
            return Bypass.class;
        }
    }


    /**
     * TestOption
     */
    public static final class TestOption implements Option {

        private final String methodName;
        private final String valueToReturn;


        public TestOption( final String methodName, final String valueToReturn ) {
            this.methodName = methodName;
            this.valueToReturn = valueToReturn;
        }


        @Override
        public Class<? extends Annotation> annotationType() {
            return Bypass.class;
        }


        @Override
        public String method() {
            return methodName;
        }


        @Override
        public String override() {
            return valueToReturn;
        }
    }
}
