---
title: Apache Usergrid data query overview
category: docs
layout: docs
---

Apache Usergrid data query overview
===================================

You can write code to query for data you've stored in your Apache Usergrid
application. You'll most likely use queries as filters to retrieve
specific entities. For example, you might want to get data about users
who are "following" a specific other user, businesses in specific
geographical locations, and so on.

The following example retrieves a list of restaurants (from a
restaurants collection) whose name property contains the value "diner",
sorting the list in ascending order by name:

    /restaurants?ql=select * where name contains 'diner' order by name asc

> **Note:**Query examples in this content are shown unencoded to make
> them easier to read. Keep in mind that you might need to encode query
> strings if you're sending them as part of URLs, such as when you're
> executing them with the cURL tool.

Having retrieved the list of restaurants, your code could display the
list to your users. You could also use a query to retrieve a list of
diners that are located within a particular geographical area (such as
near your user's current location).

> **Important:** By default, results from queries of your Apache Usergrid
> data are limited to 10 items at a time. You can control this with the
> `limit` parameter, as discussed in [Working with
> queries](/working-queries).

You query your Apache Usergrid data by using a query syntax that's like
Structured Query Language (SQL), the query language for relational
databases. Unlike a relational database, where you specify tables and
columns containing the data you want to query, in your Apache Usergrid
queries you specify collections and entities.

> **Note:** The syntax of Apache Usergrid queries only *resembles* SQL to
> make queries familiar and easier to write. However, the language isn't
> SQL. Only the syntax items documented here are supported.

The examples in these topics illustrate queries using simple strings to
make the queries easier to read. You can also use one of the Apigee
SDKs, which provide functions through which you can pass query strings,
and in some cases shortcuts for bypassing queries altogether.

> **Note:** Queries replace filters, which are deprecated.

For more detail about support for queries your Apache Usergrid database,
see the following topics:

-   [Basic query syntax](/basic-query-syntax)
-   [Query response values](/query-response-values)
-   [Data types supported in queries](/data-types-supported-queries)
-   [Querying data from the admin portal](/querying-data-admin-portal)
-   [Working with queries](/working-queries)

