Usergrid Tools
===
The Usergrid Tools are a set of Java programs that can be run from a command-line console.

How to build Usergrid Tools
---
First, build Usergrid as you would normally.

    $ cd usergrid-stack
    $ mvn install
    
Once you have done that you will find the Usergrid Tools bundle in the tools/target directory named with the pattern __usergrid-tools-X.Y.Z-release.tar.gz__ where X.Y.Z is the Usergrid version number.

How to install Usergrid Tools
---
On a UNIX computer with Java installed simply untar and unzip the usergrid-tools archive.

    $ cd /usr/share
    $ tar xzvf usergrid-toolsX.Y.Z-release.tar.gz
    
And then you will find the following directory structure (Where X.Y.Z is the version number).

    /usr/share/usergrid-tools-X.Y.Z/
      README.md
      usergrid-tools.jar
      usergrid-export.sh
      usergrid-deployment.properties

These are the important files:

* __usergrid-tools.jar__: this is the Usergrid Tools executable
* __usergrid-export.sh__: this is a shell script design to be run as a cron scheduled task
* __usergrid-deployment.properties__: this is the configuration file, refer to the documentation for the tool you are running to learn what properties are required.


How to run the Usergrid Tools
---
To run the Usergrid Tools you need a UNIX machine with Java installed. To run one of the tools
you do something like this:

    $ cd usergrid-tools
    $ java -jar usergrid-tools.jar <toolname>

Where "toolname" is the name of the tool that you wish to run (and the name of a Java class in the org.apache.usergrid.tools package). Each tool is different and may or may not have documentation available.

What Tools are available?
---
This README.md only documents two of the tools, WarehouseExport and WarehouseUpsert. You will have to seek documentation else where or look at the source code (in the org.apache.usergrid.tools package) to understand what other tools are avialable.

Enabling additional logging for debugging purposes
---

If you want to control what is logged by the Usergrid Tools, you must provide your own log4j.configuration file.  When you start the tool with the java command, add the following option right after the word "java" and (of course) replace /path/to with the real path to your log4j file.

    -Dlog4j.configuration=file:/path/to/log4j.properties

Here is a simple log4j.properties file that will turn on DEBUG logging for the Usergrid Tools:

    log4j.rootLogger=WARN,stdout
    log4j.appender.stdout=org.apache.log4j.ConsoleAppender
    log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
    log4j.appender.stdout.layout.ConversionPattern=%d %p (%t) [%c] - %m%n

    log4j.category.org.apache.usergrid.tools=DEBUG
    log4j.category.org.apache.usergrid=INFO
    log4j.logger.org.apache.usergrid.persistence.cassandra=WARN


Redshift Warehouse Export and Upsert
===
Usergrid data storage is designed for scalability, fault tolerance and performance but it's not structured well for data warehouse style metrics and analytics. So Usergrid Tools provides two tools that can be used to export Usergrid data in CSV format, upload that data to Amazon S3 and then upsert that data data into an Amazon Redshift database instance for analysis using SQL and huge number of tools that support standard SQL.

Data Warehouse Prerequisites
---
* UNIX computer with Java installed
* Access to a Usergrid Cassandra cluster
* Access to Amazon Redshift data warehouse

Setup
---
This is something of a do-it-yourself process. Here are the steps to setup warehouse export and upsert:

__Step 1__: Install Usergrid Tools on a UNIX computer with Java and can access your Cassandra cluster

__Step 2__: Edit the usergrid-cusom-tools.properties file and set the properties shown below.

    cassandra.url=MYHOST1:9160,MYHOST2:9160

    usergrid.warehouse-export-access-id=AAAAAAAAA
    usergrid.warehouse-export-secret-key=SSSSSSSSS

    usergrid.warehouse-export-bucket=MYBUCKET

    usergrid.warehouse-export-dbhost=MYHOST.MYREGION.redshift.amazonaws.com
    usergrid.warehouse-export-dbport=5439
    usergrid.warehouse-export-dbname=DATABASE_NAME
    usergrid.warehouse-export-dbuser=DATABASE_USERNAME
    usergrid.warehouse-export-dbpassword=DATABASE_PASSWORD

    usergrid.warehouse-export-staging-table=MYSTAGINGTABLE
    usergrid.warehouse-export-main-table=MYMAINTABLE

__Step 3__: Make sure your settings are correct. Test the WarehouseExport and Upsert commands. 

To run database export and upload to S3 do this:
    
    $ java -jar usergrid-tools.jar WarehouseExport -upload
    
Once that command completes, upsert the data from S3 to Redshift like so:
    
    $ java -jar usergrid-tools.jar WarehouseUpsert
    
__Step 4__: You need to run the export/upsert periodically so that the warehouse always gets fresh data. So, create a cron job that will run daily and will call usergrid-export.sh to export data from your cluster to Amazon Redshift.

Once you've got data going into Redshift you can use an standard SQL based tool to access and analyze it. Redshift is based on the PostgreSQL database and you can use the standard JDBC and ODBC drivers to access the data.




