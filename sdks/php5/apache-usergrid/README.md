# README 

This a Guzzle Web Service Client and Service Descriptor to work with the Apache Usergrid Management and Application API . 

## Getting Started 

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
or native use just create a config file



    use Apache\Usergrid\UsergridBootstrapper;
    $config = [    
    'usergrid' => [
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
                   ]
             ];
                   
                   $bootstrap = new UsergridBootstrapper($config);
                   $usergrid = $bootstrap->createUsergrid();
                   $collection =$usergrid->application()->getEntity(['collection' => 'shops']);


## Or if you like Static Facades

```
     $bootstrap = new UsergridBootstrapper($config);
     Usergrid::instance($bootstrap);
     $res = Usergrid::application()->EntityGet(['collection' => 'shops']);
```


### Laravel

In Laravel once you have install the composer package you then publish the config file like ```php artisan config:publish apache/usergrid ``` which will publish the config file to the app/config/packages/apache/usergrid/config.php 
then add your client_id and secret to the config file the set the service provider in the app/config.php providers array ```Apache\Usergrid\Laravel\ApacheUsergridServiceProvider``` and add the alias to
the aliases array ```'Usergrid' => 'Apache\Usergrid\Laravel\Facades\Usergrid``` to be able to access class via a Facade. Example for Laravel

```
    $collection = Usergrid::application()->EntityGet(['collection' => 'shops']);
```

## how it works

 You have one main client called Usergrid so if I wanted to call the Management Api and I have Facades enabled then
 I would call like this ```Usergrid::Management->OrgAppsGet();``` or ```Usergrid::Application->EntityGet();```
 
 There is one top level manifest file called Manifest.php and contain only top level Guzzle service descriptor properties and a empty operations array so 
 when calling ```php Usergrid::Management()->OrgAppsGet() ```  the Main Manifest file has the operation array filled in by the Management Manifest files operations array
 and they are cached by the Usergrid web service client. Calls on the Usergrid client are like magic method in the sense that ```php Usergrid::Management()-> method``` call is not
 backed by a Management class or method its the Manifest that it being selected . Also by using Facades all method are like static method and fit in with newer PHP frameworks just like using the
 AWS PHP SDK when calling enableFacades() on the AWS factory method.
 
 All responses are subclasses of Illuminate\Support\Collection class so all collection methods are available to call on your response model eg: first(), get(), map(), fetch(), hasKey(), hasValue(), etc.
 this is not to mistake this with a Usergrid collection which is like your DB table they too are collection object but if you get a response of a single entity then its a Collection with a count of 1.
 
 
 
### Error Handling 

All HTTP and Server error returned by the Usergrid API have error classes attached to the services descriptors so to handle error's that you want too, Just catch the correct exception eg. resource not found.

```
    try {
     $collection = Usergrid::Application()->GetEntity(['collection' => 'shops', 'uuid' => 'not found uuid']);
    } catch(NotFoundException $e) {
        // Handle resource not found
    }
```
 
### Authentication

  You can manage your own Oauth 2 flow by setting the enable_oauth2_plugin config setting to false then you need to call the Token api or get the token from elsewhere and then set the token on the usergrid instance.
  By default this will manage Oauth2 flow for you and I recommend that you leave it set to true. But if you want to do it yourself set the config setting to false and then do something like this.
  
```
    $res  =  Usergrid::management()->authPasswordGet($array);
    $token = $res->get('access_token');
    Usergrid::setToken($token)->users()->findById(['uuid' => '1234']);
```
 
 Authentication for Apache Usergrid uses OAuth-2 protocol and has 4 levels of authentication.
 
* Organization Token  Top level organization access the token is for the organization.
* Organization Admin Token Top level Admin user this token is the organizations admin users.
* Application Token  Per Application token that is for the application.
*  Application User Token User level access this token is for a logged in user.
 
 The Organization and Application token's when using client_credentials should only be used in a server side application as it has full access to all resources.
 The Organization Token can access all applications and edit Organization details. The Application token has full access to all application 
 resources. The Admin user token is the organization admin user so it too has access to all Applications and Organizations and the last level which is a User
 Token that is a per application user and will have access to all resources that roles attached to that user can access.
 
So there are two settings in the config that controls which type of token you get.
the ```'auth_type' => 'application' ``` controls the level you get Organization or Application and the ``` 'grant_type' => 'client_credentials'``` controls
which type of credentials you use which can be either `client_credentials` or `password` using client_id & client_secret or username & password.

### Result Iteration
Apache Usergrid has a default paging size of 10 records so if you ask for a collection that has more then 10 result (entities) then it will return 
a Cursor so you can pass it to the next request to get then next lot of 10 results so Ive made this easier. Using manifest version 1.0.1 and above you can now use a resource iterator  
that will return all entities for you in one request. So again let the SDK do the hard work for you. Resource Iterators as lazy loaded so they only make the api call if the data is needed 
for example when you first make the call no data is requested from the network but as soon as you dereference the data then the first page is requested and the 2nd page is not requested till
you request the data for example if I used it in a foreach loop it will make a network request for the next page of data only after the loop has gone through the current page count and if you cancel the 
foreach loop then it wont request any more data the default page size is 20 for resource iterators.

The SDK will check each response to see if their is a cursor and if there is it will get the next set till all entities are returned.
```
    $allDevices = Usergrid::DevicesIterator();
    foreach($allDevices as $device) {
    // this will have all devices. 
    }
```
# NEW feature - Attributes
### Attributes (relationships) 
The Usergrid PHP SDK has api call on the application service to get a related models but it requires that you make two api calls one to get the entity then a 2nd to get the relationship eg:

```
$users = Usergrid::users()->all(['limit' => 1]);
$user_uuid = $users-entities->fetch('uuid');
$data = [ 'collection' => 'users', 'entity_id' => $user_uuid, 'relationship' => 'devices'];
$device = Usergrid::application()->GetRelationship($data);
```

Now the data we require for you to make the relationship api call is already in the SDK we should not force you to make a 2nd api call and give us data we already have. so now you don't need to any more. Just access the default relationships as a property on your response Model.

```
    $users = Usergrid::users()->all();
    $user = $users->entities->get(0);
    $device = $user->device;
```

that's it, So the take away point is let the SDK do the work for you and if you create custom service descriptors and model classes for your custom collection then look at the User response
model class at it's deviceAttribute method to see how you can either add it to your custom collections or add custom relationship attributes to the default collections. Remember the SDK is designed
for you to be able to extend the Guzzle service descriptors (manifest files).


### HTTP headers and UserAgents

 When working with http clients & server system you may want to sett additional HTTP Headers. Ive have made this easy as well on the Usergrid class you 
 can set any http headers or access token or user agent when calling the set method it will append new headers or replace headers already set so if you 
 have some customer analytics set up on your version of usergrid server then just pass the headers you like in a fluent way eg:
 ``` Usergrid::setHeaders(['BAAS-PLATFORM-ANALYTICS' => 'user001'])->users()->findById(['uuid' => '12343']); ```
 


## Manifest Files (Guzzle & Swagger  Service Descriptors)

All the files in the manifest folder are just temp file the final Service Descriptors are versioned so
the real files are in the manifest/1.0.0 folder so as usergrid is updated new versions can be added like 1.1.0 etc.
Ill leave the other manifest file there for now but will cleanup when Apache Usergrid accepts this library.

## designs guidelines

The design of this is to make it easy to add to existing project and be able to map Model objects in your project 
to response models for example in my project I have a organization object that is saved in a mysql database and I can
call a Usergrid Api call on that model object just like using the Usergrid api class eg:
``` Usergrid::Management->putOrganization($data_array) ``` is the same as
``` Organization::put($data_array) ``` how to do this is beyond the scope of the SDK but its not hard to create 
Gateway Objects using php Traits

## PHPUnit Tests
Some unit tests require a internet connection and a usergrid install so they have been marked with a phpunit @group annotation so to run the tests without a usergrid install just pass the ```--exclude-group internet``` as a option on the command line or IDE setting
that will exclude the few tests that require access to usergrid which I've limited to only a few unit tests like testing if it can get a oauth2 access_token. 

## Javascript

There is a javascript api to go with this for example I have a site that uses the javascript sdk to login to Apache Usergrid then it send the token server side 
to be used in api calls on behalf of the logged in user. You can find this javascript sdk in my public git repo it requires one extra config setting and then you include
the javascript file in your page  It's set to works with Laravel as it posts the token to a route but it would not be hard to use else where just create uri it can post the token too. Its not part of this SDK so think
of it as a helper as some times it good to have access to both world server side calls for long running or large result sets and Ajax calls to update UI using the one login token for both type of calls.


### Contribution guidelines
* Please help if you like this.
* Writing tests
* Code review
* Code
* Manifest files
