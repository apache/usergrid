package org.apache.usergrid.chop.webapp.dao.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * A help class to group runners by user, commit and module.
 */
public class RunnerGroup {

    private String user;
    private String commitId;
    private String moduleId;

    public RunnerGroup(String user, String commitId, String moduleId) {
        this.user = user;
        this.commitId = commitId;
        this.moduleId = moduleId;
    }

    /**
     * By default ElasticSearch ignores the dash in search. We need to fix this to get correct result.
     */
    public String getId() {
        return "" + Math.abs(hashCode());
    }

    public String getUser() {
        return user;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getModuleId() {
        return moduleId;
    }

    /**
     * Returns true if any of values (user, commit, module) is null
     */
    public boolean isNull() {
        return StringUtils.isEmpty(user)
                || StringUtils.isEmpty(commitId)
                || StringUtils.isEmpty(moduleId);
    }

    @Override
    public boolean equals(Object v) {
        if (!(v instanceof RunnerGroup)) {
            return false;
        }

        RunnerGroup other = (RunnerGroup) v;

        return new EqualsBuilder()
                .append(user, other.getUser())
                .append(commitId, other.getCommitId())
                .append(moduleId, other.getModuleId())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(user)
                .append(commitId)
                .append(moduleId)
                .hashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("user", user)
                .append("commitId", commitId)
                .append("moduleId", moduleId)
                .toString();
    }
}
