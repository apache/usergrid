#Gatling Load Tests
The Usergrid loadtests directory (/stack/loadtests) contains a framework for performance testing [Apache Usergrid](http://usergrid.apache.org/). These tests currently use version 2.1.7 of [Gatling](http://gatling.io), an open-source load-testing tool. This version of Gatling, as well as the tests, use Java 7.

The test code is written in [Scala](http://www.scala-lang.org/), which is Gatling's test language.

##Usergrid Gatling test scripts
The test scripts are found in the top level loadtests directory. Look inside the scripts to see the details for running the tests.

###testConfig.sh
Contains defaults that are used for all the other test scripts.

###runAuditGetAllAppCollectionEntities.sh
For a specified organization, finds all apps, and for each app, finds all collections and writes a CSV file line containing collection name, UUID, entity name, and modified timestamp for each entity in each collection.

###runAuditGetCollectionEntities.sh
For a specified organization and application, finds all collections and writes a CSV file line containing collection name, UUID, entity name, and modified timestamp for each entity in each collection.

###runAuditVerifyCollectionEntities.sh
For a specified organization and given CSV file, verify that all entities are retrievable, writing those that fail to another CSV file.

###runCollectionQueryTest.sh
For a given collection, retrieve all entities using a cursor and a query.

###runCustomInjectRandomEntityByUuidTest.sh
Gets random entity by UUIDs read from a CSV file. Includes custom injection steps.

###runDeleteEntities.sh
Deletes entities in order via name (prefix + entity number).

###runGetEntitiesByNameSequential.sh
Retrieves entities one by one via name (prefix + entity number).

###runGetEntitiesByUuidTest.sh
Retrieves entities via UUID from a CSV file. Control the CSV feed pattern (circular=sequential, random) using the csvFeedPattern configuration

###runLoadEntities.sh
Creates entities in order via name (prefix + entity number).

###runLoadLargeMultiFieldEntities.sh
Creates large multi-field entities in order via name (prefix + entity number).

###runLoadNoNameEntities.sh
Creates entities in order without names.

###runLoadSortableEntities.sh
Creates sortable entities in order via name (prefix + entity number).

###runOrgAppSetup.sh
Runs the organization and app setup without doing a test.

###runRandomEntityByNameQueryTest.sh
Retrieves random entities via name (prefix + entity number) using queries.

###runRandomEntityByNameTest.sh
Retrieves random entities via name (prefix + entity number).

###runUpdateEntities.sh
Updates entities in order via name (prefix + entity number).

##Gatling configuration items
Understanding how configuration items work together can best be accomplished by reading the Usergrid Gatling test scripts. Some configuration items are dependent on others, and some are ignored by specific tests. Configuration items and their defaults can be found in the ConfigProperties.scala enumeration in the enums directory. The spelling of each item below is used in Maven calls via -D{configName}={value} (for example, -Dorg=gatlingtest).

Defaults listed are those that are specified by the Usergrid Gatling code, not necessarily defaults in the test scripts. Defaults are **bold**.

* org (no default) - organization name
* app (no default) - application name
* collection (**"customentities"**) - collection name
* sandboxCollection (**false**) - set permissions to allow all users read/write of collection (for anonymous access)
* adminUser (no default) - username for administrative user (to get management tokens)
* adminPassword (no default) - password for administrative user
* appUser (no default) - username for application (non-management) user
* appUserPassword (no default) - password for application user
* baseUrl (no default) - base Usergrid URL (for example, https://api.usergrid.com); trailing slash is optional
* authType (**"anonymous"**, "token", "basic") - type of authorization for test (does not affect setup operations)
* tokenType ("none", **"user"**, "management") - type of token to use if authType = token
* skipSetup (**false**) - skip entire setup section (can include creation of org and/or app, loading entities, and/or setting a collection to have world read-write permissions)
* createOrg (**false**) - create the organization specified by the org configuration item (will continue if the org already exists)
* createApp (**false**) - create the application specified by the app configuration item (will continue if the app already exists)
* loadEntities (**false**) - load entities as part of setup, instead of as part of the test
* scenarioType (**"nameRandomInfinite"**, "uuidInfinite", "getByNameSequential", "getAllByCursor", "loadEntities", "updateEntities", "deleteEntities", "auditGetCollectionEntities", "auditVerifyCollectionEntities") - type of scenario to run, more details in test scripts section
* rampUsers (**0**) - number of users to inject during the ramp up time
* rampTime (**0**) - duration in seconds of the ramp up time
* constantUsersPerSec (**0**) - number of users per second to inject during the constant injection time (decimal ok)
* constantUsersDuration (**10**) - duration in seconds of the constant injection time
* numEntities (**5000**) - number of entities for the test
* userSeed (**1**) - initial user seed; for example, if userSeed=5001 and numEntities=5000, entities 5001-10000 will be created
* entityPrefix (**"entity"**) - prefix for entity name (example of entity name including prefix and entity number: "entity1")
* entityType ("trivial", "trivialSortable", **"basic"**, "largeMultiField") - type of entity to create (trivial = 1 field, trivialSortable = trivial + sortableField containing random integer, basic = several fields with random values, largeMultiField = large entities with *multiPropertyCount* fields, each containing a string *multiPropertySizeInK* * 1000 characters long
* multiPropertyPrefix (**"prop"**) - prefix for the largeMultiField entity's properties
* multiPropertyCount (**1**) - number of properties for each entity
* multiPropertySizeInK (**1**) - each property contains a string that is this number of kilobytes long
* entityNumberProperty (no default) - property name that should contain the entity number (can be used for querying)
* searchQuery (**""**) - query to be used for searching during test
* searchLimit (**0**) - limit to be returned on searches
* endConditionType (**"minutesElapsed"**, requestCount, unlimited) - end condition for the test (minutes elapsed, number of requests made, or never end)
* endMinutes (**10**) - number of minutes to run test (if endConditionType = minutesElapsed)
* endRequestCount (**1000**) - number of requests before test is ended (if conditionType = requestCount)
* orgCreationUsername (no default) - username of admin account for org created during setup
* orgCreationName (no default) - name of admin for org created during setup
* orgCreationEmail (no default) - email address of admin for org created during setup
* orgCreationPassword (**"test"**) - password for admin account for org created during setup
* updateProperty (**"updateProp"**) - property name to be updated for update test
* updateValue (**{current date}**) - value property should be given for update test
* entityWorkerCount (**0**) - number of Gatling instances to be run at a time
* entityWorkerNum (**0**) - worker number for this Gatling instance
* uuidFilename (no default) - UUID filename for non-audit tests
* auditUuidFilename (no default) - UUID filename for audit tests
* failedUuidFilename (no default) - UUID filename for failed entries while auditing
* retryCount (**5**) - number of retries of operation before giving up
* purgeUsers (**100**) - number of entities to create at a time during loadEntities (send this many requests, then wait for the responses)
* laterThanTimestamp (**0**) - if specified for an audit, will only match entities modified >= this timestamp; can be used for incremental audits
* entityProgressCount (**10000**) - print to console every time this many requests has been sent (if entityProgressCount = 10000, will print on the 1000th, 2000th, 3000th... request)
* injectionList (**"rampUsers(10,60)"**) - custom injection pattern for CustomInjectionSimulation (discussed below)
* printFailedRequests (**true**) - prints the request and response on the console for failed requests (those that fail more than *retryCount* times)
* getViaQuery (**false**) - retrieve entities via query instead of via name or uuid
* queryParams (**""**) - additional query parameters (currently used for get by entity or by name)
* csvFeedPattern (**"random"**) - pattern to use when feeding from a CSV ("random" is random, "circular" goes through CSV sequentially and restarts from beginning when it reaches the end)

The following settings are currently not used (were used by deprecated tests, but may be valid in the future):

* numDevices (**4000**) - number of devices for a push notification test
* pushNotifier (**"loadNotifier"**) - push notifier
* pushProvider (**"noop"**) - push provider
* throttle (**50**) - maximum number of users at a time
* holdDuration (**300**) - duration for test in seconds

##User injection for tests
The models for injection are based upon the simulation type chosen.

###ConfigurableSimulation
The configurable simulation currently has a single ramp up period (*rampUsers* are injected evenly over *rampTime* seconds) followed by a constant injection period (*constantUsersPerSec* are injected per second over *constantUsersDuration* number of seconds). This is a fairly simple injection pattern -- use CustomInjectionSimulation for more control.

###CustomInjectionSimulation
*injectionList* allows a custom injection pattern for user creation by passing in a single string containing [injection steps](http://gatling.io/docs/2.1.7/general/simulation_setup.html#injection) separated by semicolons.

* atOnceUsers(nbUsers) - injects a given number of users at once (Gatling injection example: atOnceusers(10) )
* constantUsersPerSec(rate,duration) - injects users at a constant rate, defined as users per second, during a given duration (Gatling injection example: constantUsersPerSec(20) during(15 seconds) )
* constantUsersPerSecRandomized(rate,duration) - injects users at a constant rated, defined in users per second, during a given duration; users will be injected at randomized intervals (Gatling injection example: constantUsersPerSec(20) during(15 seconds) randomized)
* heavisideUsers(nbUsers,duration) - injects a given number of users following a smooth approximation of the heaviside step function stretched to a given duration (Gatling injection example: heavisideUsers(1000) over(20 seconds) )
* nothingFor(duration) - pause for a given duration (Gatling injection example: nothingFor(4 seconds) )
* rampUsers(nbUsers,duration) - injects a given number of users with a linear ramp over a given duration in seconds (Gatling injection example: rampUsers(10) over 5 seconds)
* rampUsersPerSec(rate1,rate2,duration) - injects users from starting rate to target rate, defined in users per second, during a given duration; users will be injected at regular intervals (Gatling injection example: rampUsersPerSec(rate1) to (rate2) during(duration) )
* rampUsersPerSecRandomized(rate1,rate2,duration) - injects users from starting rate to target rate, defined in users per second, during a given duration; users will be injected at randomized intervals (Gatling injection example: rampUsersPerSec(rate1) to (rate2) during(duration) randomized)

Example injectionList string: "rampUsers(100,300);nothingFor(60);constantUsersPerSecRandomized(10,120)"

###AuditSimulation
Audit simulation has only a single ramp up period (specified by *rampUsers* and *rampTime*).

##loadtest structure
Feel free to skip this section -- it contains information to help you understand the code

* /stack/loadtests - Top level directory
	* pom.xml - Gatling's Maven POM file
	* testConfig.sh - contains defaults for configuration items (listed above)
	* run*.sh - scripts that simplify running Usergrid Gatling test scripts (listed above)

* /stack/loadtests/src - Code for Usergrid tests

* /stack/loadtests/target - Contains code and resources when tests are compiled and run

* /stack/loadtests/src/main/scala/org/apache/usergrid/datagenerators - Contains code to generate [Gatling feeders](http://gatling.io/docs/2.1.7/session/feeder.html) and build entities to be stored in Usergrid collections during tests

* /stack/loadtests/src/main/scala/org/apache/usergrid/enums - Contains enumerations for test configuration items

* /stack/loadtests/src/main/scala/org/apache/usergrid/helpers - Contains helper functionality
	* Extractors.scala - extracts information from payloads and injects information into the [Gatling session](http://gatling.io/docs/2.1.7/session/session_api.html)
	* Headers.scala - common headers to be added into generated requests
	* Setup.scala - contains functions to set up a test (for example, creation of an organization or application) and get OAuth tokens
	* Utils.scala - utilities to create random elements and URLs

* /stack/loadtests/src/main/scala/org/apache/usergrid/scenarios - Contains [Gatling scenarios](http://gatling.io/docs/2.1.7/general/scenario.html) used in the Gatling simulations; most of the current tests use the scenarios in EntityCollectionScenarios.scala and AuditScenarios.scala

* /stack/loadtests/src/main/scala/org/apache/usergrid/settings - Contains code to handle test settings and generate CSV files for use in testing

* /stack/loadtests/src/main/scala/org/apache/usergrid/simulations - Contains [Gatling simulations](http://gatling.io/docs/2.1.7/general/simulation_structure.html) for running the tests
	* AuditSimulation.scala - Tests for a) finding all entities in an organization and writing the names and UUIDs to a CSV file, and b) validating the existence of the entities in the CSV file; audit tests can be used to test that a copy/migration of an organization is complete
	* ConfigurableSimulation.scala - contains many different types of tests that can be configured via Gatling test shell script or Maven Gatling call
	* CustomInjectionSimulation.scala - tests that allow full configuration of [Gatling user injection] via Gatling test shell script or Maven Gatling call

##Running tests using Maven
Gatling uses [Apache Maven](https://maven.apache.org/) to handle dependencies and run the tests. The test scripts run Gatling via Maven, and have good examples of the Maven calls.

###Example Maven (run from loadtests directory)

    > mvn gatling:execute \
       -Dorg=myorg
       -Dapp=myapp
       -DbaseUrl=http://api.usergrid.com
       ...
       -DscenarioType=loadEntities
       -Dgatling.simulationClass=org.apache.usergrid.simulations.ConfigurableSimulation





