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

package org.apache.usergrid.security;


import java.util.Collection;


/**
 * Interface to password policy.
 */
public interface PasswordPolicy {

    String ERROR_POLICY_VIOLIATION    = "error_password_policy_violation";

    String ERROR_UPPERCASE_POLICY     = "error_uppercase_policy";

    String ERROR_DIGITS_POLICY        = "error_digits_policy";

    String ERROR_SPECIAL_CHARS_POLICY = "error_special_chars_policy";

    String ERROR_LENGTH_POLICY        = "error_length_policy";


    /**
     * Check to see if password conforms to policy.
     *
     * @param password Password to check.
     * @return Collection of error strings, one for each policy violated or empty if password conforms.
     */
    Collection<String> policyCheck( String password, boolean isAdminUser );


    /**
     * Get description of password policy for error messages.
     */
    String getDescription( boolean isAdminUser );
}
