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
package org.apache.usergrid.system;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UsergridFeatures {

    public static final String USERGRID_FEATURES_ENABLED_PROP = "usergrid.features.enabled";

    public enum Feature {

        ALL, GRAPH, KVM

    }


    public static Collection<Feature> getFeaturesEnabled(){

        List<Feature> features = new ArrayList<>();

        String featureString = System.getProperty(USERGRID_FEATURES_ENABLED_PROP, "all");

        String[] splitFeatures = featureString.split(",");
        for(String feature : splitFeatures){

            features.add(Feature.valueOf(feature.toUpperCase()));

        }

        return features;

    }

    public static boolean isGraphFeatureEnabled(){

        return getFeaturesEnabled().contains(Feature.ALL) || getFeaturesEnabled().contains(Feature.GRAPH);

    }

    public static boolean isQueryFeatureEnabled(){

        return getFeaturesEnabled().contains(Feature.ALL);

    }

    public static boolean isKvmFeatureEnabled(){

        return true;

    }
}
