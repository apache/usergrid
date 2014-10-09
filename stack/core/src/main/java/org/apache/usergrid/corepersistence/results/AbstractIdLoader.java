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

package org.apache.usergrid.corepersistence.results;/*
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


import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.usergrid.corepersistence.CpNamingUtils;
import org.apache.usergrid.persistence.Results;
import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.impl.CollectionScopeImpl;
import org.apache.usergrid.persistence.core.scope.ApplicationScope;
import org.apache.usergrid.persistence.index.IndexScope;
import org.apache.usergrid.persistence.index.impl.IndexScopeImpl;
import org.apache.usergrid.persistence.index.query.CandidateResult;
import org.apache.usergrid.persistence.index.query.CandidateResults;
import org.apache.usergrid.persistence.model.entity.Id;


public abstract class AbstractIdLoader implements  ResultsLoader{

    @Override
    public Results getResults( final ApplicationScope applicationScope, final CandidateResults crs ) {
//        Map<Id, CandidateResult> latestVersions = new LinkedHashMap<Id, CandidateResult>();
//
//               Iterator<CandidateResult> iter = crs.iterator();
//               while ( iter.hasNext() ) {
//
//                   CandidateResult cr = iter.next();
//
//                   CollectionScope collScope = new CollectionScopeImpl(
//                       applicationScope.getApplication(),
//                       applicationScope.getApplication(),
//                       CpNamingUtils.getCollectionScopeNameFromEntityType( cr.getId().getType() ));
//
//                   EntityCollectionManager ecm = managerCache.getEntityCollectionManager(collScope);
//
//                   UUID latestVersion = ecm.getLatestVersion( cr.getId() ).toBlocking().lastOrDefault(null);
//
//                   if ( logger.isDebugEnabled() ) {
//                       logger.debug("Getting version for entity {} from scope\n   app {}\n   owner {}\n   name {}",
//                       new Object[] {
//                           cr.getId(),
//                           collScope.getApplication(),
//                           collScope.getOwner(),
//                           collScope.getName()
//                       });
//                   }
//
//                   if ( latestVersion == null ) {
//                       logger.error("Version for Entity {}:{} not found",
//                               cr.getId().getType(), cr.getId().getUuid());
//                       continue;
//                   }
//
//                   if ( cr.getVersion().compareTo( latestVersion) < 0 )  {
//                       logger.debug("Stale version of Entity uuid:{} type:{}, stale v:{}, latest v:{}",
//                           new Object[] { cr.getId().getUuid(), cr.getId().getType(),
//                               cr.getVersion(), latestVersion});
//
//                       IndexScope indexScope = new IndexScopeImpl(
//                           cpHeadEntity.getId(),
//                           CpNamingUtils.getCollectionScopeNameFromEntityType( cr.getId().getType() ));
//                       indexBatch.deindex( indexScope, cr);
//
//                       continue;
//                   }
//
//                   CandidateResult alreadySeen = latestVersions.get( cr.getId() );
//
//                   if ( alreadySeen == null ) { // never seen it, so add to map
//                       latestVersions.put( cr.getId(), cr );
//
//                   } else {
//                       // we seen this id before, only add entity if we now have newer version
//                       if ( latestVersion.compareTo( alreadySeen.getVersion() ) > 0 ) {
//
//                           latestVersions.put( cr.getId(), cr);
//
//                           IndexScope indexScope = new IndexScopeImpl(
//                               cpHeadEntity.getId(),
//                               CpNamingUtils.getCollectionScopeNameFromEntityType( cr.getId().getType() ));
//                           indexBatch.deindex( indexScope, alreadySeen);
//                       }
//                   }
//               }
//
//               indexBatch.execute();
        return null;
    }
}
