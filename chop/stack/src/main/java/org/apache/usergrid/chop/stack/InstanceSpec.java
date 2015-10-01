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
import java.util.List;
import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;


/**
 * An IaaS provider independent instance specification encapsulates all the information
 * needed to create, configure, and enable access to an instance.
 */
@JsonDeserialize( as = BasicInstanceSpec.class )
public interface InstanceSpec {
    /**
     * Gets the IaaS identifier for a base image (template) used for creating instances.
     *
     * @return the image identifier specific to IaaS provider
     */
    @JsonProperty
    String getImageId();

    /**
     * Gets the IaaS identifier for the instance type. This is very provider specific.
     *
     * @return the instance type
     */
    @JsonProperty
    String getType();

    /**
     * Private key pair name used to authenticate into instances.
     *
     * @return the private key pair name
     */
    @JsonProperty
    String getKeyName();

    /**
     * A list of scripts executed on newly created instances of this instance specification.
     *
     * @return the setup scripts
     */
    @JsonProperty
    List<URL> getSetupScripts();

    /**
     * A list of scripts executed on newly created runner instances of this instance specification.
     *
     * @return the runner scripts
     */
    @JsonProperty
    List<URL> getRunnerScripts();

    /**
     * The environment properties to inject into the shell before executing setup scripts.
     */
    @JsonProperty
    Properties getScriptEnvironment();
}
