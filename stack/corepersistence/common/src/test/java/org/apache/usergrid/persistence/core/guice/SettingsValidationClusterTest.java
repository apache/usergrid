package org.apache.usergrid.persistence.core.guice;

import org.apache.usergrid.persistence.core.guicyfig.ClusterFig;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by russo on 8/27/15.
 */
public class SettingsValidationClusterTest {

    @Test
    public void clusterValidationSuccess(){

        final String myCluster = "myCluster";

        ClusterFig clusterFig = mock(ClusterFig.class);
        when(clusterFig.getClusterName()).thenReturn(myCluster);


        new SettingsValidationCluster(clusterFig);

    }

    @Test(expected=IllegalArgumentException.class)
    public void clusterValidationFailure(){

        final String myPrefix = ClusterFig.VALIDATION_DEFAULT_VALUE;

        ClusterFig clusterFig = mock(ClusterFig.class);
        when(clusterFig.getClusterName()).thenReturn(myPrefix);

        new SettingsValidationCluster(clusterFig);

    }

}
