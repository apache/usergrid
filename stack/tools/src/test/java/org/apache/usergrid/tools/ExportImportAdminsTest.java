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
import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.usergrid.ServiceITSetup;
import org.apache.usergrid.ServiceITSetupImpl;
import org.apache.usergrid.ServiceITSuite;
import org.apache.usergrid.management.OrganizationInfo;
import org.apache.usergrid.management.OrganizationOwnerInfo;
import org.apache.usergrid.management.UserInfo;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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

        String rand1 = RandomStringUtils.randomAlphanumeric( 10 );
        String rand2 = RandomStringUtils.randomAlphanumeric( 10 );

        UUID user_uuid_1 = UUIDUtils.newTimeUUID();
        UUID user_uuid_2 = UUIDUtils.newTimeUUID();

        UUID org_uuid_1  = UUIDUtils.newTimeUUID();
        UUID org_uuid_2  = UUIDUtils.newTimeUUID();

        String user_name_1 = "user_" + rand1;
        String user_name_2 = "user_" + rand2;

        String org_name_1  = "org_"  + rand1;
        String org_name_2  = "org_"  + rand2;

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

                fileContent = fileContent.replaceAll( "ORG_UUID_1",  org_uuid_1.toString() );
                fileContent = fileContent.replaceAll( "ORG_UUID_2",  org_uuid_2.toString() );

                fileContent = fileContent.replaceAll( "USER_NAME_1", user_name_1 );
                fileContent = fileContent.replaceAll( "USER_NAME_2", user_name_2 );

                fileContent = fileContent.replaceAll( "ORG_NAME_1", org_name_1 );
                fileContent = fileContent.replaceAll( "ORG_NAME_2", org_name_2 );

                FileOutputStream os = new FileOutputStream(
                        tempDir.getAbsolutePath() + File.separator + fileName );

                IOUtils.write( fileContent, os );
                os.close();
            }
        }

        // import data from temp directory

        ImportAdmins importAdmins = new ImportAdmins();
        importAdmins.startTool( new String[] {
            "-host", "localhost:" + ServiceITSuite.cassandraResource.getRpcPort(),
            "-inputDir", tempDir.getAbsolutePath()
        }, false );

        // verify that users and orgs were created correctly

        OrganizationInfo orgInfo1 = setup.getMgmtSvc().getOrganizationByUuid( org_uuid_1 );
        assertNotNull( orgInfo1 );

        OrganizationInfo orgInfo2 = setup.getMgmtSvc().getOrganizationByUuid( org_uuid_2 );
        assertNotNull( orgInfo2 );

        BiMap<UUID, String> user1_orgs = setup.getMgmtSvc().getOrganizationsForAdminUser( user_uuid_1 );
        assertEquals("user1 has two orgs", 2, user1_orgs.size() );

        BiMap<UUID, String> user2_orgs = setup.getMgmtSvc().getOrganizationsForAdminUser( user_uuid_2 );
        assertEquals("user2 has one orgs", 1, user2_orgs.size() );

        List<UserInfo> org1_users = setup.getMgmtSvc().getAdminUsersForOrganization( org_uuid_1 );
        assertEquals("org1 has one user", 1, org1_users.size() );

        List<UserInfo> org2_users = setup.getMgmtSvc().getAdminUsersForOrganization( org_uuid_2 );
        assertEquals("org2 has two users", 2, org2_users.size() );
    }

}