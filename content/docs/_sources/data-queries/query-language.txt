# Query Language

> Query examples in this content are shown unencoded to make
> them easier to read. Keep in mind that you might need to encode query
> strings if you're sending them as part of URLs, such as when you're
> executing them with the cURL tool.

The following example retrieves a list of restaurants (from a
restaurants collection) whose name property contains the value "diner",
sorting the list in ascending order by name:

    /restaurants?ql=select * where name contains 'diner' order by name asc


## Basic syntax

Queries of Usergrid data for Apache Usergrid are made up of two kinds of
statements: the path to the collection you want to query, followed by
the query language statement containing your query. These two statements
are separated by "?ql=" to indicate where the query language statement
starts.

To retrieve items from a collection, you would use a syntax such as the
following:

    /<collection>?ql=<query_statement>

In the following example, the query is retrieving all users whose name
is Gladys Kravitz.

    /users?ql=select * where name = 'Gladys Kravitz'

The following example selects all items except those that have an a
property value of 5:

    /items?ql=select * where NOT a = 5

Note that there is a shortcut available when your query selects all
items matching certain criteria -- in other words, where you use a
statement that starts "select \* where". In this case, you can omit the
first part of the statement and abbreviate it this way:

    /items?ql=NOT a = 5

You query your Apache Usergrid data by using a query syntax that's like
Structured Query Language (SQL), the query language for relational
databases. Unlike a relational database, where you specify tables and
columns containing the data you want to query, in your Apache Usergrid
queries you specify collections and entities.

The syntax of Apache Usergrid queries only *resembles* SQL to
make queries familiar and easier to write. However, the language isn't
SQL. Only the syntax items documented here are supported.

## Supported operators

Comparisons

* Less than `<` or `lt`
* Less than or equal `<=` or `lte`
* Equal `=` or `eq`
* Greater than or equal `>=` or `gte`
* Greater than `>` or `gt`
* Not equal `NOT`

Logical operations

* Intersection of results `and`
* Union of results `or`
* Subtraction of results `not`


## Query Response Format

the query’s response is formatted in
JavaScript Object Notation (JSON). This is a common format used for
parameter and return values in REST web services.

So for the following query:

    /users?ql=select * where name = ‘Gladys Kravitz’

...you would get a response such as the the one below. The JSON format
arranges the data in name/value pairs. Many of the values correspond to
specifics of the request, including the request’s HTTP action (GET), the
application’s UUID, the request’s parameters (the query string you
sent), and so on.

Here, the query is asking for whole entities in the users collection.
Data corresponding to the response is captured in the response’s
`entities` array. The array has one member here, corresponding to the
one user found by the query (another kind of query might have found more
users). That one member gives the UUID of the entity (user), the entity
type, and values for properties such as name, username, email, and so
on.

```json
{
  "action" : "get",
  "application" : "8272c9b0-d86a-11e2-92e2-cdf1ce04c1c0",
  "params" : {
    "ql" : [ "select * where name = 'Gladys Kravitz'" ]
  },
  "path" : "/users",
  "uri" : "http://api.usergrid.com/myorg/myapp/users",
  "entities" : [ {
    "uuid" : "d0d7d0ba-e97b-11e2-8cef-411c466c4f2c",
    "type" : "user",
    "name" : "Gladys Kravitz",
    "created" : 1373472876859,
    "modified" : 1373472876859,
    "username" : "gladys",
    "email" : "gladys@example.com",
    "activated" : true,
    "picture" : "http://www.gravatar.com/avatar/20c57d4f41cf51f2db44165eb058b3b2",
    "metadata" : {
      "path" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c",
      "sets" : {
        "rolenames" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/rolenames",
        "permissions" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/permissions"
      },
      "connections" : {
        "firstname" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/firstname",
        "lastname" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/lastname"
      },
      "collections" : {
        "activities" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/activities",
        "devices" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/devices",
        "feed" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/feed",
        "groups" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/groups",
        "roles" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/roles",
        "following" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/following",
        "followers" : "/users/d0d7d0ba-e97b-11e2-8cef-411c466c4f2c/followers"
      }
    }
  } ],
  "timestamp" : 1374694196061,
  "duration" : 48,
  "organization" : "myorg",
  "applicationName" : "myapp",
  "count" : 1
}
```

Compare the preceding example with the following for another kind of
query. Imagine the following request string, where the query string is
asking for only the values of two of the entity’s properties (username
and name):

    /users?ql=select username,name where name=’Gladys Kravitz’

In the response JSON from this query, the return value is specified as
the property of the `list` item -- here, an array containing only the
values of the properties the query asked for, in the order they were
requested (username first, then name).

    {
      "action" : "get",
      "application" : "8272c9b0-d86a-11e2-92e2-cdf1ce04c1c0",
      "params" : {
        "ql" : [ "select username,name where name='Gladys Kravitz'" ]
      },
      "path" : "/users",
      "uri" : "http://api.usergrid.com/myorg/myapp/users",
      "list" : [ [ "gladys", "Gladys Kravitz" ] ],
      "timestamp" : 1374697463190,
      "duration" : 25,
      "organization" : "myorg",
      "applicationName" : "myapp",
      "count" : 1
    }


## Data types supported in queries

As you develop queries for your Apache Usergrid data, remember that entity
properties each conform to a particular data type (whether the entity is
included by default or an entity you defined). Your queries must
acknowledge this, testing with values that conform to each property's
data type. (You can view the list of property data types for the default
entities at [Default Data Entities](/default-data-entities).)

For example, in the default entity `User`, the `name` property is stored
as a `string`, the created date as a `long`, and metadata is stored as a
JSON object. Your queries must be data type-aware so that you can be
sure that query results are as you expect them to be.

So imagine you define an entity with a `price` property whose value
might be `100.00`. Querying for `100` will return no results even if
there are occurrences of `100.00` as `price` values in your data set.
That's because the database expected a decimal-delimited `float` value
in your query.


Data Type     Examples                                                                                    Notes
----------- ------------------------------------------------------------------------------------------- ---------
`string`    `'value'`, `unicode '\uFFFF'`, `octal '\0707'`                                              true | false
`long`      1357412326021                                                                               Timestamps are typically stored as `long` values.
`float`     10.1, -10.1, 10e10, 10e-10, 10E10, 10e-10                                                   Your query must be specific about the value you're looking for, down to the value (if any) after the decimal point.
`boolean`   true | false                                      
`UUID`      ee912c4b-5769-11e2-924d-02e81ac5a17b                                                        UUID types are typically used for the unique IDs of entities. The value must conform to the following format (do not enclose with quotation marks): xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx


`object`    For a JSON object like this one:

```
  {
    "items":[
      {"name":"rocks"},
      {"name":"boats"}
    ]
  }
```

you can use dot notation to reach property values in the object: /mycollection/thing?ql="select * where items.name = 'rocks'"                        Objects are often used to contain entity metadata, such as the activities associated with a user, the users associated with a role, and so on.

## Retrieving values for multiple properties

Your query can return multiple kinds of values -- such as the values of
multiple properties -- by specifying the property names in your select
statement as a comma-separated list.

For example, the following request returns the address and phone number
of users whose name is Gladys Kravitz:

    /users?ql=select address,phone_number where name = 'Gladys Kravitz'


## Querying for the contents of text

Your query can search the text of entity values of the string data type.
For example, you can search a postal code field for values that start
with a specific three numbers.

For example, the following query selects all restaurants with the word
`diner` in the name:

    /restaurants?ql=select * where name contains 'diner'

**Note:** Not all string properties of the default entities are
indexed for searching. This includes the `User` entity's `username`
property.

This will return all users whose name property contains the word 'Kravitz'

    /users?ql=select * where name contains 'Kravitz'

This will return all users whose name property contains a word beginning with 'Krav'

    /users?ql=select * where name contains 'Krav*'

This will return all users whose name is exactly 'Gladys Kravitz'

    /users?ql=select * where name = 'Gladys Kravitz'


## Sorting results

You can return query results that are sorted in the order you specify.
Use the `order by` clause to specify the property to sort by, along with
the order in which results should be sorted. The syntax for the clause
is as follows `order by <property_name> asc | desc`

The following table includes a few examples:

    /users?ql=select * where lastname = 'Smith' order by firstname asc


    /users?ql=select * where lastname = 'Smith' order by firstname desc


    /users?ql=select * where lastname contains 'Sm*' order by lastname asc, firstname asc


## Geoqueries

Many of today's apps are enhanced by the use of *geolocation*, wireless
detection of the physical location of a remote device. These apps are
said to be *geolocation-aware* in that they query the device to
determine the user's position and then use this data to further enhance
the user's experience. For example, apps can capture the exact location
where a picture was taken or a message was created.

Usergrid support geolocation on any entity, both built in (e.g.,
users, groups) and user defined.

To add a location to any entity, include the following member to the
JSON in a POST or PUT call:

    "location": {
        "latitude": 37.779632,
        "longitude": -122.395131  
    } 

For example, to store a listing of restaurants and their locations,
start by creating a collection called restaurants:

    POST https://api.usergrid.com/org_name/app_name/restaurants

Next, add a new entity to the collection:

    POST https://api.usergrid.com/org_name/app_name/restaurants
    {
      "name": "Rockadero",
      "address": "21 Slate Street, Bedrock, CA",
      "location": {
        "latitude": 37.779632,
        "longitude": -122.395131
      }
    }

This creates a new restaurant entity called "Rockadero" with the
longitude and latitude included as part of the object.

When a location is added to an entity, it is easy to make queries
against that data. For example, to see all restaurants within a 10 mile
radius of the user's location, make a GET call against that entity, and
include a search query in the following format:

    location within <distance in meters> of <latitude>, <longitude>

If we use the location of our user Fred, we first need to convert miles
to meters. 1 mile is equivalent to 1609.344 meters, so 10 miles is about
16093 meters. Thus, the API call looks like this:

    GET https://api.usergrid.com/org_name/app_name/restaurants?ql=location within 16093 of 37.776753, -122.407846


## Managing large sets of results

When your query might return more results than you want to display to
the user at once, you can use the limit parameter with cursors or API
methods to manage the display of results. By default, query results are
limited to 10 at a time. You can adjust this by setting the limit
parameter to a value you prefer.

For example, you might execute a query that could potentially return
hundreds of results, but you want to display 20 of those at a time to
users. To do this, your code sets the limit parameter to 20 when
querying for data, then provides a way for the user to request more of
the results when they're ready.

You would use the following parameters in your query:

+-------------------------+-------------------------+-------------------------+
| Parameter               | Type                    | Description             |
+=========================+=========================+=========================+
| `limit`                 | integer                 | Number of results to    |
|                         |                         | return. The maximum     |
|                         |                         | number of results is    |
|                         |                         | 1,000. Specifying a     |
|                         |                         | limit greater than      |
|                         |                         | 1,000 will result in a  |
|                         |                         | limit of 1,000.         |
|                         |                         |                         |
|                         |                         | Limit is applied to the |
|                         |                         | collection, not the     |
|                         |                         | query string. For       |
|                         |                         | example, the following  |
|                         |                         | query will find the     |
|                         |                         | first 100 entities in   |
|                         |                         | the books collection,   |
|                         |                         | then from that set      |
|                         |                         | return the ones with    |
|                         |                         | author='Hemingway':     |
|                         |                         |                         |
|                         |                         |     /books?ql=author =  |
|                         |                         | 'Hemingway'&limit=100   |
|                         |                         |                         |
|                         |                         | You can also use the    |
|                         |                         | limit parameter on a    |
|                         |                         | request without a query |
|                         |                         | string. The following   |
|                         |                         | example is shorthand    |
|                         |                         | for selecting all books |
|                         |                         | and limiting by 100 at  |
|                         |                         | a time:                 |
|                         |                         |                         |
|                         |                         |     /books?limit=100    |
|                         |                         |                         |
|                         |                         | Using a limit on a      |
|                         |                         | DELETE can help you     |
|                         |                         | manage the amount of    |
|                         |                         | time it takes to delete |
|                         |                         | data. For example you   |
|                         |                         | can delete all of the   |
|                         |                         | books, 1000 at a time,  |
|                         |                         | with the following:     |
|                         |                         |                         |
|                         |                         |     DELETE /books?limit |
|                         |                         | =1000                   |
|                         |                         |                         |
|                         |                         | Keep in mind that       |
|                         |                         | DELETE operations can   |
|                         |                         | take longer to execute. |
|                         |                         | Yet even though the     |
|                         |                         | DELETE query call might |
|                         |                         | time out (such as with  |
|                         |                         | a very large limit),    |
|                         |                         | the operation will      |
|                         |                         | continue on the server  |
|                         |                         | even if the client      |
|                         |                         | stops waiting for the   |
|                         |                         | result.                 |
+-------------------------+-------------------------+-------------------------+
| `cursor`                | string                  | An encoded              |
|                         |                         | representation of the   |
|                         |                         | query position pointing |
|                         |                         | to a set of results. To |
|                         |                         | retrieve the next set   |
|                         |                         | of results, pass the    |
|                         |                         | cursor with your next   |
|                         |                         | call for most results.  |
+-------------------------+-------------------------+-------------------------+

For example:

Select all users whose name starts with fred, and returns the first 50
results:

    /users?ql=select * where name = 'fred*'&limit=50

Retrieve the next batch of users whose name is "fred", passing the
cursor received from the last request to specify where the next set of
results should begin:

    /users?ql=select * where name = 'fred*'&limit=50&cursor=LTIxNDg0NDUxNDpnR2tBQVFFQWdITUFDWFJ2YlM1emJXbDBhQUNBZFFBUUQyMVZneExfRWVLRlV3TG9Hc1doZXdDQWRRQVFIYVdjb0JwREVlS1VCd0xvR3NWT0JRQQ
