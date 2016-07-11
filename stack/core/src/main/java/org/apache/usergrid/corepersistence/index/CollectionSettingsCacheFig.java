package org.apache.usergrid.corepersistence.index;/*
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


import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


/**
 * Collection settings cache config
 */
@FigSingleton
public interface CollectionSettingsCacheFig extends GuicyFig {

    @Key( "usergrid.collection_settings_cache_size" )
    @Default( "5000" )
    int getCacheSize();

    @Key( "usergrid.collection_settings_cache_timeout_ms" )
    @Default( "15000" )
    int getCacheTimeout();

}
