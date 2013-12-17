PriamCluster
===

For testing, creates a Cassandra cluster with Priam for seed discovery and token management.

Two parts:

1) A Maven assembly that builds an installer tarball and uploads it to S3. 
The tarball includes scripts to install Oracle JDK, Cassandra and Priam on EC2 instance.

2) A CloudFormation script that creates an autoscaling cluster, security group and some
number of EC2 instances, The template includes a CloudInit script that runs scipts from 
the installer tarball to configure and start Cassandra on each node. 
