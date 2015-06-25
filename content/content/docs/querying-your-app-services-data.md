---
title: Querying your Apache Usergrid data
category: docs
layout: docs
---

Querying your Apache Usergrid data
===============================

You can write code to query for data you've stored in your Apache Usergrid
application. You'll most likely use queries as filters to retrieve
specific entities. For example, you might want to get data about users
who are "following" a specific other user, businesses in specific
geographical locations, and so on.

The following example retrieves a list of restaurants (from a
restaurants collection) whose name property contains the value "diner",
sorting the list in ascending order by name:

    /restaurants?ql=select * where name contains 'diner' order by name asc

Having retrieved the list of restaurants, your code could display the
list to your users. You could also use a query to retrieve a list of
diners that are located within a particular geographical area (such as
near your user's current location).

> **Important:** By default, results from queries of your Apache Usergrid
> data are limited to 10 items at a time. You can control this with the
> `limit` parameter, as discussed [Working with
> queries](/working-queries#cursor).

You query your Apache Usergrid data by using a query syntax that's like
Structured Query Language (SQL), the query language for relational
databases. Unlike a relational database, where you specify tables and
columns containing the data you want to query, in your Apache Usergrid
queries you specify collections and entities.

The examples in this section illustrate queries using simple strings to
make the queries easier to read. You can also use one of the Apigee
SDKs, which provide functions through which you can pass query strings,
and in some cases shortcuts for bypassing queries altogether.

**Note:** Queries replace filters, which are deprecated.

Querying data from the admin portal
-----------------------------------

The easiest way to try out queries you're considering is to use the
admin portal, which you can reach at
[https://apigee.com/usergrid/](https://apigee.com/usergrid/).

To try out queries in the portal, use the following steps:

1.  Go to the **Data Explorer** using the left navigation pane.

    ![](/docs/sites/docs/files/styles/large/public/as_push_console_delete.png?itok=BFAfMReE)

2.  Under **Method**, select the HTTP method you want to use, as
    follows:\
    -   GET to retrieve data.
    -   POST to create data.
    -   PUT to update data.
    -   DELETE to delete data.

3.  In the **Path** box, enter the path to the collection you're
    querying.
4.  In the **Query String** box, enter your query string.

    Note that you put the path and query string in separate fields,
    rather than appending the query string to the path in the **Path**
    box.

The admin portal transforms queries into standard URL-encoded parameters
before issuing HTTP requests. For example, given the following query
resulting from what you've entered in the portal:

    /users?ql=select * where name = 'gladys*'

The string received by Apigee would be the following:

    /users?ql=select%20*%20where%20name%20%3d%20'gladys*'
