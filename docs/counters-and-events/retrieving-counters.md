# Retrieving counters
To retrieve a counter, do the following:

## Request syntax

    curl -X GET https://api.usergrid.com/counters?counter=<counter_name>
    
Parameters

Parameter       Description
---------       -----------
counter_name	The name of the counter to be retrieved. 

More than one counter can be retrieved with a single request by appending additional counter parameters to the request URI.

## Example request

    curl -X GET https://api.usergrid.com/my-org/my-app/counters?counter=button_clicks
    
## Example response

    {
      "action" : "get",
      "application" : "f34f4222-a166-11e2-a7f7-02e81adcf3d0",
      "params" : {
        "counter" : [ "button_clicks" ]
      },
      "uri" : "https://api.usergrid.com/your-org/your-app",
      "entities" : [ ],
      "timestamp" : 1401310084096,
      "duration" : 21,
      "organization" : "your-org",
      "applicationName" : "your-app",
      "count" : 0,
      "counters" : [ {
        "name" : "button_clicks",
        "values" : [ {
          "timestamp" : 1,
          "value" : 9
        } ]
      } ]
    }
		
## Retrieving counters by time interval
Knowing the value of a counter is useful; however, you often need to know how the value varies over time. Fortunately, the API provides a method for you to view this data over any time interval or level of granularity.

For example, let’s say you’re incrementing a counter every time someone launches your application. You might be interested in which days of the week the application sees the most usage. Using the API, you can examine the counter over a set of weeks, with the data split into daily intervals. Using this information, you can see which are your peak days of usage. You can also view usage across a single day, so you can see if your application is used more in the mornings or the evenings.

## Request syntax

    curl -X GET https://api.usergrid.com/counters?start_time=<timestamp>&end_time=<timestamp>&resolution=<resolution>&counter=<counter_name>
    
Parameters

Parameter	 Description
---------    -----------
start_time   The beginning of the time period to search
end_time     The end of the time period to search
resolution   The interval at which counters are displayed. 
counter_name The name of the counter to be retrieved.

The following resolutions are supported:

* all
* minute
* five_minutes
* half_hour
* hour
* six_day
* day
* week
* month

For example, if the interval is day, and the start time and end time values span 4 days, you will get aggregate counts for each of the 4 days.

## Example request

For example, to retrieve a time range of values, with a granularity of "day", for a counter called "button_clicks", the GET request would look like this:

    curl -X GET https://api.usergrid.com/my-org/my-app/counters?start_time=1315119600000&end_time=1315724400000&resolution=day&counter=button_clicks
    
## Example response

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
		