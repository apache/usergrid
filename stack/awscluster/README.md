AWS Cluster
===

*UNDER CONSTRUCTION* 

This project provides a AWS Cloud Formation template that launches and configures a complete Usergrid installation
on Amazon EC2 including Tomcat intances, Cassandra instances, Security Groups, a Load Balancer and DNS records.

Two parts:

1) A Maven assembly that builds an installer tarball and uploads it to S3. 
The tarball includes scripts to install Oracle JDK, Cassandra an EC2 instance.

2) A CloudFormation script `ugcluster-cf.json` that creates an auto-scaling cluster, security groups and some
number of EC2 instances, The template includes a CloudInit script that runs scipts from 
the installer tarball to configure and start either Cassandra or on each instance. 


Getting set-up
---
To setup our AWS account to use  AWS Cluster here's what you need to do:

* __Create an AWS EC2 key pair__. You will need this PEM file to login to your Cassandra instances. 

* __Create an AWS S3 bucket__ for the AWS Cluster install bundle. e.g. Create an S3 bucket in your 
account with the name `ug-cloudformation`. 

* __Setup a top-level domain name__ in the AWS Route 53 DNS service. The default value of usergrid.com will not work (unless you happen to own that domain name).

* __Upload the Oracle Java JDK__ to that same S3 bucket. Best practice is to use the Oracle Java u
JDK with Cassandra, so you must upload the JDK to the S3 bucket that we created above. The JDK must 
be named `jdk-7u45-linux-x64.gz`.

* __Create an aws.properties file__ with your AWS credentials in the same directory as this 
README file. The file is git-ignored so you don't have to worry about accidentally committing it.

* __Deploy this the  AWS Cluster assembly__ by running the Maven command `mvn deploy` in the same
directory as this README file. 


Building the Installation Tarball
---
First you need to build the Usergrid components: the Java SDK, the Stack and the Portal.

For example, here's how you would do that on a UNIX system (assuming that all Stack and Portal build pre-requistes are in place:

	$ cd usergrid/sdks/java
	$ mvn clean install
	
	$ cd ../../stack
	$ mvn -DskipTests=true clean install
	
	$ cd ../portal
	$ ./build.sh

Next, cd to the awscluster directory and run Maven deploy:

	$ cd usergrid/stack/awscluster
	$ mvn deploy

Maven will create the installation tarball and will copy it to your S3 bucket uf-cloudformation. You can find a copy of the tarball in the target directory: `awscluster-1.0-SNAPSHOT.tgz`.


Launching a new stack
---
Login to AWS Console and create a new CloudFormation stack. On the first screen, pick a short 
and simple name for your stack and choose the option to upload a template and upload 
the `ugcluster-cf.json` file in this very directory. 

On the next screen, enter the number of DB and REST servers you wish to create, the replication factor and 
the instance type you wish to use. Check the "I acknowledge that this will create IAM resources" 
and then click Next, Next, Next to take defaults and start the stack.

Watch the EC2 console and see your instances come up. They will be tagged with the stack name
that you provided.


Accessing your new stack
---
Assuming everything went well then you can access your stack at the DNS sub-domain and domain name that you specified in the configuration. For example, if you specified DNS domain `usergrid.com` and sub-domain `test1` then you should be able to access Usergrid at `http://test1.usergrid.com/`.


Initializing your stack
---

First, visit the Database setup URL: `http://<YOUR HOST NAME>/system/database/setup`

When prompted to login use the username `superuser` and password `test`. If the operation is successful you should a message like the one below in your browser. If not, check your logs for clues about what went wrong.

	{
  		"action" : "cassandra setup",
  		"status" : "ok",
  		"timestamp" : 1379424622947,
  		"duration" : 76
	}

Next, visit the Superuser setup URL: `http://<YOUR HOST NAME>/system/superuser/setup`

You should not be prompted for login because you already logged into for the Database Setup. If setup works, you should see a message like this:

	{
  		"action" : "superuser setup",
  		"status" : "ok",
  		"timestamp" : 1379424667936,
  		"duration" : 2
	}
	
Now you're ready to get started using Usergrid.


Login to the Usergrid Console & get started
---
You should now be able to login to the Usergrid console and start configuring applications, users and more.

The Usergrid Portal's URL is `http://<YOUR HOST NAME>/portal` and you can login with username `superuser` and password `test`.



Exploring a Cassandra node
---
Take a look at your new cluster. Login to one of the instances and take a look at the Cassandra
setup via `nodetool ring` and by looking at the following log file locations.

Log File Locations

`/var/log/usergrid-bootstrap.log` - log created as instance was created

`/var/log/cassandra/*` - the Cassandra log files



Exploring a Tomcat node
---
Take a look at your new cluster. Login to one of the instances and take a look at the Cassandra
setup via `nodetool ring` and by looking at the following logs locations:

Log File Locations

`/var/log/usergrid-bootstrap.log` - log created as instance was created

`/var/log/tomcat7/*` - the Tomcat log files



How it works
---
Here's what happens when the stack is started.

CloudFormation reads the `ugcluster-cf.json` template and uses starts the EC2 instances that it 
specifies. There are two types of instances, Cassandra instances and REST instances. 

When each Cassandra instance comes up CloudFormation runs the CloudInit script specified in 
`ugcluster-cf.json` and that script downloads the , sets up some environment scripts
and calls the `init_instance/init_db_server.sh` script to complete the setup.

The `init_instance/init_db_server.sh` calls `install_oraclejdk.sh` to download the JDK from S3 and 
install it. The script then installs Tomcat and Cassandra. Next it uses Groovy scripts to configure 
Cassandra and wait for other Cassandra nodes to come alive.

When a REST instance comes up, it does the same things but it calls the `init_rest_server.sh` to install and configure Tomcat, wait for Cassandra nodes to become available and then setup the Usergrid Stack and Usergrid Portal webapps.



