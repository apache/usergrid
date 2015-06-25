---
title: Geolocation
category: docs
layout: docs
---

Geolocation
===========

Many of today's apps are enhanced by the use of *geolocation*, wireless
detection of the physical location of a remote device. These apps are
said to be *geolocation-aware* in that they query the device to
determine the user's position and then use this data to further enhance
the user's experience. For example, apps can capture the exact location
where a picture was taken or a message was created.

App services support geolocation on any entity, both built in (e.g.,
users, groups) and user defined.

Adding a location to an entity
------------------------------

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

You can also use geolocation to keep track of the location of a user. In
this case, the user already exists, so you just need to update that user
entity. Use POST and include the location member:

    POST https://api.usergrid.com/org_name/app_name/users/fred
    {
      "location": {
        "latitude": 37.779632,
        "longitude": -122.395131
      }
    }

This either adds or updates the location member of the user object for
the user fred.

Making queries against an entity's location
-------------------------------------------

When a location is added to an entity, it is easy to make queries
against that data. For example, to see all restaurants within a 10 mile
radius of the user's location, make a GET call against that entity, and
include a search query in the following format:

    location within <distance in meters> of <latitude>, <longitude>

If we use the location of our user Fred, we first need to convert miles
to meters. 1 mile is equivalent to 1609.344 meters, so 10 miles is about
16093 meters. Thus, the API call looks like this:

    GET https://api.usergrid.com/org_name/app_name/restaurants?ql=location within 16093 of 37.776753, -122.407846

The url-encoded version looks like this:

    https://api.usergrid.com/org_name/app_name/restaurants?ql=location%20within%2016093%20of%2037.776753%2C%20-122.407846&_=1337570474469

In this case, the API call returns one entry for the Rockadero, which is
exactly where Fred happens to be. Bon Appetit!
