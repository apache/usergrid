/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.token;


import org.apache.usergrid.persistence.token.guice.TokenModule;
import org.apache.usergrid.persistence.token.impl.TokenSerialization;
import org.apache.usergrid.persistence.token.impl.TokenSerializationImpl;
import org.apache.usergrid.persistence.core.guice.CommonModule;



public class TokenTestModule extends org.apache.usergrid.persistence.core.guice.TestModule {

    @Override
    protected void configure() {

        install( new CommonModule() );
        install( new TokenModule() );

        bind(TokenSerialization.class).to(TokenSerializationImpl.class).asEagerSingleton();
    }
}
