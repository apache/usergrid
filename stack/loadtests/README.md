To make it easy for you to load test your instance of Usergrid, we have bundledin the Gatling load test tool, along with some pre-built tests of different functionality. To get started do the following:

### Setting up Gatling
1. Unzip loadtest.zip
2. cd to the 'gatling' dir
3. Run 'sh loadtest_setup.sh'. This will do the following:
	- Add some handy options to gatling/bin/gatling.sh that will allow you to set certain test parameters using environment variables (more on this later)
	- Run the PostUsersSimulation, which will load 5k users with geolocation data into a specified UG org/app. This is just to seed some data entities to make it easier to run some of the tests.
4. Set the following environment variables:
- GATLING_BASE_URL - Required. UG base url, e.g. http://api.usergrid.com/.
- GATLING_ORG      - Required. UG organization name.
- GATLING_APP      - Required. UG application name.

- GATLING_NUMUSERS - Number of users in the simulation. Default is 100.
- GATLING_DURATION - Duration of the simulation. Default is 300.
- GATLING_RAMPTIME - Time period to inject the users over. Default is 0.
- GATLING_THROTTLE - Requests per second the simulation to try to reach. Default is 50.

- GATLING_NOTIFIER - Name of the notifier to use for PushNotificationSimulation.
- GATLING_PROVIDER - Push notification provider that corresponds to the notifier, e.g. apple, google, etc.

### Running load tests
To run Gatling, do the following:
1. Run 'gatling/bin/gatling.sh'
2. Enter the number of the test you want to run from the list (see below for an explanation of each test)
3. Optional. Set a identifier for the results of this run of the simulation
4. Optional. Set a description for this run of the simulation

### Viewing results
Results of the test are output to the gatling/results. The output directory is shown once the test has successfully run. The location of the generated report is also shown.

### Default tests
The following default tests are available. Not that the GATLING_BASE_URL, GATLING_ORG, and GATLING_APP environment variables must be set before any tests can be run. Each test also requires certain additional env variables to be set.

- PostUsersSimulation

POSTs 5k entities with geolocation data to /users. Entities are named sequentially, i.e. user1, user2, etc.

- GetEntitySimulation

Performs simple GETs on the /users collection. You should run PostUsersSimulation or loadtest_Setup.sh first to load data into the collection.

- PostDevicesSimulation

POSTs a user-specified number of entities in the /devices collection. This is useful if you want to load test push notifications

- PushNotificationSimulation

Sends push notifications. To run this, you will need to do create a notifier, then set the GATLING_NOTIFIER environment variable to equal the name or UUID of the notifier. You'll also need to set GATLING_PROVIDER to match the provider in the notifier.