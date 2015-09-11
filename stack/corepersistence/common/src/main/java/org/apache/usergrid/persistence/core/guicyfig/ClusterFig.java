/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */
package org.apache.usergrid.persistence.core.guicyfig;

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;

/**
 * Configuration for environment
 */
@FigSingleton
public interface ClusterFig extends GuicyFig{


    /**
     * This value used in guice module validations so we can force a value to be set.  See IndexSettingValidation
     * for an example use.
     */
    String VALIDATION_DEFAULT_VALUE = "default-property";

    String CLUSTER_NAME_PROPERTY = "usergrid.cluster_name";


    @Default( VALIDATION_DEFAULT_VALUE )
    @Key( CLUSTER_NAME_PROPERTY )
    String getClusterName();

}
