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
package org.apache.usergrid.chop.webapp.elasticsearch;


import org.elasticsearch.common.io.FileSystemUtils;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.safehaus.jettyjam.utils.StartableResource;

import java.io.File;


public class ElasticSearchResource implements StartableResource {
    private final EsEmbedded embedded = new EsEmbedded();


    @Override
    public void start(Description description) throws Exception {
        FileSystemUtils.deleteRecursively(new File(embedded.getConfig().getDataDir()));
        embedded.start();
    }


    public ElasticSearchFig getConfig() {
        return embedded.getConfig();
    }


    @Override
    public void stop(Description description) {
        embedded.stop();
        FileSystemUtils.deleteRecursively(new File(embedded.getConfig().getDataDir()));
    }


    @Override
    public boolean isStarted() {
        return embedded.isStarted();
    }


    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                start(description);
                try {
                    base.evaluate();
                } finally {
                    stop(description);
                }
            }
        };
    }
}
