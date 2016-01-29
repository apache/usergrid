
# Usergrid 2.1.0 Deployment Guide

__NOTE__: DRAFT VERSION

This document covers these two topics:

   * Deploying the Usergrid Stack
   * Deploying the Usergrid Portal


## Deploying the Usergrid Stack

The Usergrid Stack is a Java 8 web application that runs on Tomcat, 
uses the Cassandra database for storage and the ElasticSearch search-engine for queries.
Below are the software requirements for the Stack. You can install them all on 
one computer for development purposes, and for deployment you can deploy them
separately using clustering.

   * [Java SE 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
   * [Apache Tomcat 7+](https://tomcat.apache.org/download-70.cgi)
   * [Apache Cassandra 1.2.1+](http://cassandra.apache.org/download/)
   * [ElasticSearch 1.4+](https://www.elastic.co/downloads/elasticsearch)  
 
Before installing the Usegrid Stack into Tomcat, start by setting up your databases.  
   
### Setup Cassandra 

Usergrid needs access to at least one Cassandra node. You can setup a single node of
Cassandra on your computer for development and testing. For production deployment, 
a three or more node cluster is recommended.

Cassandra requires Java and we recommend that you use the same version of Java 
for Cassandra as you use to run Tomcat and ElasticSearch.

Refer to the [Apache Cassandra documentation](http://XXXXXXXX) 
for instructions on how to install 
Cassandra. Once you are up and running make a note of these things:

   * The name of the Cassandra cluster
   * Hostname or IP address of each Cassandra node
   * Port number used for Cassandra RPC (the default is 9160)
   * Replication factor of Cassandra cluster

### Setup ElasticSearch

Usergrid also needs access to at least one ElasticSearch node. As with Cassandra, 
you can setup single ElasticSearch node on your computer, and you should run 
a cluster in production.

ElasticSearch requires Java and you MUST ensure that you use the same version of Java 
for ElasticSearch as you do for running Tomcat.

Refer to the [ElasticSearch documentation](http://XXXXXXXX) 
for instructions on how to install. 
Once you are up and running make a note of these things:

   * The name of the ElasticSearch cluster
   * Hostname or IP address of each ElasticSearch node
   * Port number used for ElasticSearch protocol (the default is 9200)

If you are running a single-node ElasticSearch cluster then you should
set the number of replicas to zero, otherwise it will appear to be unhealthy. 
  
    curl -XPUT 'localhost:9200/_settings' -d '{"index" : { "number_of_replicas" : 0}}'

### Setup Tomcat and deploy the Usergrid Stack

The Usergrid Stack is contained in a file named ROOT.war, a standard Java EE WAR
ready for deployment to Tomcat. On each machine that will run the Usergrid Stack 
you must install the Java SE 8 JDK and Tomcat 7+. Refer to the Apache Tomcat 
documentation for installation instructions.

Once Tomcat installed, you need to create and edit some configuration files.

### Configure Usergrid Stack

You must create a Usergrid properties file called __usergrid-deployment.properties__. 
The properties in this file tell Usergrid how to communicate with Cassandra and
ElasticSearch, and how to form URLs using the hostname you wish to use for Usegrid.
There are many properties that you can set to configure Usergrid. 

Once you have created your Usergrid properties file, place it in the Tomcat lib directory.
On a Linux system, that directory is probably located at __/usr/share/tomcat7/lib__.

__What goes in a properties file?__

The default properties file that is built into Usergrid contains the full list of properties, defaults and some documentation:
   
   * [The Default Usergrid Properties File](https://github.com/apache/usergrid/blob/master/stack/config/src/main/resources/usergrid-default.properties)

You should review the defaults in the above file. To get you started, let's look at a minimal example properties file that you can edit and use as your own.

#### Example Usergrid Stack Properties File

Below is an minimal example Usergrid properties file with the parts you need to change indicated like 
shell variables, e.g. ${USERGRID_CLUSTER_NAME}. Here's a guide to the things you need to set:

__Table 1: Values to set in Example Properties file:__

| Value       | Description |
|-------------|-------------|
| __BASEURL__ | This is the base URL for the Usergrid installation, e.g. __https://api.example.com__. |
| __USERGRID_CLUSTER_NAME__ | This is your name for your Usergrid installation. |
| __CASSANDRA_CLUSTER_NAME__ | Name of Cassandra cluster, must match what's in Cassandra configuration. |
| __CASSANDRA_HOSTS__ | Comma-separated lists of Cassandra hosts, with port numbers if you are not using the default 9120. The default for this property is __localhost:9120__ |
| __ELASTICSEARCH_CLUSTER_NAME__ | Name of ElasticSearch cluster, must match what's in ElasticSearch configuration. |
| __ELASTICSEARCH_HOSTS__ | Comma-separated lists of ElasticSearch hosts, with port numbers if you are not using the default 9120. The default for this property is __localhost:9300__ |
| __SUPER_USER_EMAIL__ | Email address of person responsible for the superuser account. |
| __SUPER_USER_PASSWORD__ | Password for the superuser account. |
| __TEST_ADMIN_USER_EMAIL__ | If __usergrid.setup-test-account=true__, as shown below, Usergrid will create a test account and you should specify a valid email here. |
| __TEST_ADMIN_USER_PASSWORD__ | Password for the username 'test' account. |
   
Make sure you set all of the above properties when you edit this example for your installation.   
   
__Example 1: usergrid-deployment.properties file__

    usergrid.cluster_name=${USERGRID_CLUSTER_NAME}

    cassandra.url=${CASSANDRA_HOSTS}
    cassanrda.cluster=${CASSANDRA_CLUSTER_NAME}

    elasticsearch.cluster_name=${ELASTICSEARCH_CLUSTER_NAME}
    elasticsearch.hosts=${ELASTIC_SEARCH_HOSTS}

    ######################################################
    # Admin and test user setup

    usergrid.sysadmin.login.allowed=true
    usergrid.sysadmin.login.name=superuser
    usergrid.sysadmin.login.password=${SUPER_USER_PASSWORD}
    usergrid.sysadmin.login.email=${SUPER_USER_EMAIL}

    usergrid.sysadmin.email=${SUPER_USER_EMAIL}
    usergrid.sysadmin.approve.users=true
    usergrid.sysadmin.approve.organizations=true

    # Base mailer account - default for all outgoing messages
    usergrid.management.mailer=Admin <${SUPER_USER_EMAIL}>

    usergrid.setup-test-account=true
    usergrid.test-account.app=test-app
    usergrid.test-account.organization=test-organization
    usergrid.test-account.admin-user.username=test
    usergrid.test-account.admin-user.name=Test User
    usergrid.test-account.admin-user.email=${TEST_ADMIN_USER_EMAIL}
    usergrid.test-account.admin-user.password=${TEST_ADMIN_USER_PASSWORD}

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
    usergrid.redirect_root=${BASEURL}/status

    usergrid.view.management.organizations.organization.activate=${BASEURL}/accounts/welcome
    usergrid.view.management.organizations.organization.confirm=${BASEURL}/accounts/welcome
    
    usergrid.view.management.users.user.activate=${BASEURL}/accounts/welcome
    usergrid.view.management.users.user.confirm=${BASEURL}/accounts/welcome

    usergrid.admin.confirmation.url=${BASEURL}/management/users/%s/confirm
    usergrid.user.confirmation.url=${BASEURL}/%s/%s/users/%s/confirm
    usergrid.organization.activation.url=${BASEURL}/management/organizations/%s/activate
    usergrid.admin.activation.url=${BASEURL}/management/users/%s/activate
    usergrid.user.activation.url=${BASEURL}%s/%s/users/%s/activate

    usergrid.admin.resetpw.url=${BASEURL}/management/users/%s/resetpw
    usergrid.user.resetpw.url=${BASEURL}/%s/%s/users/%s/resetpw
    

### Configure Logging

Usegrid includes the Apache Log4j logging system and you can control the levels of logs for each
Usergrid package and even down to the class level by providing your own __log4j.properties__ file.

To configure logging you need to:

1. Create a __log4j.properties__ file and place it on the computer where Tomcat is running
2. Add __-D__ system property to Tomcat so that Tomcat can find your Log4j properties file.

#### Example Logging Configuration

The Log4j properties file below is a good starting point for Usergrid. It configures ERROR level
logging for the 3rd party libraries that Usergrid depends on, and INFO level logging for Usergrid.
Plus, it configures some noisy parts of Usergrid to be quiet.

__Example 2: log4.properties file__

    # output messages into a rolling log file as well as stdout
    log4j.rootLogger=ERROR,stdout

    # stdout
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
    log4j.appender.stdout.layout.ConversionPattern=%d %p (%t) [%c] - %m%n

    log4j.logger.org.apache.usergrid=INFO
    
    log4j.logger.me.prettyprint.cassandra.hector.TimingLogger=WARN
    log4j.logger.org.apache.usergrid.rest.security.AllowAjaxFilter=WARN
    log4j.logger.me.prettyprint.hector.api.beans.AbstractComposite=ERROR
    
    
#### Add Logging Configuration to Tomcat

You can configure Tomcat to use your Log4j properties file but adding a __-D__ system property to Tomcat.
The property is __log4j.configuration__ and you must set it to be a "file:/" URL that points to your
properties file.

For example, if your property file is in /usr/share/tomcat7/lib/log4j.properties, then the variable would be set like this: __-Dlog4j.configuration=file:///usr/share/tomcat7/lib/log4j.properties__

There are a variety of ways for you to set that property in the Tomcat startup, one way is to create
a Tomcat setenv script in Tomcat's bin directory that sets the property in the JAVA_OPTS environment variable. For example on a Linux system you might do something like this to create the file:

__Example 3: Creating a Tomcat setenv.sh file on Linux__

    cat >> /usr/share/tomcat7/bin/setenv.sh << EOF
    export JAVA_OPTS="-Dlog4j.configuration=file:///usr/share/tomcat7/lib/log4j.properties"
    EOF
    chmod +x /usr/share/tomcat7/bin/setenv.sh

You might want set other __-D__ and __-X__ options in that setenv file, e.g. Java heap size.


### Deploy ROOT.war to Tomcat

The next step is to deploy the Usergrid Stack software to Tomcat. There are a variey of ways 
of doing this and the simplest is probably to place the Usergrid Stack ROOT.war file into
the Tomcat webapps directory, then restart Tomcat.


## Deploying the Usergrid Portal

The Usergrid Portal is an HTML5/JavaScript application, a bunch of static files that 
can be deployed to any web server, e.g. Apache HTTPD or Tomcat.

Configuration File
