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
package org.apache.usergrid.services.exceptions;


import java.util.Collection;

import static org.apache.usergrid.security.PasswordPolicy.ERROR_POLICY_VIOLIATION;


public class PasswordPolicyViolationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final Collection<String> violations;
    private final String description;

    public PasswordPolicyViolationException( String description, Collection<String> violations ) {
        super( ERROR_POLICY_VIOLIATION );
        this.violations = violations;
        this.description = description;
    }


    public Collection<String> getViolations() {
        return violations;
    }


    public String getDescription() {
        return description;
    }
}
