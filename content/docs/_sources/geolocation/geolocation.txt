# Geolocating your Entities 
Many of today's apps are enhanced by the use of geolocation – wireless detection of the physical location of a remote device. These apps determine the user's position and use this data to enhance user experience. For example, apps can capture the exact location where a picture was taken or determine what businesses stored in the database to return to the user based on their current location.

API Services provides a standard format for storing geolocation information in any entity, as well as syntax for querying that data based on distance from a latitude/longitude point.

## Saving location data in an entity
In API Services, geolocation data is saved in the location property of an entity with latitude and longitude sub-properites in the following format:

    "location": {	
        "latitude": <latitude_coordinate>,
        "longitude": <longitude_coordinate>  
    } 	
    
An entity's geolocation can be specified when the entity is [created](../data-storage/entities.html#creating-custom-data-entities) or added later by [updating](../data-storage/entities.html#updating-data-entities) an existing entity.

For example, the following entity describes a restaurant:

	{
	    "uuid" : "03ae956a-249f-11e3-9f80-d16344f5a0e1",
	    "type" : "restaurant",
	    "name" : "Rockadero",
			"location": {
			    "latitude": 37.779632,
			    "longitude": -122.395131  
			} 
	    "created" : 1379975113142,
	    "modified" : 1379975113142,
	    "metadata" : {
	      "path" : "/restaurants/03ae956a-249f-11e3-9f80-d16344f5a0e1"
	}      
	
## Querying location data
Location-aware apps require the ability to return content and results based on the user's current location. To easily enable this, API Services supports the following query parameter to retrieve entities within a specified distance of any geocoordinate based on its location property:

	location within <distance_in_meters> of <latitude>, <longitude>
	
The returned results are sorted from nearest to furthest. Entities with the same location are returned in the order they were created.

The location parameter can be appended to any standard API Services query. For more information on how to query your API Services data, see [Querying your Data](../data-queries/querying-your-data.html).

For example, here is how you would find all the devices within 8,046 meters (~10 miles) of the center of San Francisco:

	curl -X GET https://api.usergrid.com/your-org/your-app/devices?ql=location within 8046 of 37.774989,-122.419413
	
## Enrich your app with location data
Location-awareness has become a feature users expect in many types of mobile applications because of its ability to create a more personalized and relevant experience for each user. With this in mind, the geolocation feature in API Services was designed to work with many of the available [default data entities](../api-docs.html#models) to allow app developers to easily integrate powerful in-app features that can increase user engagement.

Here are just a few of the ways that saving location data to a data entity can improve an app:

<table class="usergrid-table">
<tr>
  <th>Entity</th>
  <th>Usage</th>
</tr>
<tr>
  <td>user</td>
  <td>Save the location of a user's home as part of their profile in the ``users`` collection to suggest upcoming special events or activities located nearby, or to display advertisements that are relevant based on the user's proximity to a business.</td>
</tr>
<tr>
  <td>device</td>
  <td>Periodically save the location data returned from a user's device, then query the ``devices`` collection to send offers and alerts to user's that are located near your business with a [push notification](../push-notifications/push-notifications-overview).</td>
</tr>
<tr>
  <td>activity</td>
  <td>Create stronger social connections by associating a user ``activity`` with the location where it occurred. The activity can then be displayed to nearby friends and family, or used to enrich the user's activity stream.</td>
</tr>
<tr>
  <td>asset</td>
  <td>Save user photos with location data in the ``asset collection`` to allow users to retrieve and sort their memories based on when and where they happened.</td>
</tr>
</table>

