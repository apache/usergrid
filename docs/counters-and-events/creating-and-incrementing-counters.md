## Creating & incrementing counters
To create a new counter or increment an existing counter, include the counter property in the body of a POST to the /events endpoint. More than one counter can be incremented in the same request.

__Note__: It may take up to 30 seconds after an event has been posted for the counter to be incremented.

### Request syntax

    curl -X POST https://api.usergrid.com/<org>/<app>/events -d '{"timestamp":<timestamp>, "counters" : {<counter_name>:<increment_value>}}'
    
Parameters

Parameter	    Description
---------       -----------
org	            Organization UUID or organization name
app	            Application UUID or application name
timestamp	    A required UNIX timestamp that specifies the time the counter is being incremented. 
counter_name    The name of the counter to create or the existing counter to increment.
increment_value	The value to increment the counter by. 

Regarding the ``increment_value``, a negative number can be specified to decrement the value. A value of '0' can be specified to reset the value of the counter.

For the ``timestamp``, specifying a value of 0 will automatically assign the current time.

### Example request
The following request will increment the 'button_clicks' counter by one, with a timestamp of the current time.

    curl -X POST https://api.usergrid.com/your-org/your-app/events -d '{"timestamp":0, "counters" : {"button_clicks":1}}'
    
### Example response

    {
      "action" : "post",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : { },
      "path" : "/events",
      "uri" : "https://api.usergrid.com/your-org/your-app/events",
      "entities" : [ {
        "uuid" : "b11217fc-9d3a-1427-b24e-699740088e05",
        "type" : "event",
        "created" : 1401224590293,
        "modified" : 1401224590293,
        "timestamp" : 1401224590293,
        "counters" : {
          "button_clicks" : 1
        },
        "message" : null,
        "metadata" : {
          "path" : "/events/b11217fc-9d3a-1427-b24e-699740088e05"
        }
      } ],
      "timestamp" : 1401224590291,
      "duration" : 30,
      "organization" : "your-org",
      "applicationName" : "your-app"
    }
		
## Decrementing/resetting counters
To decrement a counter, specify a negative number for the counter value in any of the above examples.

To reset a counter, specify a value of 0 for the counter value in any of the above examples.

Note that the Usergrid JavaScript SDK also provides dedicated methods for decrementing and resetting counters.

## Using counters hierarchically

You can organize counters into hierarchies by giving them dot-separated names, e.g. ``button_clicks.homepage``. Incrementing a counter lower in a hierarchy increments all of the counters upward in the hierarchy chain. 

For example, you want to log errors that your app generates, so you create hierarchical counters for each module and function within that module. In this example, you create the following set of counters:

    errors
    errors.module
    errors.module.function

Incrementing ``errors.module.function`` by 1 increments all three counters by 1. A hierarchy can be a useful way of easily tracking actions in your app at both a cumulative and granular level.