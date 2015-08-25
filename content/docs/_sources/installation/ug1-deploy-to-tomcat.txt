# Usegrid 1: Deploying to Tomcat

This is a guide that explains how to install and run Usergrid using stock Tomcat and Cassandra on a single computer.

NOTE: running Cassandra on a single computer is something you should do ONLY for testing purposes. You don't want to run one node in production even just to start out. To get the benefit of Cassandra's architecture, which is designed to support linear scalability. You should be running a Cassandra cluster with at least three nodes. 

For more information:

* [Cassandra FAQ: Can I Start With a Single Node?](http://planetcassandra.org/blog/post/cassandra-faq-can-i-start-with-a-single-node/)
* [Why don't you start off with a “single & small” Cassandra server](http://stackoverflow.com/questions/18462530/why-dont-you-start-off-with-a-single-small-cassandra-server-as-you-usually)

## Requirements

* [JDK 1.7](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
* [Maven](http://maven.apache.org/)

## Download

Use GitHub to clone the [apache/usergrid](https://github.com/apache/usergrid) repo.

Or you can start by [downloading our latest code](https://github.com/apache/usergrid/archive/master.zip) and extract it.

## Building

From the command line, navigate to `stack` directory and type the following:

    mvn clean package -DskipTests=true

Once you are done the Usergrid application will be package as a Java EE WAR file at the location __stack/rest/target/ROOT.war__.

Install and configure Cassandra
---

Install Cassandra, don't edit configuration files as we just want default values for this simple setup. Here are the [instructions for installing Cassandra](http://wiki.apache.org/cassandra/GettingStarted)

Install and configure Tomcat
---

Follow instructions, don't edit configuration files as we just want default values for this simple setup. Here are the [instructions for installing Tomcat 7](http://tomcat.apache.org/tomcat-7.0-doc/setup.html)

Add Usergrid WAR to Tomcat
---

Remove the existing `tomcat/webapps/ROOT` directory. 

Place the Usergrid `ROOT.war` file into the `tomcat/webapps` directory

Add Usergrid configuration file to Tomcat
---

Create a ____usergrid-custom.properties____ file and place it in Tomcat's __lib__ directory. You can find an example properties file below  that should work well for a local Tomcat & Cassandra setup. You will probably only need to change the properties below to use your email address and preferred password for the install.


    usergrid.sysadmin.login.allowed=true
    usergrid.sysadmin.login.name=superuser
    usergrid.sysadmin.login.password=pw123
    usergrid.sysadmin.email=me@example.com
    usergrid.sysadmin.login.email=myself@example.com
    usergrid.management.mailer=Myself<myself@example.com>
    usergrid.test-account.admin-user.email=myself@example.com
    usergrid.test-account.admin-user.password=test

Run Usergrid Database & Super User Setup
---

Start Tomcat and use your web browser to visit the URLs below. While you do this you might want to watch the logs under tomcat/logs for clues, just in case anything goes wrong. 

Database setup URL - [http://localhost:8080/system/database/setup](http://localhost:8080/system/database/setup)

When prompted to login use the sysadmin credentials that you specified in your __usergrid-custom.properties__ file. Based on the example above that would be superuser and pw123. If the operation is successful you should a message like the one below in your browser. If not, check your logs for clues about what went wrong.

    {
      "action" : "cassandra setup",
      "status" : "ok",
      "timestamp" : 1379424622947,
      "duration" : 76
    }

Superuser setup URL - [http://localhost:8080/system/superuser/setup](http://localhost:8080/system/superuser/setup)

You should not be prompted for login because you already logged into for the Database Setup. If setup works, you should see a message like this:

    {
      "action" : "superuser setup",
      "status" : "ok",
      "timestamp" : 1379424667936,
      "duration" : 2
    }

Build the Usergrid Console
---
The Usergrid Console is an admin interface written in JavaScript that connects to your running Usergrid instance. For evaluation purposes, you can run it within Tomcat. Build it by following the steps [here](https://github.com/apache/usergrid/blob/master/portal/README.md). Once built, copy the directory _portal/build/usergrid-portal_ to _tomcat/webapps_.


Login to the Usergrid Console & get started
---
You should now be able to login to the Usergrid console and start configuring applications, users and more. 

You can use an static version of the portal to get started:

http://localhost:8080/usergrid-portal/(http://localhost:8080/usergrid-portal)


Example __usergrid-custom.properties__ file
---
Here's a complete example properties file to get you started.

    # Minimal Usergrid configuration properties for local Tomcat and Cassandra 
    #
    # The cassandra configuration options. 

    # The cassandra host to use
    cassandra.url=localhost:9160

    # if your cassandra instance requires username/password
    cassandra.username=someuser
    cassandra.password=somepassword
    
    # The strategy to use when creating the keyspace. This is the default. 
    # We recommend creating the keyspace with this default, then editing it 
    # via the cassandra CLI to meet the client's needs.
    cassandra.keyspace.strategy=org.apache.cassandra.locator.SimpleStrategy
     
    # The default replication factor for the simple strategy. Again, leave the 
    # default, create the app, then use the cassandra cli to set the replication 
    # factor options. This can become complicated with different topologies and 
    # is more a Cassandra administration issue than a UG issue.
    cassandra.keyspace.strategy.options.replication_factor=1
     
    ######################################################
    # Custom mail transport. Not usually used for local testing

    #mail.transport.protocol=smtps
    #mail.smtps.host=email-smtp.us-east-1.amazonaws.com
    #mail.smtps.port=465
    #mail.smtps.auth=true
    #mail.smtps.quitwait=false
    #mail.smtps.username=
    #mail.smtps.password=

    ######################################################
    # Admin and test user setup (change these to be their super user

    usergrid.sysadmin.login.name=superuser
    usergrid.sysadmin.login.email=myself@example.com     <--- Change this
    usergrid.sysadmin.login.password=pw123               <--- Change this
    usergrid.sysadmin.login.allowed=true
    usergrid.sysadmin.email=myself@example.com           <--- Change this
    
    # Enable or disable this to require superadmin approval of users
    usergrid.sysadmin.approve.users=false

    ######################################################
    # Auto-confirm and sign-up notifications settings

    usergrid.management.admin_users_require_confirmation=false
    usergrid.management.admin_users_require_activation=false
    usergrid.management.organizations_require_activation=false
    usergrid.management.notify_sysadmin_of_new_organizations=false
    usergrid.management.notify_sysadmin_of_new_admin_users=false

    ######################################################
    # URLs
    # Redirect path when request come in for TLD

    usergrid.redirect_root=https://localhost:8080/status
    usergrid.view.management.organizations.organization.activate=https://localhost:8080/accounts/welcome
    usergrid.view.management.organizations.organization.confirm=https://localhost:8080/accounts/welcome
    usergrid.view.management.users.user.activate=https://localhost:8080/accounts/welcome
    usergrid.view.management.users.user.confirm=https://localhost:8080/accounts/welcome
    usergrid.organization.activation.url=https://localhost:8080/management/organizations/%s/activate
    usergrid.admin.activation.url=https://localhost:8080/management/users/%s/activate
    usergrid.admin.resetpw.url=https://localhost:8080/management/users/%s/resetpw
    usergrid.admin.confirmation.url=https://localhost:8080/management/users/%s/confirm
    usergrid.user.activation.url=https://localhost:8080%s/%s/users/%s/activate
    usergrid.user.confirmation.url=https://localhost:8080/%s/%s/users/%s/confirm
    usergrid.user.resetpw.url=https://localhost:8080/%s/%s/users/%s/resetpw
 
 