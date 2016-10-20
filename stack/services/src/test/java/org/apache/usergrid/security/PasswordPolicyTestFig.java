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

import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.OptionState;
import org.safehaus.guicyfig.Overrides;

import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Properties;


public class PasswordPolicyTestFig implements PasswordPolicyFig {


    @Override
    public int getMinUppercaseAdmin() {
        return 1;
    }

    @Override
    public int getMinUppercase() {
        return 1;
    }

    @Override
    public int getMinDigitsAdmin() {
        return 1;
    }

    @Override
    public int getMinDigits() {
        return 1;
    }

    @Override
    public int getMinSpecialCharsAdmin() {
        return 1;
    }

    @Override
    public int getMinSpecialChars() {
        return 1;
    }

    @Override
    public int getMinLengthAdmin() {
        return 1;
    }

    @Override
    public int getMinLength() {
        return 1;
    }

    @Override
    public String getAllowedSpecialChars() {
        return null;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {

    }

    @Override
    public OptionState[] getOptions() {
        return new OptionState[0];
    }

    @Override
    public OptionState getOption(String key) {
        return null;
    }

    @Override
    public String getKeyByMethod(String methodName) {
        return null;
    }

    @Override
    public Object getValueByMethod(String methodName) {
        return null;
    }

    @Override
    public Properties filterOptions(Properties properties) {
        return null;
    }

    @Override
    public Map<String, Object> filterOptions(Map<String, Object> entries) {
        return null;
    }

    @Override
    public void override(String key, String override) {

    }

    @Override
    public boolean setOverrides(Overrides overrides) {
        return false;
    }

    @Override
    public Overrides getOverrides() {
        return null;
    }

    @Override
    public void bypass(String key, String bypass) {

    }

    @Override
    public boolean setBypass(Bypass bypass) {
        return false;
    }

    @Override
    public Bypass getBypass() {
        return null;
    }

    @Override
    public Class getFigInterface() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
