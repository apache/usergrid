# README #

This a Guzzle Web Service Client and Service Descriptor to work with the Apache Usergrid Management and Application API . 

## Getting Started ##
install as composer package by adding this to your composer.json file.

``` 
"apache-usergrid" : "dev-master" 
```

and add the URL to the repositories section within your composer.json file.

```
"repositories": [{
    "type" : "vcs",
    "url" : "https://apps4u@bitbucket.org/apps4u/apache-usergrid.git"
 }]
```

import the classes ``` include autoload.php ``` then create a new instance of the class with a path to you config file

For native use just create a config file.

```
use Apache\Usergrid\UsergridBootstrapper;

    $config = [    'usergrid' => [
                       'url' => 'https://api.usergrid.com',
                       'version' => '1.0.0',
                       'orgName' => '',
                       'appName' => '',
                       'manifestPath' => './src/Manifests',
                       'clientId' => '',
                       'clientSecret' => '',
                       'username' => '',
                       'password' => '',
                       /**
                        * The Auth Type setting is the Oauth 2 end point you want to get the OAuth 2
                        * Token from.  You have two options here one is 'application' the other is 'organization'
                        *
                        *  organization will get the the token from http://example.com/management/token using  client_credentials or password grant type
                        *  application will get the token from http://example.com/org_name/app_name/token using client_credentials or password grant type
                        */
                       'auth_type' => 'application',
                       /** The Grant Type to use
                        *
                        * This has to be set to one of the 2 grant types that Apache Usergrid
                        * supports which at the moment is client_credentials or password.
                        */
                       'grant_type' => 'client_credentials'
                        /**
                        * if you want to manage your own auth flow by calling the token api and setting the token your self just set this to false
                        * */
                       'enable_oauth2_plugin' => true
                   ]];
                   
                   $bootstrap = new UsergridBootstrapper($config);
                   $usergrid = $bootstrap->createUsergrid();
                   $collection =$usergrid->application()->getEntity(['collection' => 'shops']);
                   
```

Or if you like Static Facades

```
use Apache\Usergrid\Native\Facades\Usergrid;

$bootstrap = new UsergridBootstrapper($config);
Usergrid::instance($boostraper);
$res = Usergrid::application()->EntityGet(['collection' => 'shops']);

```


### Laravel ###
In Laravel once you have install the composer package you then publish the config file like ```php artisan config:publish apache/usergrid ``` which will publish the config file to the app/config/packages/apache/usergrid/config.php 
then add your client_id and secret to the config file the set the service provider in the app/config.php providers array ```Apache\Usergrid\Laravel\ApacheUsergridServiceProvider``` and add the alias to
the aliases array ```'Usergrid' => 'Apache\Usergrid\Laravel\Facades\Usergrid``` to be able to access class via a Facade. Example for Laravel

```
    $collection = Usergrid::application()->getEntity(['collection' => 'shops']);
```

## how it works ##

 You have one main client called Usergrid so if I wanted to call the Management Api and I have Facades enabled then
 I would call like this ```Usergrid::Management->getApps();``` or ```Usergrid::Application->getEntity();```
 
 There is one top level manifest file called Manifest.php and contain only top level Guzzle service descriptor properties and a empty operations array so 
 when calling ```php Usergrid::Management()->getEntity() ```  the Main Manifest file has the operation array filled in by the Management Manifest files operations array
 and they are cached by the Usergrid web service client. Calls on the Usergrid client are like magic method in the sense that ```php Usergrid::Management()-> method``` call is not
 backed by a Management class or method its the Manifest that it being selected . Also by using Facades all method are like static method and fit in with newer PHP frameworks just like using the
 AWS PHP SDK when calling enableFacades() on the AWS factory method.
 
### Error Handling ### 
All HTTP and Server error returned by the Usergrid API have error classes attached to the services descriptors so to handle error's that you want too, Just catch the correct exception eg. resource not found.

```
try {
 $collection = Usergrid::Application()->GetEntity(['collection' => 'shops', 'uuid' => 'not found uuid']);
} catch(NotFoundException $e) {
    // Handle resource not found
}

```
 
### Authentication ###
  You can manage your own Oauth 2 flow by setting the enable_oauth2_plugin config setting to false then you need to call the Token api or get the token from elsewhere and then set the token on the usergrid instance.
  By default this will manage Oauth2 flow for you and I recommend that you leave it set to true. But if you want to do it yourself set the config setting to false and then do something like this.
  
```
 $res  =  Usergrid::management()->authPasswordGet($array);
 $token = $res->get('access_token');
 Usergrid::setToken($token);
 
```
 
 Authentication for Apache Usergrid uses OAuth-2 protocol and has 4 levels of authentication .
 * Organization Token -- Top level organization access the token is for the organization.
 * Organization Admin Token -- Top level Admin user this token is the organizations admin users.
 * Application Token -- Per Application token that is for the application
 * Application User Token -- User level access this token is for a logged in user.
 
 The Organization and Application token's when using client_credentials should only be used in a server side application as it has full access to all resources.
 The Organization Token can access all applications and edit Organization details. The Application token has full access to all application 
 resources. The Admin user token is the organization admin user so it too has access to all Applications and Organizations and the last level which is a User
 Token that is a per application user and will have access to all resources that roles attached to that user can access.
 
So there are two settings in the config that controls which type of token you get.
the ```'auth_type' => 'application' ``` controls the level you get Organization or Application and the ``` 'grant_type' => 'client_credentials'``` controls
which type of credentials you use which can be either client_id & client_secret or username & password.

### Result Iteration ###
Apache Usergrid has a default paging size of 10 records so if you ask for a collection that has more then 10 result (entities) then it will return 
a Cursor so you can pass it to the next request to get then next lot of 10 results so Ive made this easier. Using manifest version 1.0.1 and above you can now use a resource iterator  
that will return all entities for you in one request. So again let the SDK do the hard work for you.

``` 
//The SDK will check each response to see if their is a cursor and if there is it will get the next set till all entities are returned.
$allDevices = Usergrid::DevicesIterator();

foreach($allDevices as $device) {
// this will have all devices. 
}
```

## Manifest Files (Guzzle & Swagger  Service Descriptors) ## 
All the files in the manifest folder are just temp file the final Service Descriptors are versioned so
the real files are in the manifest/1.0.0 folder so as usergrid is updated new versions can be added like 1.1.0 etc.
Ill leave the other manifest file there for now but will cleanup when Apache Usergrid accepts this library.

## designs guidelines ##
The design of this is to make it easy to add to existing project and be able to map Model objects in your project 
to response models for example in my project I have a organization object that is saved in a mysql database and I can
call a Usergrid Api call on that model object just like using the Usergrid api class eg:
``` Usergrid::Management->putOrganization($data_array) ``` is the same as
``` Organization::put($data_array) ``` how to do this is beyond the scope of the SDK but its not hard to create 
Gateway Objects using php Traits


## Javascript ##
There is a javascript api to go with this for example I have a site that uses the javascript sdk to login to Apache Usergrid then it send the token server side 
to be used in api calls on behalf of the logged in user. You can find this javascript sdk in my public git repo it requires one extra config setting and then you include
the javascript file in you page but It only works with Laravel as it posts the token to a route bu it would not be hard to use else where its not part of this SDK so think
of it as a helper as some times it good to have access to both world server side calls for and Ajax calls using the one login token.


### Contribution guidelines ###
* Please help if you like this.
* Writing tests
* Code review
* Code
* Manifest files