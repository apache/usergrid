/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one or more
 *  *  contributor license agreements.  The ASF licenses this file to You
 *  * under the Apache License, Version 2.0 (the "License"); you may not
 *  * use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.  For additional information regarding
 *  * copyright in this work, please see the NOTICE file in the top level
 *  * directory of this distribution.
 *
 */

package org.apache.usergrid.persistence.index.impl;

import com.google.inject.Inject;

import com.netflix.astyanax.Keyspace;
import org.apache.usergrid.persistence.core.util.StringUtils;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexIdentifier;


/**
 * Class is used to generate an index name and alias name
 */
public class IndexIdentifierv2Impl implements IndexIdentifier {
    private final IndexFig config;
    private final String prefix;

    @Inject
    public IndexIdentifierv2Impl(final Keyspace keyspace, final IndexFig config) {
        this.config = config;
        this.prefix = getPrefix(keyspace, config);
    }

    private String getPrefix(Keyspace keyspace, IndexFig config) {
        //remove usergrid
        final String indexPrefixConfig = StringUtils.isNotEmpty(config.getIndexPrefix())
            ? config.getIndexPrefix().toLowerCase()  ////use lowercase value
            : "usergrid"; // default to something so its not null
        final String keyspaceName = keyspace.getKeyspaceName().toLowerCase();
        //check for repetition
        final boolean removePrefix = indexPrefixConfig.contains("usergrid") && keyspaceName.contains("usergrid");
        return !removePrefix
            ? indexPrefixConfig + "_" + keyspaceName
            : keyspaceName;
    }

    /**
     * Get the alias name
     * @return
     */
    @Override
    public IndexAlias getAlias() {
        return new IndexAlias(config,prefix);
    }

    /**
     * Get index name, send in additional parameter to add incremental indexes
     * @param suffix
     * @return
     */
    @Override
    public String getIndex(String suffix) {
        if (suffix != null) {
            return prefix + "_" + suffix;
        } else {
            return prefix;
        }
    }


    @Override
    public String toString() {
        return "index id: "+prefix;
    }
}
