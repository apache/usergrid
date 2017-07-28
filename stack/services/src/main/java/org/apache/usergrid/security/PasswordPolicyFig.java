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

import org.safehaus.guicyfig.Default;
import org.safehaus.guicyfig.FigSingleton;
import org.safehaus.guicyfig.GuicyFig;
import org.safehaus.guicyfig.Key;


@FigSingleton
public interface PasswordPolicyFig extends GuicyFig {

    String ALLOWED_SPECIAL_CHARS       = "usergrid.password-policy.allowed-special-chars";

    String MIN_UPPERCASE_ADMIN         = "usergrid.password-policy.min-uppercase-admin";
    String MIN_UPPERCASE               = "usergrid.password-policy.min-uppercase";

    String MIN_DIGITS_ADMIN            = "usergrid.password-policy.min-digits-admin";
    String MIN_DIGITS                  = "usergrid.password-policy.min-digits";

    String MIN_SPECIAL_CHARS_ADMIN     = "usergrid.password-policy.min-special-chars-admin";
    String MIN_SPECIAL_CHARS           = "usergrid.password-policy.min-special-chars";

    String MIN_LENGTH_ADMIN            = "usergrid.password-policy.min-length-admin";
    String MIN_LENGTH                  = "usergrid.password-policy.min-length";


    @Key(MIN_UPPERCASE_ADMIN)
    @Default("0")
    int getMinUppercaseAdmin();

    @Key(MIN_UPPERCASE)
    @Default("0")
    int getMinUppercase();

    @Key(MIN_DIGITS_ADMIN)
    @Default("0")
    int getMinDigitsAdmin();

    @Key(MIN_DIGITS)
    @Default("0")
    int getMinDigits();

    @Key(MIN_SPECIAL_CHARS_ADMIN)
    @Default("0")
    int getMinSpecialCharsAdmin();

    @Key(MIN_SPECIAL_CHARS)
    @Default("0")
    int getMinSpecialChars();

    @Key(MIN_LENGTH_ADMIN)
    @Default("4")
    int getMinLengthAdmin();

    @Key(MIN_LENGTH)
    @Default("4")
    int getMinLength();

    @Key(ALLOWED_SPECIAL_CHARS)
    @Default("`~!@#$%^&*()-_=+[{]}\\|;:'\",<.>/?")
    String getAllowedSpecialChars();
}
