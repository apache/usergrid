# Events and counters

Events are typically used for application logging. For example, they
could be used to log error messages that a system administrator might
review at a later time. The event mechanism is optimized to handle large
numbers of events, so it is an ideal mechanism for logging in your
application.

You can link events to specific users, groups, or custom-defined
categories. When created with these links, events offer a powerful tool
for running highly targeted, custom reports.

Events are also the primary way to store custom counter data for
analytics. See Counters (below) for further details.


Event properties
----------------

You can pass various system-defined properties for an event. For
example, you can specify a user property, with a uuid as its value. This
will link the event to the user and can be used to query against the
events queue (see Categorization below). The same is true for the group
property. Pass it with a uuid as the value and the event will be linked
to the group. To include a uuid of a user or group, add the uuid to the
request body, for example:

    POST https://api.usergrid.com/my-org/my-app/events {"timestamp":0, "category" : "advertising", "counters" :  {"ad_clicks" : 5},"user" : "1234891-477d-11e1-b2bd-22005a1c4e22", "group" : "75577d891-347d-2231-b5bd-23400a1c4e22"}

The response body would look something like this:

    {
     "action": "post",
     "application": "7fb8d891-477d-11e1-b2bd-22000a1c4e22",
     "params": {},
     "path": "/events",
     "uri": "https://api.usergrid.com/22000a1c4e22-7fb8d891-477d-11e1-b2bd/7fb8d891-477d-11e1-b2bd-22000a1c4e22/events",
     "entities": [
       {
         "uuid": "ce07ea3c-68b5-11e1-a586-9227e40e3559",
         "user": "1234891-477d-11e1-b2bd-22005a1c4e22",
         "group": "75577d891-347d-2231-b5bd-23400a1c4e22",
         "type": "event",
         "created": 1331166585282,
         "modified": 1331166585282,
         "counters": {
           "ad_clicks": 5
         },
         "metadata": {
           "path": "/events/ce07ea3c-68b5-11e1-a586-9227e40e3559"
         },
         "timestamp": 1331166585282
       }
     ],
     "timestamp": 1331166585018,
     "duration": 919,
     "organization": "my-org",
     "applicationName": "my-app"
    }

You can also create application-specific event properties in addition to
these predefined properties. The system-defined properties are reserved.
You cannot use these names to create other properties for an event
entity. In addition the events name is reserved for the events
collection — you can't use it to name another collection.

The System-defined properties are as follows:

  Property    Type     Description
  ----------- -------- -------------------------------------------------------------------------------------------
  uuid        UUID     Event’s unique entity ID
  type        String   "event"
  created     long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity creation
  modified    long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of entity modification
  timestamp   long     [UNIX timestamp](http://en.wikipedia.org/wiki/Unix_time) of application event (mandatory)
  user        UUID     UUID of application user that posted the event
  group       UUID     UUID of application group that posted the event
  category    string   Category used for organizing similar events
  counters    map      Counter used for tracking number of similar events
  message     string   Message describing event

### Categorizing events

As mentioned previously, you can link an event to a user or a group using a standard [Relationship](/docs/relationships).
This enables you to categorize and qualify event data for use in
tracking and analysis. For example, linking events to users and groups,
enables you to track how often people use a feature. Not only can you
determine the total number of users that used the feature, but also
which groups contain users that made the most use of the feature. Or,
you could provide a location with the event, so you could see how often
a feature was used by people in San Francisco versus Los Angeles.

Counters
--------

User-defined counters are used in conjunction with events to keep
statistics on many aspects of an application. User-defined counters are
JSON objects that are added to events when they are posted. The act of
posting an event increments the counter. Once created, user-defined
counters can be used to get reports.

**Note:** It may take up to 30 seconds for a counter to be updated.

### User-Defined Counters

With Apache Usergrid, you can define your own counters. Some of the things
that you might track with a user-defined counter are:

-   How many times people click on the help button in your application.
-   How many times your game is played each day.
-   How many times your banner ads are clicked each day.

You can choose any name for a user-defined counter. For example, the
counter for the help button might be named “help\_button” or
“help\_button\_clicks”.

To create a user-defined counter, you post to the events collection and
specify the name and increment value of the counter in the request body.
The increment value is the value that the counter is incremented by for
each tracked event. An increment value of 1, means that the counter is
increment by 1 for each tracked event. You could also specify a higher
increment value, such as 15, which would increment the value by that
number, or a negative number, which would decrement the value.  You can
also reset the value of the counter, by specifying an increment value of
0.

When a counter is incremented by an event, there will be a 20-30 second
delay between when the event is posted and when the counter is
incremented. 

As an example, here's how to create a user-defined counter named
“button\_clicks” with an increment value of 1:

    POST https://api.usergrid.com/my-org/my-app/events {"counters" : {"button_clicks" : 1},"timestamp" : "0"}

The response body would look something like this:

    {
       "action": "post",
       "path": "/events",
       "uri": "http://api.usergrid.com/438a1ca1-cf9b-11e0-bcc1-12313f0204bb/events",
       "entities": [
           {
               "uuid": "39d41c46-d8e4-11e0-bcc1-12313f0204bb",
               "type": "event",
               "timestamp": 1315353555546016,
               "category":"advertising",
               "counters": {
                   "button_clicks": 1
               },
               "created": 1315353555546016,
               "modified": 1315353555546016,
               "metadata": {
               "path": "/events/39d41c46-d8e4-11e0-bcc1-12313f0204bb"
               }
           }
       ],
       "timestamp": 1315353555537,
       "duration": 110,
       "organization": "my-org",
       "applicationName": "my-app"
    }

### Counter hierarchy

Counters are hierarchical in their structure and in the way they are
incremented. Each level of the hierarchy is separated by the dot “.”
operator. The hierarchical structure can be useful if you want to store
or view statistics in a hierarchical way. For example, suppose you want
to log errors that your app generates. One way to do this, is to fire an
event every time an error occurs, by creating a counter called “errors”.
However, you can get more detail by adding additional parameters to the
counter. Imagine that you want to track errors in a specific module and
function within that module, say module\_1, function\_1, you could use a
counter like this:

    errors.module_1.function_1

And then for a different function in the same module:

    errors.module_1.function_2

And then for a different function in a different module:

    errors.module_2.function_3

If each of the preceding examples were called once, the resulting values
would be:

    errors = 3
    errors.module_1 = 2
    errors.module_1.function_1 = 1
    errors.module_1.function_2 = 1
    errors.module_2 = 1
    errors.module_2.function_3 = 1

This tells you that there were 3 errors in the application, with 2 of
those errors in module\_1. You can then drill down further to see errors
in specific functions.

### Using counters in time series queries

Knowing the value of a counter is useful. However, you often need to
know how the value varies over time. Fortunately, the API provides a
method for you to view this data over any time interval or level of
granularity.

For example, let’s say you’re incrementing a counter every time someone
launches your application. You might be interested in which days of the
week the application sees the most usage. Using the API, you can examine
the counter over a set of weeks, with the data split into daily
intervals. Using this information, you can see which are your peak days
of usage. You can also view usage across a single day, so you can see if
your application is used more in the mornings or the evenings. For
business reporting, you may be more interested in monthly reporting.

Note that the system is able to provide you with the report data as you
need it. The data is maintained in real-time so it can be viewed
instantly.

### Retrieving counter data

To retrieve counter data, you issue a GET request to the /counters
endpoint. This is a special, built-in collection that gives you access
to all the counters currently in the system. The API also provides a
number of parameters that you can use to search for counter data, as
follows:

  Parameter     Type                                                                                 Description
  ------------- ------------------------------------------------------------------------------------ ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  start\_time   An [Epoch(Unix)](http://en.wikipedia.org/wiki/Unix_time) timestamp in milliseconds   The beginning of the time period to search
  end\_time     An [Epoch(Unix)](http://en.wikipedia.org/wiki/Unix_time) timestamp in milliseconds   The end of the time period to search
  counter       string                                                                               The name of a specific counter to search. You can specify more than one counter in a query (for example, counter=counter1&counter=counter2...)
  resolution    string                                                                               The interval at which counters are displayed. For example, if the interval is day, and the start time and end time values span 4 days, you will get aggregate counts for each of the 4 days. Possible values are all, minute, five\_minutes, half\_hour, hour, six\_day, day, week, and month.

For example, to retrieve a time range of values, with a granularity of
"day", for a counter called "button\_clicks", the GET request would look
like this:

    GET /my-org/my-app/counters?start_time=1315119600000&end_time=1315724400000&resolution=day&counter=button_clicks

The response body would look something like this:

    {
     action: "get",
        uri: "http://api.usergrid.com/438a1ca1-cf9b-11e0-bcc1-12313f0204bb/counters",
        timestamp: 1315354369272,
        duration: 28,
        counters: [
            {
                name: "button_clicks",
                values: [
                    {
                        value: 2
                        timestamp: 1315180800000
                    },
                    {
                        value: 1
                        timestamp: 1315267200000
                    },
                    {
                        value: 1
                        timestamp: 1315353600000
                    }
                ]
            }
        ]
    }
