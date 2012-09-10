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
package org.usergrid.security.salt;

import java.util.UUID;

/**
 * Returns the salt for a user to be used to hash the password
 * @author tnine
 *
 */
public interface SaltProvider {

    /**
     * Get the salt that should be used for a user in the given application
     * @param applicationName
     * @param username
     * @return
     */
    public String getSalt(UUID applicationId, UUID userId);
}
