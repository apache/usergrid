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
package org.apache.usergrid.services.assets;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.services.AbstractPathBasedColllectionService;
import org.apache.usergrid.services.ServiceContext;
import org.apache.usergrid.services.ServiceResults;


public class AssetsService extends AbstractPathBasedColllectionService {

    private static final Logger logger = LoggerFactory.getLogger( AssetsService.class );


    public AssetsService() {
        super();
        logger.debug( "/assets" );
        declareServiceCommands( "data" );
    }


    @Override
    public ServiceResults getEntityCommand( ServiceContext context, List<EntityRef> refs, String command )
            throws Exception {
        logger.debug( "handling command: {}", command );

        ServiceResults sr = ServiceResults.genericServiceResults();

        return sr;
    }


    @Override
    public ServiceResults getServiceCommand( ServiceContext context, String command ) throws Exception {
        logger.debug( "in getServiceCommand with command: {}", command );
        return ServiceResults.genericServiceResults();
    }
}
