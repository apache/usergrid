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
package org.apache.usergrid.chop.api;


import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Properties;

import org.safehaus.guicyfig.Bypass;
import org.safehaus.guicyfig.OptionState;
import org.safehaus.guicyfig.Overrides;

import org.apache.commons.lang.NotImplementedException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;


/**
 * Builds a Project for use many by the plugin.
 */
public class ProjectBuilder {
    private Properties props;
    private Project supplied;
    private String testPackageBase;
    private String createTimestamp;
    private String artifactId;
    private String version;
    private String groupId;
    private String vcsRepoUrl;
    private String commitId;
    private String loadKey;
    private String chopVersion;
    private String md5;
    private String loadTime;


    public ProjectBuilder() {
        // do nothing - supplied will be injected if in Guice env
    }


    public ProjectBuilder( Properties props ) {
        this.props = props;
        updateValues();
    }


    public ProjectBuilder( Project project ) {
        // set the supplied project - this is manually provided
        this.supplied = project;
        updateValues();
    }


    @Inject
    public void setProject( Project project ) {
        if ( supplied == null ) {
            supplied = project;
            updateValues();
        }
    }


    private void updateValues() {
        if ( supplied != null ) {
            this.testPackageBase = supplied.getTestPackageBase();
            this.createTimestamp = supplied.getCreateTimestamp();
            this.artifactId = supplied.getArtifactId();
            this.version = supplied.getVersion();
            this.groupId = supplied.getGroupId();
            this.vcsRepoUrl = supplied.getVcsRepoUrl();
            this.commitId = supplied.getVcsVersion();
            this.loadKey = supplied.getLoadKey();
            this.chopVersion = supplied.getChopVersion();
            this.md5 = supplied.getMd5();
            this.loadTime = supplied.getLoadTime();
        }

        if ( props != null ) {
            if ( props.containsKey( Project.LOAD_TIME_KEY ) ) {
                this.loadTime = props.getProperty( Project.LOAD_TIME_KEY );
            }

            if ( props.containsKey( Project.LOAD_KEY ) ) {
                this.loadKey = props.getProperty( Project.LOAD_KEY );
            }

            if ( props.containsKey( Project.ARTIFACT_ID_KEY ) ) {
                this.artifactId = props.getProperty( Project.ARTIFACT_ID_KEY );
            }

            if ( props.containsKey( Project.CHOP_VERSION_KEY ) ) {
                this.chopVersion = props.getProperty( Project.CHOP_VERSION_KEY );
            }

            if ( props.containsKey( Project.CREATE_TIMESTAMP_KEY ) ) {
                this.createTimestamp = props.getProperty( Project.CREATE_TIMESTAMP_KEY );
            }

            if ( props.containsKey( Project.GIT_URL_KEY ) ) {
                this.vcsRepoUrl = props.getProperty( Project.GIT_URL_KEY );
            }

            if ( props.containsKey( Project.GIT_UUID_KEY ) ) {
                this.commitId = props.getProperty( Project.GIT_UUID_KEY );
            }

            if ( props.containsKey( Project.GROUP_ID_KEY ) ) {
                this.groupId = props.getProperty( Project.GROUP_ID_KEY );
            }

            if ( props.containsKey( Project.PROJECT_VERSION_KEY ) ) {
                this.version = props.getProperty( Project.PROJECT_VERSION_KEY );
            }

            if ( props.containsKey( Project.TEST_PACKAGE_BASE ) ) {
                this.testPackageBase = props.getProperty( Project.TEST_PACKAGE_BASE );
            }

            if ( props.containsKey( Project.MD5_KEY ) ) {
                this.md5 = props.getProperty( Project.MD5_KEY );
            }
        }
    }


    @JsonProperty
    public ProjectBuilder setTestPackageBase( final String testPackageBase ) {
        this.testPackageBase = testPackageBase;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setCreateTimestamp( final String timeStamp ) {
        this.createTimestamp = timeStamp;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setArtifactId( final String artifactId ) {
        this.artifactId = artifactId;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setProjectVersion( final String version ) {
        this.version = version;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setGroupId( final String groupId ) {
        this.groupId = groupId;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setVcsRepoUrl( final String vcsRepoUrl ) {
        this.vcsRepoUrl = vcsRepoUrl;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setVcsVersion( final String commitId ) {
        this.commitId = commitId;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setLoadKey( final String loadKey ) {
        this.loadKey = loadKey;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setChopVersion( final String version ) {
        this.chopVersion = version;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setMd5( final String md5 ) {
        this.md5 = md5;
        return this;
    }


    @JsonProperty
    public ProjectBuilder setLoadTime( final String loadTime ) {
        this.loadTime = loadTime;
        return this;
    }


    public Project getProject() {
        return new Project() {
            @Override
            public String getChopVersion() {
                return chopVersion;
            }


            @Override
            public String getCreateTimestamp() {
                return createTimestamp;
            }


            @Override
            public String getVcsVersion() {
                return commitId;
            }


            @Override
            public String getVcsRepoUrl() {
                return vcsRepoUrl;
            }


            @Override
            public String getGroupId() {
                return groupId;
            }


            @Override
            public String getArtifactId() {
                return artifactId;
            }


            @Override
            public String getVersion() {
                return version;
            }


            @Override
            public String getTestPackageBase() {
                return testPackageBase;
            }


            @Override
            public long getTestStopTimeout() {
                return Long.parseLong( DEFAULT_TEST_STOP_TIMEOUT );
            }


            @Override
            public String getLoadTime() {
                return loadTime;
            }


            @Override
            public String getLoadKey() {
                return loadKey;
            }


            @Override
            public String getMd5() {
                return md5;
            }


            @JsonIgnore
            @Override
            public void addPropertyChangeListener( final PropertyChangeListener listener ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public void removePropertyChangeListener( final PropertyChangeListener listener ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public OptionState[] getOptions() {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public OptionState getOption( final String key ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public String getKeyByMethod( final String methodName ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Object getValueByMethod( final String methodName ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Properties filterOptions( final Properties properties ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Map<String, Object> filterOptions( final Map<String, Object> entries ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public void override( final String key, final String override ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public boolean setOverrides( final Overrides overrides ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Overrides getOverrides() {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public void bypass( final String key, final String bypass ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public boolean setBypass( final Bypass bypass ) {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Bypass getBypass() {
                throw new NotImplementedException();
            }


            @JsonIgnore
            @Override
            public Class getFigInterface() {
                return Project.class;
            }


            @JsonIgnore
            @Override
            public boolean isSingleton() {
                return false;
            }
        };
    }
}
