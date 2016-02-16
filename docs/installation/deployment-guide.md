# Usergrid 2.1.0 (unreleased) Deployment Guide

This document explains how to deploy the Usergrid v2.1.0 Backend-as-a-Service (BaaS), 
which comprises the Usergrid Stack, a Java web application, and the Usergrid Portal,
which is an HTML5/JavaScript application. 


## Intended audience

You should be able to follow this guide if you are a developer, system admin or 
operations person with some knowledge of Java application deployment and good 
knowledge of Linux and the bash shell.

This guide is a starting point and does NOT explain everything you need to know to 
run Usergrid at-scale and in production. To do that you will need some additional 
skills and knowledge around running, monitoring and trouble-shooting Tomcat 
applications, multi-node Cassandra & ElasticSearch clusters and more.


## Prerequsites

Below are the software requirements for Usergrid 2.1.0 Stack and Portal. 
You can install them all on one computer for development purposes, and for 
deployment you can deploy them separately using clustering.

   * Linux or a UNIX-like system (Usergrid may run on Windows, but we haven't tried it)
   
   * [Java SE 8 JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
   
   * [Apache Tomcat 7+](https://tomcat.apache.org/download-70.cgi)
   
   * [Apache Cassandra 1.2.1+](http://cassandra.apache.org/download/)
   
   * [ElasticSearch 1.4+](https://www.elastic.co/downloads/elasticsearch)  
   
Optional but helpful:

   * An HTTP or REST client, such as [curl](http://curl.haxx.se)
   * A web server such as [Apache HTTPD](https://httpd.apache.org) for running the Usergrid Portal
      
   
## Getting Started

__Download the Apache Usergrid 2.1.0 binary release__ from the official Usergrid releases page:

* [Apache Usergrid Releases](https://usergrid.apache.org/releases)

When you un-tar the Usergrid binary release, you will see a directory layout like this:

    +-- apache-usergrid-2.1.0
        |
        +-- LICENSE
        |
        +-- NOTICE
        |
        +-- CHANGELOG
        |
        +-- stack
        |   | 
        |   + ROOT.war
        |
        +-- portal
        |   |
        |   +-- dist
        |       |
        |       + usergrid-portal.tar    
        |
        +-- sdks
        |   |
        |   +-- html5-javascript (JavaScript SDK and source)
        |   | 
        |   +-- java (Java SDK and source)
        
The files that you need for deploying Usergrid Stack and Portal are `ROOT.war` and `usergrid-portal.tar`.
                    

## Deploying the Usergrid Stack

The Usergrid Stack is a Java EE web application that runs on Tomcat, 
uses the Cassandra database for storage and the ElasticSearch search-engine for queries.
 
Before installing the Usegrid Stack into Tomcat, you'll start by setting up the 
required database and search engine nodes. 
 
   
### Stack STEP #1: Setup Cassandra 

Usergrid needs access to at least one Apache Cassandra node. You can setup a single node of
Cassandra on your computer for development and testing. For production deployment, 
a three or more node cluster is recommended.

__Use the right Java.__ Cassandra requires Java and we recommend that you use the same version of Java 
for Cassandra as you use to run Tomcat and ElasticSearch.

__Refer to the__ [Apache Cassandra documentation](http://wiki.apache.org/cassandra/GettingStarted) 
__for instructions on how to install Cassandra__. The [Datastax documentation for Cassandra 1.2](http://docs.datastax.com/en/cassandra/1.2/cassandra/features/featuresTOC.html) is also helpful. 
Once you are up and running make a note of these things:

   * The name of the Cassandra cluster
   * Hostname or IP address of each Cassandra node
   * Port number used for Cassandra RPC (the default is 9160)
   * Replication factor of Cassandra cluster
   

### Stack STEP #2: Setup ElasticSearch

Usergrid also needs access to at least one ElasticSearch node. As with Cassandra, 
you can setup single ElasticSearch node on your computer, and you should run 
a cluster in production.

__Use the right Java__. ElasticSearch requires Java and you *must* ensure that you use the 
same version of Java for ElasticSearch as you do for running Tomcat.

__Refer to the__ 
[ElasticSearch 1.4 documentation](https://www.elastic.co/guide/en/elasticsearch/reference/1.4/index.html) 
__for instructions on how to install__. Once you are up and running make a note of these things:

   * The name of the ElasticSearch cluster
   * Hostname or IP address of each ElasticSearch node
   * Port number used for ElasticSearch protocol (the default is 9200)

__Running a single-node?__ If you are running a single-node ElasticSearch cluster then 
you should set the number of replicas to zero, otherwise the cluster will report status YELLOW. 
  
    curl -XPUT 'localhost:9200/_settings' -d '{"index" : { "number_of_replicas" : 0}}'
    

### Stack STEP #3: Setup Tomcat

The Usergrid Stack is contained in a file named ROOT.war, a standard Java EE WAR
ready for deployment to Tomcat. On each machine that will run the Usergrid Stack 
you must install the Java SE 8 JDK and Tomcat 7+. 

__Refer to the__ [Apache Tomcat 7](https://tomcat.apache.org/tomcat-7.0-doc/setup.html) __documentation for  instructions on how to install__. Once Tomcat installed, you need to create and edit some configuration files.


### Stack STEP #4: Configure Usergrid Stack & Logging

You must create a Usergrid properties file called `usergrid-deployment.properties`. 
The properties in this file tell Usergrid how to communicate with Cassandra and
ElasticSearch, and how to form URLs using the hostname you wish to use for Usegrid.
There are many properties that you can set to configure Usergrid. 

Once you have created your Usergrid properties file, place it in the Tomcat lib directory.
On a Linux system, that directory is probably located at `/usr/share/tomcat7/lib`.

__What goes in a properties file?__

The default properties file that is built into Usergrid contains the full list of properties, defaults and some documentation:
   
   * [The Default Usergrid Properties File](https://github.com/apache/usergrid/blob/master/stack/config/src/main/resources/usergrid-default.properties)

You should review the defaults in the above file. To get you started, let's look at a minimal example properties file that you can edit and use as your own.


#### Example Usergrid Stack Properties File

Below is an minimal example Usergrid properties file with the parts you need to change indicated like 
shell variables, e.g. `${USERGRID_CLUSTER_NAME}`.  
   
Example 1: usergrid-deployment.properties file

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
    

Here's a guide to the things you need to set in the above properties file.

__Table 1: Values to set in Example Properties file:__

<table class="usergrid-table">
<tr>
  <th>Value</th>
  <th>Description</th>
</tr>
<tr>
    <td>__BASEURL__</td>
    <td>This is the base URL for the Usergrid installation, e.g. `https://api.example.com`. </td>
</tr>
<tr>
    <td>__USERGRID_CLUSTER_NAME__</td>
    <td>This is your name for your Usergrid installation. </td>
</tr>
<tr>
    <td>__CASSANDRA_CLUSTER_NAME__</td>
    <td>Name of Cassandra cluster, must match what's in Cassandra configuration. </td>
</tr>
<tr>
    <td>__CASSANDRA_HOSTS__</td>
    <td>Comma-separated lists of Cassandra hosts, with port numbers if you are not using the default 9160. The default for this property is `localhost:9160` </td>
</tr>
<tr>
    <td>__ELASTICSEARCH_CLUSTER_NAME__</td>
    <td>Name of ElasticSearch cluster, must match what's in ElasticSearch configuration. </td>
</tr>
<tr>
    <td>__ELASTICSEARCH_HOSTS__</td>
    <td>Comma-separated lists of ElasticSearch hosts, with port numbers if you are not using the default 9300. The default for this property is `localhost:9300` </td>
</tr>
<tr>
    <td>__SUPER_USER_EMAIL__</td>
    <td>Email address of person responsible for the superuser account. </td>
</tr>
<tr>
    <td>__SUPER_USER_PASSWORD__</td>
    <td>Password for the superuser account. </td>
</tr>
<tr>
    <td>__TEST_ADMIN_USER_EMAIL__</td>
    <td>If `usergrid.setup-test-account=true`, as shown below, Usergrid will create a test account and you should specify a valid email here. </td>
</tr>
<tr>
    <td>__TEST_ADMIN_USER_PASSWORD__</td>
    <td>Password for the username 'test' account. </td>
</tr>
</table>   
   
Make sure you set all of the above properties when you edit this example for your installation.  


#### Configure Logging

Usegrid includes the Apache Log4j logging system and you can control the levels of logs for each
Usergrid package and even down to the class level by providing your own `log4j.properties` file.

To configure logging you need to:

1. Create a `log4j.properties` file and place it on the computer where Tomcat is running
2. Add `-D` system property to Tomcat so that Tomcat can find your Log4j properties file.


##### Example Logging Configuration

The Log4j properties file below is a good starting point for Usergrid. It configures `ERROR` level
logging for the 3rd party libraries that Usergrid depends on, and INFO level logging for Usergrid.
Plus, it configures some noisy parts of Usergrid to be quiet.

Example 2: log4.properties file

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
    
    
##### Add Logging Configuration to Tomcat

You can configure Tomcat to use your Log4j properties file but adding a system property to Tomcat
named `log4j.configuration` which must be set to a `file:/` URL that points to your
properties file. One way to add the above property to the Tomcat start-up is to add a line to a 
Tomcat `setenv.sh` script in Tomcat's bin directory. If that file does not exist, then create it.

For example, if your property file is in `/usr/share/tomcat7/lib/log4j.properties`, then you 
would add the following line to `setenv.sh`:

    export JAVA_OPTS="-Dlog4j.configuration=file:///usr/share/tomcat7/lib/log4j.properties"
    
If the file already exists and already sets the JAVA_OPTS variable, then you'll have to 
add your `-D` option to ones already there. Also note, you might want set other `-D` and `-X` 
options in that setenv file, e.g. Java heap size.


### Stack STEP #5: Deploy ROOT.war to Tomcat

The next step is to deploy the Usergrid Stack software to Tomcat. There are a variey of ways 
of doing this and the simplest is probably to place the Usergrid Stack `ROOT.war` file into
the Tomcat `webapps` directory, then restart Tomcat.


__For example, on Linux...__

You would probabaly copy the ROOT.war file like so:

    cp ROOT.war /usr/share/tomcat7/webapps
    
And you would restart Tomcat 7 like so:

    /etc/init.d/tomcat7 restart
    
You can watch the Tomcat log in `/var/log/tomcat7/catalina.out` for errors:

    tail -f /var/log/tomcat7/catalina.out
    
 Look for messages like this, which indicate that the ROOT.war file was deployed:
 
    INFO: Starting service Catalina
    Jan 29, 2016 1:00:32 PM org.apache.catalina.core.StandardEngine startInternal
    INFO: Starting Servlet Engine: Apache Tomcat/7.0.59
    Jan 29, 2016 1:00:32 PM org.apache.catalina.startup.HostConfig deployWAR
    INFO: Deploying web application archive /usr/share/tomcat7/webapps/ROOT.war
    
    
__Does it work?__

Check to see if Usergrid is up and running by calling the status end-point. 
If your web browser is running on the same computer as Tomcat (and Tomcat is on port 8080), 
then you can browse to [http://localhost:8080/status](http://localhost:8080/status) 
to view the Usergrid status page. 

Or you can use curl:

    curl http://localhost:8080/status
    
If you get a JSON file of status data, then you're ready to move to the next step.
You should see a response that begins like this:

    {
      "timestamp" : 1454090178953,
        "duration" : 10,
        "status" : {
          "started" : 1453957327516,
          "uptime" : 132851437,
          "version" : "201601240200-595955dff9ee4a706de9d97b86c5f0636fe24b43",
          "cassandraAvailable" : true,
          "cassandraStatus" : "GREEN",
          "managementAppIndexStatus" : "GREEN",
          "queueDepth" : 0,
          "org.apache.usergrid.count.AbstractBatcher" : {
            "add_invocation" : {
              "type" : "timer",
              "unit" : "microseconds",
              
     ... etc. ...
 

#### Initialize the Usergrid Database

Next, you must initialize the Usergrid database, index and query systems.

To do this you must issue a series of HTTP operations using the superuser credentials.
You can only do this if Usergrid is configured to allow superused login via
this property `usergrid.sysadmin.login.allowed=true` and if you used the 
above example properties file, it is allowed.

The three operation you must perform are expressed by the curl commands below and,
of course, you will have ot change the password 'test' to match the superuser password 
that you set in your Usergrid properties file.

    curl -X PUT http://localhost:8080/system/database/setup     -u superuser:test
    curl -X PUT http://localhost:8080/system/database/bootstrap -u superuser:test
    curl -X GET http://localhost:8080/system/superuser/setup    -u superuser:test
    
When you issue each of those curl commands, you should see a success message like this:

    {
        "action" : "cassandra setup",
        "status" : "ok",
        "timestamp" : 1454100922067,
        "duration" : 374
    }    

If you don't see a success message, then refer to the Tomcat logs for error message and
seek help from the [Usergrid community](http://usergrid.apache.org/community).

Now that you've gotten Usergrid up and running, you're ready to deploy the Usergrid Portal.


## Deploying the Usergrid Portal

The Usergrid Portal is an HTML5/JavaScript application, a bunch of static files that 
can be deployed to any web server, e.g. Apache HTTPD or Tomcat.

To deploy the Portal to a web server, you will un-tar the `usergrid-portal.tar` file into 
directory that serves as the root directory of your web pages. 

For example, with Tomcat on Linux you might do something like this:

    cp usergrid-portal.tar /usr/share/tomcat7/webapps
    cd /usr/share/tomcat7/webapps
    tar xf usergrid-portal.tar
    
Then you will probably want to rename the Portal directory to something that will work
well in a URL. For example, if you want your Portal to exist at the path `/portal` then:

    mv usergrid-portal.2.0.18 portal
    
Once you have done that there is one more step. You need to configure the portal so that 
it can find the Usergrid stack. You do that by editing the `portal/config.js` and changing
this line:

    Usergrid.overrideUrl = 'http://localhost:8080/';

To set the hostname that you will be using for your Usergrid installation. 

Start your web server and Portal should be up and running at http://localhost:8080/portal or wherever you deployed it.



## Additional Resources

Resources that might be useful to those deploying Usergrid:

[Usergrid-Vagrant](https://github.com/snoopdave/usergrid-vagrant): A VagrantFile and set of bash scripts that will launch a Linux Virtual Machine running Cassandra, ElasticSearch, Tomcat and the Usergrid 2.1 Stack and Portal. 

[Usergrid AWS Cluster](https://github.com/apache/usergrid/tree/master/deployment/aws): An AWS Cloud Formation template and supporting scripts that create a set of multiple EC2 instances running Usergrid Stack/Portal and a set of EC2 instances running Cassandra and ElasticSearch.


## The End

That's all folks.
