/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */
package org.apache.usergrid.persistence.cache.guice;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.apache.usergrid.persistence.cache.CacheFactory;
import org.apache.usergrid.persistence.cache.impl.CacheFactoryImpl;
import org.apache.usergrid.persistence.cache.impl.ScopedCacheSerialization;
import org.apache.usergrid.persistence.cache.impl.ScopedCacheSerializationImpl;

import java.util.Map;


/**
 * Wire up cache impl.
 */
public class CacheModule extends AbstractModule {

    @Override
    protected void configure() {

        bind( CacheFactory.class ).to( CacheFactoryImpl.class );
        bind( ScopedCacheSerialization.class ).to( ScopedCacheSerializationImpl.class );

        bind( new TypeLiteral<CacheFactory<String, Map<String, Object>>>() {} )
            .to( new TypeLiteral<CacheFactoryImpl<String, Map<String, Object>>>() {} );

    }
}


