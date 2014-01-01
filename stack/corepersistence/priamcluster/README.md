PriamCluster
===

For testing, creates a Cassandra cluster with Priam for seed discovery and token management.

Two parts:

1) A Maven assembly that builds an installer tarball and uploads it to S3. 
The tarball includes scripts to install Oracle JDK, Cassandra and Priam on EC2 instance.

2) A CloudFormation script that creates an autoscaling cluster, security group and some
number of EC2 instances, The template includes a CloudInit script that runs scipts from 
the installer tarball to configure and start Cassandra on each node. 


Getting set-up
---
To setup our AWS account to use PriamCluster here's what you need to do:

* __Create an AWS EC2 key pair__. You will need this PEM file to login to your Cassandra instances. 

* __Create an AWS S3 bucket__ for the PriamCluster install bundle. Create an S3 bucket in your 
account with the name `ug-cloudformation-priam`. 

* __Upload the Oracle Java JDK__ to that same S3 bucket. Best practice is to use the Oracle Java u
JDK with Cassandra, so you must upload the JDK to the S3 bucket that we created above. The JDK must 
be named `jdk-7u45-linux-x64.gz`.

* __Create AWS SimpleDB domain for Priam__ properties and instance registration. It's best if you 
use a SimpleDB client (e.g. SDBNavigator for Chrome) to create these domains. You must create two
domains named `PriamProperties` and `InstanceIdentity`. Just create the domains and Priam will create 
the attributes. 

* __Create an aws.properties file__ with your AWS credentials in the same directory as this 
README file. The file is git-ignored so you don't have to worry about accidentally committing it.

* __Deploy this the PriamCluster assembly__ by running the Maven command `mvn depoy` in the same
directory as this README file. 


Launching a new stack
---
Login to AWS Console and create a new CloudFormation stack. On the first screen, pick a short 
and simple name for your stack and choose the option to upload a template and upload 
the `cassandra-cf.json` file in this very directory. 

On the next screen, enter the number of servers you wish to create, the replication factor and 
the instance type you wish to use. Check the "I acknowlege that this will create IAM resources" 
and then click Next, Next, Next to take defaults and start the stack.

Watch the EC2 console and see your instances come up. They will be tagged with the stack name
that you provided.


Exploring a Cassandra node
---
Take a look at your new cluster! Login to one of the instances and take a look at the Cassandra
setup via `nodetool ring` and by looking at the following logs locations:

    /var/log/usergrid-bootstrap.log - log created as instance was created

    /var/log/tomcat7 - the Tomcat and Priam log files

    /var/log/cassandra - the Cassandra log files


How things work
---
Here's what happens when the stack is started.

CloudFormation reads the `cassanrda-cf.json` template and uses starts the EC2 instances that it 
specifies. When each instance comes up CloudFormation runs the CloudInit script specified in 
`cassandra-cf.json` and that script downloads the PriamCluster, sets up some environment scripts
and calls the `init_instance/init_cass.sh` script to complete the setup.

The `init_instance/init_cass.sh` calls `install_oraclejdk.sh` to download the JDK from S3 and 
install it. The script then installs Tomcat and Cassandra. Next it uses Groovy scripts to configure 
Priam and Cassandra and wait for other Cassandra nodes to come alive.

When Tomcat starts up, Priam comes to life. Priam reads it's SimpleDB tables, writes a new 
cassandra.yaml file and with the right seends and initial token starts Cassandra.


