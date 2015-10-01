package org.apache.usergrid.persistence.core.guice;


import org.apache.usergrid.persistence.core.guicyfig.ClusterFig;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Created by russo on 8/27/15.
 */
@Singleton
public class SettingsValidationCluster {

    @Inject
    public SettingsValidationCluster(final ClusterFig clusterFig) {

        final String configuredClusterName = clusterFig.getClusterName();
        final String defaultCluster = ClusterFig.VALIDATION_DEFAULT_VALUE;

        Preconditions.checkArgument(!configuredClusterName.equalsIgnoreCase(defaultCluster), ClusterFig.CLUSTER_NAME_PROPERTY + " property must be set.");

    }

}
