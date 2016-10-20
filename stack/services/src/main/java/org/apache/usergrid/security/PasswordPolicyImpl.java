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

import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class PasswordPolicyImpl implements PasswordPolicy {

    private final PasswordPolicyFig passwordPolicyFig;


    @Inject
    PasswordPolicyImpl( PasswordPolicyFig passwordPolicyFig ) {
        this.passwordPolicyFig = passwordPolicyFig;
    }


    @Override
    public String getDescription( boolean isAdminUser ) {

        final int minLength;
        final int minUppercase;
        final int minDigits;
        final int minSpecialChars;

        if ( isAdminUser ) {
            minLength       = passwordPolicyFig.getMinLengthAdmin();
            minUppercase    = passwordPolicyFig.getMinUppercaseAdmin();
            minDigits       = passwordPolicyFig.getMinDigitsAdmin();
            minSpecialChars = passwordPolicyFig.getMinSpecialCharsAdmin();
        } else {
            minLength       = passwordPolicyFig.getMinLength();
            minUppercase    = passwordPolicyFig.getMinUppercase();
            minDigits       = passwordPolicyFig.getMinDigits();
            minSpecialChars = passwordPolicyFig.getMinSpecialChars();
        }

        StringBuilder sb = new StringBuilder();
        sb.append( "Password must be at least " ).append( minLength ).append(" characters. ");
        if ( minUppercase > 0 ) {
            sb.append( "Must include " ).append( minUppercase ).append(" uppercase characters. ");
        }
        if ( minDigits > 0 ) {
            sb.append( "Must include " ).append( minDigits ).append(" numbers. ");
        }
        if ( minSpecialChars > 0 ) {
            sb.append( "Must include " ).append( minUppercase ).append(" special characters. ");
        }
        return sb.toString();
    }


    @Override
    public Collection<String> policyCheck( String password, boolean isAdminUser ) {

        final int minLength;
        final int minUppercase;
        final int minDigits;
        final int minSpecialChars;

        if ( isAdminUser ) {
            minLength       = passwordPolicyFig.getMinLengthAdmin();
            minUppercase    = passwordPolicyFig.getMinUppercaseAdmin();
            minDigits       = passwordPolicyFig.getMinDigitsAdmin();
            minSpecialChars = passwordPolicyFig.getMinSpecialCharsAdmin();
        } else {
            minLength       = passwordPolicyFig.getMinLength();
            minUppercase    = passwordPolicyFig.getMinUppercase();
            minDigits       = passwordPolicyFig.getMinDigits();
            minSpecialChars = passwordPolicyFig.getMinSpecialChars();
        }

        return policyCheck( password, minLength, minUppercase, minDigits, minSpecialChars );
    }


    public Collection<String> policyCheck(
        String password, int minLength, int minUppercase, int minDigits, int minSpecialChars ) {


        List<String> violations = new ArrayList<>(3);

        // check length
        if ( password == null || password.length() < minLength ) {
            violations.add( PasswordPolicy.ERROR_LENGTH_POLICY
                + ": must be at least " + minLength + " characters" );
        }

        // count upper case
        if ( minUppercase > 0 ) {
            int upperCaseCount = 0;
            for (char c : password.toCharArray()) {
                if (StringUtils.isAllUpperCase( String.valueOf( c ) )) {
                    upperCaseCount++;
                }
            }
            if (upperCaseCount < minUppercase) {
                violations.add( PasswordPolicy.ERROR_UPPERCASE_POLICY
                    + ": requires " + minUppercase + " uppercase characters" );
            }
        }

        // count digits case
        if ( minDigits > 0 ) {
            int digitCount = 0;
            for (char c : password.toCharArray()) {
                if (StringUtils.isNumeric( String.valueOf( c ) )) {
                    digitCount++;
                }
            }
            if (digitCount < minDigits) {
                violations.add( PasswordPolicy.ERROR_DIGITS_POLICY
                    + ": requires " + minDigits + " digits" );
            }
        }

        // count special characters
        if ( minSpecialChars > 0 ) {
            int specialCharCount = 0;
            for (char c : password.toCharArray()) {
                if (passwordPolicyFig.getAllowedSpecialChars().contains( String.valueOf( c ) )) {
                    specialCharCount++;
                }
            }
            if (specialCharCount < minSpecialChars) {
                violations.add( PasswordPolicy.ERROR_SPECIAL_CHARS_POLICY
                    + ": requires " + minSpecialChars + " special characters" );
            }
        }

        return violations;
    }


}
