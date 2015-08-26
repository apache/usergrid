# Usergrid 2: Deploy to Tomcat

__NOTE__: Beware that Usergrid 2 is UNRELEASED SOFTWARE

## Requirements

* [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven 3.2+](http://maven.apache.org/)
* [Tomcat 7+](https://tomcat.apache.org/download-70.cgi)
* [Cassandra 1.2.1*](http://cassandra.apache.org/download/)
* [ElasticSearch 1.4+](https://www.elastic.co/downloads/elasticsearch) 
* [Usergrid 2.0](https://github.com/apache/usergrid/tree/two-dot-o)

##Running


1. Start up Cassandra [^1]
	a. To do this you can navigate to the cassandra folder and run ```./bin/cassandra ```
2. Start up Elasticsearch
	a. To do this you can navigate to the folder where you extracted elasticsearch and run ```/bin/elasticsearch``` 	

###Running Usergrid	

####Build The Java Sdk

1. Navigate to where you cloned the usergrid repo
2. Navigate to the ```sdks/java``` directory
3. Run ```mvn clean install```

####Build The Stack Itself

1. Navigate to the ```stack``` directory.
2. Run ```mvn clean install -DskipTests```
3. This will generate a war at ```rest/target/ROOT.war```

####Deploying the Stack Locally
1. Take this war and deploy it on downloaded tomcat.
1. In the lib directory of the tomcat you must also put usergrid-deployment.properties. ( An example file is provided below)
1. Start up Tomcat
	a. To do this you can navigate to folder where Tomcat is install and run ```./bin/catalina.sh start```
1. Go to a web browser and input the following to initilizing the database ```localhost:8080/system/database/setup```. 
	a. The credentials it asks for are the admin credentialls and password as defined in the usergrid-deployment.properties. 
	b. You can also do a curl call with basic auth to automatically authenticate the call instead of using the web browser.
1. Then using the same steps as before call ```localhost:8080/system/superuser/setup```

The stack is now ready to be queried against, but to get the most out of it you'll need to initilize and use our portal!

####Running The Portal Locally
#####Requirments 
[nodejs 0.10+](https://nodejs.org/download/) 

1. Make sure you've installed node.js above. Any version above .10 or .10 should work fine.
2. Navigate to ```usergrid/portal```.
3. Open config.js and make sure the override URL is pointing to your local tomcat.
4. Now in the portal folder run the following command ```./build.sh dev``` 
5. The portal should automatically open ready for use!

Now usergrid is fully ready to use! Feel free to query against it or use it however you like!



Example __usergrid-deployment.properties__ file
---
```
# core persistence properties

cassandra.embedded=false
cassandra.version=1.2.18
cassandra.timeout=2000

collections.keyspace=Usergrid_Applications
collections.keyspace.strategy.options=replication_factor:1
collections.keyspace.strategy.class=org.apache.cassandra.locator.SimpleStrategy

collection.stage.transient.timeout=60

hystrix.threadpool.graph_user.coreSize=40
hystrix.threadpool.graph_async.coreSize=40

elasticsearch.embedded=false
elasticsearch.cluster_name=elasticsearch
elasticsearch.index_prefix=usergrid
elasticsearch.hosts=127.0.0.1
elasticsearch.port=9300

elasticsearch.force_refresh=true

index.query.limit.default=100

# Max Cassandra connections, applies to both CP and EM
cassandra.connections=600

######################################################
# Minimal Usergrid configuration properties for local Tomcat and Cassandra 
#

cassandra.url=127.0.0.1:9160

cassandra.keyspace.strategy=org.apache.cassandra.locator.SimpleStrategy
cassandra.keyspace.strategy.options.replication_factor=1

######################################################
# Custom mail transport 

mail.transport.protocol=smtps
mail.smtps.host=smtp.gmail.com
mail.smtps.port=465
mail.smtps.auth=true
mail.smtps.username=
mail.smtps.password=
mail.smtps.quitwait=false


######################################################
# Admin and test user setup

usergrid.sysadmin.login.name=superuser
usergrid.sysadmin.login.email=myself@example.com     <--- Change this
usergrid.sysadmin.login.password=pwHERE               <--- Change this
usergrid.sysadmin.login.allowed=true
usergrid.sysadmin.email=myself@example.com           <--- Change this

usergrid.sysadmin.approve.users=false
usergrid.sysadmin.approve.organizations=false

# Base mailer account - default for all outgoing messages
usergrid.management.mailer=User <myself@example.com>    <--- Change this

usergrid.setup-test-account=true

usergrid.test-account.app=test-app
usergrid.test-account.organization=test-organization
usergrid.test-account.admin-user.username=test
usergrid.test-account.admin-user.name=Test User
usergrid.test-account.admin-user.email=myself@example.com    <---Change this
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
usergrid.redirect_root=http://localhost:8080/status

usergrid.view.management.organizations.organization.activate=http://localhost:8080/accounts/welcome
usergrid.view.management.organizations.organization.confirm=http://localhost:8080/accounts/welcome
usergrid.view.management.users.user.activate=http://localhost:8080/accounts/welcome
usergrid.view.management.users.user.confirm=http://localhost:8080/accounts/welcome

usergrid.organization.activation.url=http://localhost:8080/management/organizations/%s/activate
usergrid.admin.activation.url=http://localhost:8080/management/users/%s/activate
usergrid.admin.resetpw.url=http://localhost:8080/management/users/%s/resetpw
usergrid.admin.confirmation.url=http://localhost:8080/management/users/%s/confirm
usergrid.user.activation.url=http://localhost:8080%s/%s/users/%s/activate
usergrid.user.confirmation.url=http://localhost:8080/%s/%s/users/%s/confirm
usergrid.user.resetpw.url=http://localhost:8080/%s/%s/users/%s/resetpw
``` 

[^1]: You can start up cassandra and elasticsearch in any order but for the sake of ordered lists I put Cassandra first. 