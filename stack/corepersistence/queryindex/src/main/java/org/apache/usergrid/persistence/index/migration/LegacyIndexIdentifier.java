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
package org.apache.usergrid.persistence.index.migration;

import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexIdentifier;
import org.apache.usergrid.persistence.index.impl.IndexingUtils;

/**
 * Class is used to generate an index name and alias name the old way via app name
 */
public class LegacyIndexIdentifier implements IndexIdentifier {
    private final IndexFig config;
    private final ApplicationScope applicationScope;

    public LegacyIndexIdentifier(IndexFig config, ApplicationScope applicationScope) {
        this.config = config;
        this.applicationScope = applicationScope;
    }

    /**
     * Get the alias name
     * @return
     */
    public IndexAlias getAlias() {
        return new IndexAlias(config,getIndexBase());
    }

    /**
     * Get index name, send in additional parameter to add incremental indexes
     * @param suffix
     * @return
     */
    public String getIndex(String suffix) {
        if (suffix != null) {
            return getIndexBase() + "_" + suffix;
        } else {
            return getIndexBase();
        }
    }

    /**
     * returns the base name for index which will be used to add an alias and index
     * @return
     */
    private String getIndexBase() {
        StringBuilder sb = new StringBuilder();
        sb.append(config.getIndexPrefix()).append(IndexingUtils.SEPARATOR);
        IndexingUtils.idString(sb, applicationScope.getApplication());
        return sb.toString();
    }



    public String toString() {
        return "application: " + applicationScope.getApplication().getUuid();
    }

}
