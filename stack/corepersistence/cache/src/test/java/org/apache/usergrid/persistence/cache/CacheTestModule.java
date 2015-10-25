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

package org.apache.usergrid.persistence.cache;


import com.google.inject.TypeLiteral;
import org.apache.usergrid.persistence.cache.guice.CacheModule;
import org.apache.usergrid.persistence.cache.impl.CacheFactoryImpl;
import org.apache.usergrid.persistence.cache.impl.ScopedCacheSerialization;
import org.apache.usergrid.persistence.cache.impl.ScopedCacheSerializationImpl;
import org.apache.usergrid.persistence.core.guice.CommonModule;

import java.util.Map;


public class CacheTestModule extends org.apache.usergrid.persistence.core.guice.TestModule {

    @Override
    protected void configure() {

        install( new CommonModule() );
        install( new CacheModule() );

        bind( new TypeLiteral<CacheFactory<String, Map<String, Object>>>() {} )
            .to(new TypeLiteral<CacheFactoryImpl<String, Map<String, Object>>>() { });

        bind( new TypeLiteral<ScopedCacheSerialization<String, Map<String, Object>>>() {} )
            .to(new TypeLiteral<ScopedCacheSerializationImpl<String, Map<String, Object>>>() { });

    }
}
