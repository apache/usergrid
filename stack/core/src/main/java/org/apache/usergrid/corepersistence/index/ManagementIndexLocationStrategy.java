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
package org.apache.usergrid.corepersistence.index;

import org.apache.usergrid.corepersistence.util.CpNamingUtils;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.core.scope.ApplicationScopeImpl;
import org.apache.usergrid.persistence.index.IndexAlias;
import org.apache.usergrid.persistence.index.IndexFig;
import org.apache.usergrid.persistence.index.IndexLocationStrategy;
import org.apache.usergrid.persistence.model.entity.Id;
import org.apache.usergrid.utils.StringUtils;

/**
 * Strategy for getting the management index name
 */
class ManagementIndexLocationStrategy implements IndexLocationStrategy {
    private final Id managementAppId;
    private final String prefix;
    private final IndexFig indexFig;
    private final CoreIndexFig coreIndexFig;

    public ManagementIndexLocationStrategy(final IndexFig indexFig, final CoreIndexFig coreIndexFig){
        this.indexFig = indexFig;
        this.coreIndexFig = coreIndexFig;
        this.managementAppId = CpNamingUtils.getManagementApplicationId();
        //remove usergrid
        this.prefix = coreIndexFig.getManagementAppIndexName().toLowerCase();  ////use lowercase value


    }
    @Override
    public IndexAlias getAlias() {
        return new IndexAlias(indexFig,prefix);
    }

    @Override
    public String getIndex(String suffix) {
        if (suffix != null) {
            return prefix + "_" + suffix;
        } else {
            return prefix;
        }
    }

    @Override
    public ApplicationScope getApplicationScope() {
        return new ApplicationScopeImpl(managementAppId);
    }

    @Override
    public int getNumberOfShards() {
        return coreIndexFig.getManagementNumberOfShards();
    }

    @Override
    public int getNumberOfReplicas() {
        return coreIndexFig.getManagementNumberOfReplicas();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ManagementIndexLocationStrategy that = (ManagementIndexLocationStrategy) o;

        if (!managementAppId.equals(that.managementAppId)) return false;
        return prefix.equals(that.prefix);

    }

    @Override
    public int hashCode() {
        int result = managementAppId.hashCode();
        result = 31 * result + prefix.hashCode();
        return result;
    }
}
