package org.apache.usergrid.perftest.settings;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Module;
import org.apache.usergrid.perftest.Perftest;

import java.util.ArrayList;
import java.util.List;


/**
 * Test specific information.
 */
public class TestInfo {
    private final Perftest userPerftest;
    private final Module userModule;
    private final String perftestVersion = PropSettings.getPerftestVersion();
    private final List<RunInfo> runInfos = new ArrayList<RunInfo>();
    private final String perftestFormation = PropSettings.getFormation();
    private final String createTimestamp = PropSettings.getCreateTimestamp();
    private final String gitUuid = PropSettings.getGitUuid();
    private final String getGitRepoUrl = PropSettings.getGitUrl();
    private final String getGroupId = PropSettings.getGroupId();
    private final String getArtifactId = PropSettings.getArtifactId();
    private final String loadKey;
    private String loadTime;


    public TestInfo( Perftest userPerftest, Module userModule ) {
        this.userPerftest = userPerftest;
        this.userModule = userModule;

        StringBuilder sb = new StringBuilder();
        sb.append( "tests/" )
                .append(gitUuid)
                .append('-')
                .append( createTimestamp )
                .append( '/' )
                .append( "perftest.war" );
        loadKey = sb.toString();
    }


    @JsonProperty
    public Perftest getUserPerftest() {
        return userPerftest;
    }


    @JsonProperty
    public String getUserModuleFQCN() {
        return userModule.getClass().getCanonicalName();
    }


    @JsonProperty
    public String getPerftestVersion() {
        return perftestVersion;
    }


    @JsonProperty
    public List<RunInfo> getRunInfos() {
        return runInfos;
    }


    public void addRunInfo( RunInfo runInfo ) {
        runInfos.add( runInfo );
    }


    @JsonProperty
    public String getPerftestFormation() {
        return perftestFormation;
    }


    @JsonProperty
    public String getCreateTimestamp() {
        return createTimestamp;
    }


    @JsonProperty
    public String getGitUuid() {
        return gitUuid;
    }


    @JsonProperty
    public String getGetGitRepoUrl() {
        return getGitRepoUrl;
    }


    @JsonProperty
    public String getGetGroupId() {
        return getGroupId;
    }


    @JsonProperty
    public String getGetArtifactId() {
        return getArtifactId;
    }


    @JsonProperty
    public String getLoadKey() {
        return loadKey;
    }


    @JsonProperty
    public String getLoadTime() {
        return loadTime;
    }


    @SuppressWarnings("UnusedDeclaration")
    public void setLoadTime(String loadTime) {
        this.loadTime = loadTime;
    }
}
