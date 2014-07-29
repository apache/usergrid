/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */


// 
// configure_usergrid.groovy 
// 
// Emits usergrid properties file based on environment and Cassandra node registry in SimpleDB
//
import com.amazonaws.auth.*
import com.amazonaws.services.simpledb.*
import com.amazonaws.services.simpledb.model.*


String accessKey = (String)System.getenv().get("AWS_ACCESS_KEY")
String secretKey = (String)System.getenv().get("AWS_SECRET_KEY")

def baseUrl      = "http://${System.getenv().get("DNS_NAME")}.${System.getenv().get("DNS_DOMAIN")}"
String stackName = (String)System.getenv().get("STACK_NAME")
String domain    = stackName
String hostName  = (String)System.getenv().get("PUBLIC_HOSTNAME")
def replFactor   = System.getenv().get("CASSANDRA_REPLICATION_FACTOR")
def clusterName  = System.getenv().get("CASSANDRA_CLUSTER_NAME")

def superUserEmail     = System.getenv().get("SUPER_USER_EMAIL")
def testAdminUserEmail = System.getenv().get("TEST_ADMIN_USER_EMAIL")


NodeRegistry registry = new NodeRegistry();

def selectResult = registry.searchNode('cassandra')

// build seed list by listing all Cassandra nodes found in SimpleDB domain with our stackName
def cassandras = ""
def sep = ""
for (item in selectResult) {
    cassandras = "${cassandras}${sep}${item}:9160"
    sep = ","
}

// TODO T.N Make this the graphite url
selectResult = registry.searchNode('graphite')
def graphite = ""
sep = ""
for (item in selectResult) {
    graphite = "${graphite}${sep}${item}"
    sep = ","
}

// cassandra nodes are also our elasticsearch nodes
selectResult = registry.searchNode('cassandra')
def esnodes = ""
sep = ""
for (item in selectResult) {
    esnodes = "${esnodes}${sep}${item}"
    sep = ","
}

def usergridConfig = """
######################################################
# Minimal Usergrid configuration properties for local Tomcat and Cassandra 

cassandra.url=${cassandras}
cassandra.cluster=${clusterName}
cassandra.keyspace.strategy=org.apache.cassandra.locator.SimpleStrategy
cassandra.keyspace.replication=${replFactor}

elasticsearch.cluster_name=${clusterName}
elasticsearch.index_prefix=usergrid
elasticsearch.hosts=${esnodes}
elasticsearch.port=9300

######################################################
# Custom mail transport 

mail.transport.protocol=smtps
mail.smtps.host=smtp.gmail.com
mail.smtps.port=465
mail.smtps.auth=true
mail.smtps.quitwait=false

# TODO: make all usernames and passwords configurable via Cloud Formation parameters.

# CAUTION: THERE IS A PASSWORD HERE!
mail.smtps.username=usergridtest@gmail.com
mail.smtps.password=pw123

######################################################
# Admin and test user setup

usergrid.sysadmin.login.allowed=true
usergrid.sysadmin.login.name=superuser
usergrid.sysadmin.login.password=test
usergrid.sysadmin.login.email=${superUserEmail}

usergrid.sysadmin.email=${superUserEmail}
usergrid.sysadmin.approve.users=true
usergrid.sysadmin.approve.organizations=true

# Base mailer account - default for all outgoing messages
usergrid.management.mailer=Admin <${superUserEmail}>

usergrid.setup-test-account=true

usergrid.test-account.app=test-app
usergrid.test-account.organization=test-organization
usergrid.test-account.admin-user.username=test
usergrid.test-account.admin-user.name=Test User
usergrid.test-account.admin-user.email=${testAdminUserEmail}
usergrid.test-account.admin-user.password=test

######################################################
# Auto-confirm and sign-up notifications settings

usergrid.management.admin_users_require_confirmation=false
usergrid.management.admin_users_require_activation=false

usergrid.management.organizations_require_activation=false
usergrid.management.notify_sysadmin_of_new_organizations=true
usergrid.management.notify_sysadmin_of_new_admin_users=true

######################################################
# URLs

# Redirect path when request come in for TLD
usergrid.redirect_root=${baseUrl}/status

usergrid.view.management.organizations.organization.activate=${baseUrl}/accounts/welcome
usergrid.view.management.organizations.organization.confirm=${baseUrl}/accounts/welcome
\n\
usergrid.view.management.users.user.activate=${baseUrl}/accounts/welcome
usergrid.view.management.users.user.confirm=${baseUrl}/accounts/welcome

usergrid.admin.confirmation.url=${baseUrl}/management/users/%s/confirm
usergrid.user.confirmation.url=${baseUrl}/%s/%s/users/%s/confirm

usergrid.organization.activation.url=${baseUrl}/management/organizations/%s/activate

usergrid.admin.activation.url=${baseUrl}/management/users/%s/activate
usergrid.user.activation.url=${baseUrl}%s/%s/users/%s/activate

usergrid.admin.resetpw.url=${baseUrl}/management/users/%s/resetpw
usergrid.user.resetpw.url=${baseUrl}/%s/%s/users/%s/resetpw


usergrid.metrics.graphite.host=${graphite}
"""

println usergridConfig 
