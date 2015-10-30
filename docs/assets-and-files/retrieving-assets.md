# Retrieving assets

## Retrieving asset data

To retrieve the asset from an entity, send a ``GET`` request with the ``Accept`` header set to the content type of the asset. The content type can be retrieved from the ``file-metadata.content-type`` property of the entity the asset is attached to.

### Request syntax

    curl -X GET -H 'Accept: <content_type>' 'https://api.usergrid.com/<org>/<app>/<collection>/<entity>

Parameters

Parameter	     Description
---------        -----------
content_type	 The content type of the attached asset. For example, text/plain, image/jpeg.
org	Organization UUID or organization name
app	Application  UUID or application name
collection	     Name or UUID of the collection of the entity the asset is attached to.
entity	         Name or UUID of the entity the asset is attached to.

### Example request
The following request will retrieve the data for a jpeg file attached to an entity named 'cloud' in the 'pictures' collection:

    curl -X GET -H 'Accept: image/jpeg' 'https://api.usergrid.com/your-org/your-app/pictures/cloud
    
## Retrieving an asset entity
To retrieve the entity that an asset is attached to, perform a ``GET`` request as you normally would to retrieve an entity. For more information, see [Retrieving Data Entities](../data-storage/entities.html#retrieving-data-entities).