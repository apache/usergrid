/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.usergrid.chop.stack;


import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 * A basic implementation of a InstanceSpec.
 */
public class BasicInstanceSpec implements InstanceSpec
{
    private String imageId;
    private String type;
    private String keyName;
    private List<URL> setupScripts = new ArrayList<URL>();
    private List<URL> runnerScripts = new ArrayList<URL>();
    private Properties scriptEnvironment = new Properties();



    @Override
    public String getImageId() {
        return imageId;
    }


    public BasicInstanceSpec setImageId( String imageId ) {
        this.imageId = imageId;
        return this;
    }


    @Override
    public String getType() {
        return type;
    }


    public BasicInstanceSpec setType( final String type ) {
        this.type = type;
        return this;
    }


    @Override
    public String getKeyName() {
        return keyName;
    }


    public BasicInstanceSpec setKeyName( final String keyName ) {
        this.keyName = keyName;
        return this;
    }


    @Override
    public List<URL> getSetupScripts() {
        return setupScripts;
    }

    @Override
    public List<URL> getRunnerScripts() {
        return runnerScripts;
    }


    public BasicInstanceSpec setSetupScripts( final List<URL> setupScripts ) {
        this.setupScripts = setupScripts;
        return this;
    }


    public BasicInstanceSpec setRunnerScripts( final List<URL> runnerScripts ) {
        this.runnerScripts = runnerScripts;
        return this;
    }


    public BasicInstanceSpec addSetupScript( URL setupScript ) {
        this.setupScripts.add( setupScript );
        return this;
    }


    @Override
    public Properties getScriptEnvironment() {
        return scriptEnvironment;
    }


    public BasicInstanceSpec setScriptEnvironment( final Properties scriptEnvironment ) {
        this.scriptEnvironment = scriptEnvironment;
        return this;
    }


    public BasicInstanceSpec setScriptEnvProperty( String key, String value ) {
        this.scriptEnvironment.setProperty( key, value );
        return this;
    }
}
