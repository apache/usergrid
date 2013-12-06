# A Simple Performance Testing Framework on AWS

This is a simple performance testing framework designed to pound the heck out 
of a clustered persistence teir. It is designed as a web application that 
can be run on several tomcat or jetty servers to bombard in unison an in JVM 
API that operates against a clustered data storage layer like Cassandra.

## Setting up a Perftest

The framework simply executes a number of calls which you specify using a 
Perftest implementation class. This class specifies all the parameters as 
methods and is construct by the framework using a TestModule (a guice module)
which you also provide.

The framework simply loads your TestModule and uses its Guice Injector to 
create the Perftest instance. It coordinates executing calls against the
Perftest instance across the set of servers containing the framework. SimpleDB
is used to communicate presence to all the nodes of the workers so they can
find each other and coordinate their calls at the same time against the 
Perftest instance.

The following endpoints are used to control the framework:

 * /perftest/writeStart
 * /perftest/stop
 * /perftest/reset
 * /perftest/stats

The following ascii text shows the states of the framework which one can 
go through while issuing POSTs to the end points above:

            writeStart           stop
    +-----+       +-------+     +-------+
 ---+ready+-------+running+-----+stopped|
    +--+--+       +-------+     +---+---+
       |                            |
       |____________________________|
                   reset

A post to a single node issues the same POST to all the nodes in the cluster.

## Dependencies

It uses the following libraries to do what it does:

* Jersey - for REST services
* Jackson - for JSON <--> Java marshalling
* Guice - for the DI container
* Archaius - for dynamic properties
* Blitz4j - for asynchronous logging
* Slf4j - API for binding to Blitz4j

