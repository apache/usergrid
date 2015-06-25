---
title: Querying data from the admin portal
category: docs
layout: docs
---

Querying data from the admin portal
===================================

The easiest way to try out Apache Usergrid queries you're considering is to
use the admin portal, which you can reach at
[https://apigee.com/usergrid/](https://apigee.com/usergrid/).

To try out queries in the portal, use the following steps:

1.  Go to the **Data Explorer** using the left navigation pane.
2.  Under **Method**, select the HTTP method you want to use, as
    follows:
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
