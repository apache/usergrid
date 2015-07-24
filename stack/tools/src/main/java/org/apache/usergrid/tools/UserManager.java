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
package org.apache.usergrid.tools;


import com.google.common.collect.BiMap;
import org.apache.usergrid.management.UserInfo;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.entities.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.apache.usergrid.utils.JsonUtils.mapToFormattedJsonString;


/** @author zznate */
public class UserManager extends ToolBase {

    @Override
    public Options createOptions() {
        Options options = super.createOptions();
        options.addOption( "u", "username", true, "The username to lookup" );
        options.addOption( "p", "password", true, "The password to set for the user" );
        return options;
    }


    @Override
    public void runTool( CommandLine line ) throws Exception {
        startSpring();
        String userName = line.getOptionValue( "u" );

        UserInfo userInfo = managementService.findAdminUser( userName );
        if ( userInfo == null ) {
            logger.info( "user {} not found", userName );
            return;
        }

        logger.info("--- User information:");
        logger.info( mapToFormattedJsonString( userInfo ) );

        logger.info("--- User organizations:");
        final BiMap<UUID, String> orgs = managementService.getOrganizationsForAdminUser( userInfo.getUuid() );
        logger.info( mapToFormattedJsonString( orgs ) );

        logger.info("--- User dictionaries:");
        EntityManager em = emf.getEntityManager( CassandraService.MANAGEMENT_APPLICATION_ID );
        User user = em.get( userInfo.getUuid(), User.class );
        Set<String> dictionaries = em.getDictionaries( user );
        for (String dictionary : dictionaries) {
            Map<Object, Object> dict = em.getDictionaryAsMap( user, dictionary );
            logger.info( dictionary + " : " + mapToFormattedJsonString( dict ) );
        }

        if ( line.hasOption( "p" ) ) {
            String password = line.getOptionValue( "p" );
            managementService.setAdminUserPassword( userInfo.getUuid(), password );
            logger.info( "new password match?: " + managementService
                    .verifyAdminUserPassword( userInfo.getUuid(), password ) );
        }
    }
}
