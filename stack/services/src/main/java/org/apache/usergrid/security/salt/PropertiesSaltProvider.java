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
package org.apache.usergrid.security.salt;


import java.util.Properties;
import java.util.UUID;


/**
 * Simple implementation that uses applicationId as a key to lookup a salt value from a properties file.<br/> The file
 * must be loaded via spring, and injected into this bean
 *
 * @author tnine
 */
public class PropertiesSaltProvider implements SaltProvider {

    private Properties saltProperties;


    /* (non-Javadoc)
     * @see org.apache.usergrid.security.salt.SaltProvider#getSalt(java.util.UUID, java.util.UUID)
     */
    @Override
    public String getSalt( UUID applicationId, UUID userId ) {
        return saltProperties.getProperty( applicationId.toString() );
    }


    /** @return the saltProperties */
    public Properties getSaltProperties() {
        return saltProperties;
    }


    /** @param saltProperties the saltProperties to set */
    public void setSaltProperties( Properties saltProperties ) {
        this.saltProperties = saltProperties;
    }
}
