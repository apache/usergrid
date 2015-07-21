/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at:223
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
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.UserInfo;
import org.apache.usergrid.persistence.Entity;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.utils.UUIDUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.ClassRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static org.apache.usergrid.persistence.cassandra.CassandraService.MANAGEMENT_APPLICATION_ID;
import static org.junit.Assert.*;


public class ExportImportAdminsTest {
    static final Logger logger = LoggerFactory.getLogger( ExportImportAdminsTest.class );
    
    @ClassRule
    public static ServiceITSetup setup = new ServiceITSetupImpl( ServiceITSuite.cassandraResource );

    @org.junit.Test
    public void testExportUserAndOrg() throws Exception {

        // create two orgs each with owning user

        final String random1 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo1 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random1, "user_" + random1, "user_" + random1,
                "user_" + random1 + "@example.com", "password" );

        final String random2 = RandomStringUtils.randomAlphanumeric( 10 );
        final OrganizationOwnerInfo orgOwnerInfo2 = setup.getMgmtSvc().createOwnerAndOrganization(
                "org_" + random2, "user_" + random2, "user_" + random2,
                "user_" + random2 + "@example.com", "password" );

        // Add user1 to org2

        setup.getMgmtSvc().addAdminUserToOrganization(
                orgOwnerInfo1.getOwner(), orgOwnerInfo2.getOrganization(), false );

        setup.getMgmtSvc().addAdminUserToOrganization(
                orgOwnerInfo1.getOwner(), orgOwnerInfo2.getOrganization(), false );

        // export to file

        String directoryName = "./target/export" + RandomStringUtils.randomAlphanumeric(10);

        ExportAdmins exportAdmins = new ExportAdmins();
        exportAdmins.startTool( new String[] {
            "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort(),
            "-outputDir", directoryName
        }, false );

        // read, parse and verify files

        // first, the admin users file

        File directory = new File( directoryName );
        String[] adminUsersFileNames = directory.list( new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("admin-users.");
            }
        });

        // only one. read it into a map

        File adminUsersFile = new File(
                directory.getAbsolutePath() + File.separator + adminUsersFileNames[0] );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree( adminUsersFile );
        assertTrue( node.isArray() );

        // does file contain our two admin users?

        Set<String> usernames = new HashSet<String>();
        for ( int i=0; i<node.size(); i++) {
            JsonNode jsonNode = node.get( i );
            usernames.add( jsonNode.get("username").asText() );
        }
        assertTrue( usernames.contains( "user_" + random1 ));
        assertTrue( usernames.contains( "user_" + random2 ));

        // second, the metadata file

        String[] metadataFileNames = directory.list( new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("admin-user-metadata.");
            }
        });

        // only one, read it into a map

        File metadataFile = new File(
                directory.getAbsolutePath() + File.separator + metadataFileNames[0] );

        mapper = new ObjectMapper();
        node = mapper.readTree( metadataFile );
        assertTrue( node.isObject() );

        // do users belong to correct orgs

        JsonNode user1node = node.findValue( orgOwnerInfo1.getOwner().getUuid().toString() );
        JsonNode orgs1 = user1node.findValue( "organizations");
        assertEquals( 2, orgs1.size() );

        JsonNode user2node = node.findValue( orgOwnerInfo2.getOwner().getUuid().toString() );
        JsonNode orgs2 = user2node.findValue( "organizations");
        assertEquals( 1, orgs2.size() );
    }


    @org.junit.Test
    public void testImportAdminUsersAndOrgs() throws Exception {

        // first: generate the data file with unique user and org IDs and names
        
        // data contains three users each with a unique org, one user has a duplicate email

        String rand1 = RandomStringUtils.randomAlphanumeric( 10 );
        String rand2 = RandomStringUtils.randomAlphanumeric( 10 );
        String rand3 = RandomStringUtils.randomAlphanumeric( 10 );

        UUID user_uuid_1 = UUIDUtils.newTimeUUID();
        UUID user_uuid_2 = UUIDUtils.newTimeUUID();
        UUID user_uuid_3 = UUIDUtils.newTimeUUID();

        UUID org_uuid_1  = UUIDUtils.newTimeUUID();
        UUID org_uuid_2  = UUIDUtils.newTimeUUID();
        UUID org_uuid_3  = UUIDUtils.newTimeUUID();

        String user_name_1 = "user1_" + rand1;
        String user_name_2 = "user2_" + rand2;
        String user_name_3 = "user3_" + rand3;

        String org_name_1  = "org1_"  + rand1;
        String org_name_2  = "org2_"  + rand2;
        String org_name_3  = "org3_"  + rand3;

        // loop through resource files with prefix 'admin-user' those are the data file templates

        File resourcesDir = new File("./target/test-classes");
        String[] fileNames = resourcesDir.list();
        File tempDir = Files.createTempDir();

        for ( String fileName : fileNames ) {

            if ( fileName.startsWith("admin-user")) {

                // substitute our new unique IDs and names and write data files to temp directory

                String fileContent = IOUtils.toString( new FileInputStream(
                        resourcesDir.getAbsolutePath() + File.separator + fileName ) );

                fileContent = fileContent.replaceAll( "USER_UUID_1", user_uuid_1.toString() );
                fileContent = fileContent.replaceAll( "USER_UUID_2", user_uuid_2.toString() );
                fileContent = fileContent.replaceAll( "USER_UUID_3", user_uuid_3.toString() );

                fileContent = fileContent.replaceAll( "ORG_UUID_1",  org_uuid_1.toString() );
                fileContent = fileContent.replaceAll( "ORG_UUID_2",  org_uuid_2.toString() );
                fileContent = fileContent.replaceAll( "ORG_UUID_3",  org_uuid_3.toString() );

                fileContent = fileContent.replaceAll( "USER_NAME_1", user_name_1 );
                fileContent = fileContent.replaceAll( "USER_NAME_2", user_name_2 );
                fileContent = fileContent.replaceAll( "USER_NAME_3", user_name_3 );

                fileContent = fileContent.replaceAll( "ORG_NAME_1", org_name_1 );
                fileContent = fileContent.replaceAll( "ORG_NAME_2", org_name_2 );
                fileContent = fileContent.replaceAll( "ORG_NAME_3", org_name_3 );

                FileOutputStream os = new FileOutputStream(
                        tempDir.getAbsolutePath() + File.separator + fileName );

                IOUtils.write( fileContent, os );
                os.close();
            }
        }

        // import data from temp directory

        ImportAdmins importAdmins = new ImportAdmins();
        importAdmins.startTool( new String[]{
                "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort(),
                "-inputDir", tempDir.getAbsolutePath()
        }, false );

        // verify that users and orgs were created correctly

        OrganizationInfo orgInfo1 = setup.getMgmtSvc().getOrganizationByUuid( org_uuid_1 );
        assertNotNull( "org 1 exists", orgInfo1 );
        List<UserInfo> org1_users = setup.getMgmtSvc().getAdminUsersForOrganization( org_uuid_1 );
        assertEquals("org1 has one user", 1, org1_users.size() );

        OrganizationInfo orgInfo2 = setup.getMgmtSvc().getOrganizationByUuid( org_uuid_2 );
        assertNotNull( "org 2 exists", orgInfo2 );
        List<UserInfo> org2_users = setup.getMgmtSvc().getAdminUsersForOrganization( org_uuid_2 );
        assertEquals( "org2 has two users", 2, org2_users.size() );
        
        OrganizationInfo orgInfo3 = setup.getMgmtSvc().getOrganizationByUuid( org_uuid_3 );
        assertNotNull( "org 3 exists", orgInfo3 );
        List<UserInfo> org3_users = setup.getMgmtSvc().getAdminUsersForOrganization( org_uuid_3 );
        assertEquals( "org 3 has 1 users", 1, org3_users.size() );

        BiMap<UUID, String> user1_orgs = setup.getMgmtSvc().getOrganizationsForAdminUser( user_uuid_1 );
        assertEquals( "user 1 has 2 orgs", 2, user1_orgs.size() );
        
        BiMap<UUID, String> user2_orgs = setup.getMgmtSvc().getOrganizationsForAdminUser( user_uuid_2 );
        assertEquals( "user 2 has two orgs gained one from duplicate", 2, user2_orgs.size() );

        try {
            BiMap<UUID, String> user3_orgs = setup.getMgmtSvc().getOrganizationsForAdminUser( user_uuid_3 );
            fail("fetch user 3 should have thrown exception");
        } catch ( Exception expected ) {
            logger.info("EXCEPTION EXPECTED");
        }

        EntityManager em = setup.getEmf().getEntityManager( MANAGEMENT_APPLICATION_ID );
        Entity user3 = em.get( user_uuid_3 );
        assertNull( "duplicate user does not exist", user3 );


    }
}