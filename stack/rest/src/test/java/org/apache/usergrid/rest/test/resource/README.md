#Rest Test Framework (RTF)

##What is it?
It is a java framework that helps abstract some of the difficulty in working with the rest tier. Each endpoint is a different part of the builder pattern that ends with a request at the end. This request is sent to an embedded tomcat that has a usergrid war deployed on it. When the response comes back the RTF parses the response and allows the user to query information returned.

##How does it work?
It works by modeling the REST tier in Usergrid. We construct our query using usergrid endpoints that create our path and after it gets sent using POST/PUT/GET/DELETE.

##Where is it
All of the code is stored in resource. The older resource folder holds the old rest testing framework.

##Requirements
First you must have java 8 running on your machine. Then setup local instances of elasticsearch and cassandra on your box.

##How do I use it?

###For example...

It helps to start out with an example. Let's deconstruct the following line step by step

	clientSetup.getRestClient().management().orgs().post( payload );

1. clientSetup

	1. The clientSetup is what stores the context of the rest call. It also creates a new superuser/org/app/user and stores information specific to each one in the clientsetup. If you don't want to create a new org or app then you are free to call the getters in the clientSetup for the context of the current rest calls.

2. getRestClient()

	1. 	This returns our RestClient that handles the setup for our Jersey WebResource. In the RestClient class we handle the setup of the client and the configuration of the client.

	2. It also stores the top level rest properties that can be called. This client starts by appending the serverUrl to the resource path.

		1. The serverUrl for our rest tests is the localhost:port. These are both automatically assigned.

	3. Next we have the option of calling any of the endpoint detailed in the RestClient.java. You'll see many options but the two most important are probably /management and /org which allow you to access a majority of usergrid endpoints.

3. management()

	1. This appends the "management" endpoint unto our path. Right now we have the following path "localhost:8080/management".

	2. In the ManagementResource class you will find other endpoints that you can call. In this example we're calling ```orgs()```.

4. orgs()

	1. This appends "/organizations" to the path, so now we have "localhost:8080/management/organizations"

	2. This sends us to OrganizationResource.java and will be the first instance where it will give you access to do a REST call.

5. post( payload );

	1. Here the organization class let's us know what endpoints we have available to us. In this case we want to post, but the individual classes will let you know what next steps are available.
	2. So now we have a POST command against the locally embedded tomcat at the following endpoint "localhost:8080/management/organizations" and it will post the payload.
	3. The payload is often a map object but specific types and return types will be covered later in the README.

###ClientSetup
####What does it do?
It setups and stores a new organization/owner/application/superuser for each test. Since this happens at the beginning of each test we can call the ClientSetup class for information when we don't want/need to create our own organization/owner/application/superuser.

####For Example...
	String org = this.clientSetup.getOrganizationName();
	String app = this.clientSetup.getAppName();

    clientSetup.getRestClient().org(org).app(app).collection("cities").get();

The above example is a call you could make in a rest test as soon as you start writing one. You don't have to create the org and application and you can just reference the ones already created for you.

###RestClient
####What does it do?
Handles the setup for our Jersey WebResource. In the RestClient class we handle the configuration of the JerseyClient so we can send our rest calls against the tomcat. The rest client also contains the methods to call all of our top level classes. So from the RestClient we can call any of our top level endpoints.


###AbstractRestIT

####What does it do?
This class handles a lot of the setup required by the testing functionality. It also stores helper methods central to each test and setup the tomcat that the tests will be querying. That is why it is extended by every test class.

####What helper methods does it give me access to?
Every single one of the test queries makes a call from the context ( because it needs access to the unique org and application ) to get the client. There are methods to make the tests look cleaner by automating the calls and getting the endpoint you want without writing

	clientSetup.getRestClient()

###Endpoints
####What does it do?
Holds the classes that contain the builder pattern. There are a lot of classes here currently so its easy to get lost. Following the RestClient through the hierarchy of the calls you want to run will give you the greatest insight into how they are organized.


###Model
This folder handles the data that gets returned from the api and the payloads that go into the api.

####For Example...

To create an organization we can create a model object for the specific kind of entity we want to post.

	Organization payload = new Organization( String orgName, String username, String email, String ownerName, String password, Map<String,Object> properties );

Now that we have an organization payload, we can POST to create the organization.

	Organization org = clientSetup.getRestClient().management().orgs().post( payload );

This in turn creates an Organization object we've called ```org``` that is intialized with the POST response. This gives us easy access to the organization information to verify that it was indeed created correctly.


For a more in-depth explanation for how they work look at the readme in the model folder (Work-In-Progress).



<!-- The below needs to be refactored into a different readme just for the model class.

####How does the RET model responses?
We model responses by serializing the response into its respective model class. At the lowest level we use a ApiResponse. The ApiResponse contains fields for every kind of response we could recieve from the api. If there is any doubt about what class you need to return or push the response in you should use the ApiResponse. From the ApiResponse we model -->
