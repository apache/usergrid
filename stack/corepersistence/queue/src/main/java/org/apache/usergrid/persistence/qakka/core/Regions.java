/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.persistence.qakka.core;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.usergrid.persistence.actorsystem.ActorSystemFig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Singleton
public class Regions {
    public static final String LOCAL = "LOCAL";
    public static final String ALL = "ALL";
    public static final String REMOTE = "REMOTE";

    // load regions from properties
    String localRegion;
    List<String> regionList;


    @Inject
    public Regions( ActorSystemFig actorSystemFig ) {
        localRegion = actorSystemFig.getRegionLocal();
        regionList = Arrays.asList( actorSystemFig.getRegionsList().split(","));
    }


    public List<String> getRegions(String region) {
        List<String> ret = null;

        switch (region) {
            case ALL:
                ret = new ArrayList<>(regionList);
                break;
            case LOCAL:
                ret = Collections.singletonList(localRegion);
                break;
            case REMOTE:
                ret = new ArrayList<>(regionList);
                ret.remove(localRegion);
                break;
            default:
                // parse regions into list -- assume a single region now, but can do region1,region2

                // validate regions

                ret = Collections.singletonList(region);
                break;
        }

        return ret;
    }

    public String getLocalRegion() {
        return localRegion;
    }

    public Boolean isValidRegion(String region) {
        return regionList.contains(region);
    }

}
