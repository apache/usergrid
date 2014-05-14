/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *    http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */
package org.apache.usergrid.persistence.core.cassandra;


import java.lang.reflect.InvocationTargetException;

import org.jukito.JukitoRunner;
import org.junit.runners.model.InitializationError;

import com.google.inject.Injector;


/**
 * Run jukito with cassandra
 */
public class ITRunner extends JukitoRunner {

    //this is fugly, but we have no other way to start cassandra before the jukito runner
    static{
      CassandraRule rule = new CassandraRule();
        try {
            rule.before();
        }
        catch ( Throwable throwable ) {
            //super nasty, but we need to bail if this happens
            throwable.printStackTrace();
            System.exit( -1 );
        }
    }

    public ITRunner( final Class<?> klass )
            throws InitializationError, InvocationTargetException, InstantiationException, IllegalAccessException {
        super( klass );
    }


    public ITRunner( final Class<?> klass, final Injector injector )
            throws InitializationError, InvocationTargetException, InstantiationException, IllegalAccessException {
        super( klass, injector );
    }
}
