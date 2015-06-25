---
title: Uploading files and assets  
category: docs
layout: docs
---

Uploading files and assets
==========================

**Beta Feature**\
 Please note that this is currently a beta feature. As a result, some
functionality may not work as expected.

Using Apache Usergrid APIs, you can store and retrieve files and assets
that hold data objects such as images, video, and audio content.

Apache Usergrid manages these objects as [Assets](/assets) entities.
Optionally, you can use [Folder](/folder) entities to organize related
assets.

**Note:** All binary objects are managed as assets. However, an asset
does not have to be used for a binary object. For example, assets can be
used to model a file system.

Creating a folder to hold assets
--------------------------------

Suppose you have some images that you want to store. Suppose too that
you want to organize your images in folders. To create a folder, you
issue a POST request to the folders endpoint. The general format of the
request is:

    POST /{org-uuid}/{app-uuid}/folders {request body}

The request body must specify the name of the folder, the UUID of the
owner of the folder, as well as the relative path to the folder.

Here’s how you could create a folder named myfirstfolder to hold some of
the images:

    POST https://api.usergrid.com/my-org/my-app/folders {"name": "myfirstfolder","owner": "7d1aa429-e978-11e0-8264-005056c00008", "path": "/myfolders/folder1"}

**Note:** Although not shown in the API examples, you need to provide a
valid access token with each API call. See [Authenticating users and
application clients](/authenticating-users-and-application-clients) for
details. You also need to replace my-org with the name of your
organization, my-app with the name of your application, and the UUID and
path values as appropriate.

The response to the API request should include the UUID of the created
folder, for example:

     "uuid": "6640a601-2ac6-11e2-92c3-02e81ae640dc"

You’ll need this UUID to link the folder to assets.

Creating an assets entity
-------------------------

Assume that one of the images is named my-image.jpg. You can create an
asset for the image by issuing a POST request to the assets endpoint.
The general format of the request is:

    POST /{org-uuid}/{app-uuid}/assets {request body}

The request body must specify the UUID of the name of the asset, the
owner of the asset, as well as the relative path to the asset. For
example, you can create an asset for the my-image.jpg image like this:

    POST https://api.usergrid.com/my-org/my-app/assets {"name": "my-image.jpg","owner": "7d1aa429-e978-11e0-8264-005056c00008", "path": "/myassets/asset1"}

You can also specify a content-type property for the asset in the
request body. For example, you can specify "content-type”:”image/jpeg”.
That content type is then set as a property of the asset. It is also set
(as specified) in the response header. Specifying the content-type is
optional. Apache Usergrid can automatically detect the content type of the
asset when the binary data for the asset is uploaded. It then sets this
as the content-type property value on the asset.

The response to the API request should include the UUID of the created
asset, for example:

    "uuid": "9501cda1-2d21-11e2-b4c6-02e81ac5a17b"

You’ll need this UUID when you upload or retrieve the image.

Linking a folder to an asset
----------------------------

In order to access the asset for the image in the folder, you need to
link the folder to the asset. You can do that by issuing a POST request
in the following format:

    POST /{org-uuid}/{app-uuid}/folders/{folder-uuid}/assets/{assets_id}

where {folder-uuid} is the UUID of the folder, and {assets-uuid} is the
UUID of the assets entity.

Here’s how you could link the folder and the asset you created for the
my-image.jpg image:

    POST https://api.usergrid.com/my-org/my-app/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b 

In the response, you should see the assets entity for the image added to
the folder:

    {
      "action": "post",
      "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params": {},
      "path": "/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets",
      "uri": "https://api.usergrid.com/my-org/my-app/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets",
      "entities": [
        {
          "uuid": "9501cda1-2d21-11e2-b4c6-02e81ac5a17b",
          "type": "asset",
          "name": "my-image.jpg",
          "created": 1352763303163,
          "modified": 1352763303163,
          "metadata": {
            "path": "/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b"
          },
          "owner": "5c0c1789-d503-11e1-b36a-12313b01d5c1",
          "path": "/myassets/asset1"
        }
      ],
      "timestamp": 1352830448045,
      "duration": 54,
      "organization": "my-org”,
      "applicationName": "my-app"
    }

You can also request a list of the linked contents of the folder like
this:

    GET https://api.usergrid.com/my-org/my-app/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets

The response should look something like this:

    {
      "action": "get",
      "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params": {
        "_": [
          "1352830364891"
        ]
      },
      "path": "/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets",
      "uri": "https://api.usergrid.com/my-org/my-app/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets",
      "entities": [
        {
          "uuid": "7bf47435-2ac8-11e2-b4c6-02e81ac5a17b",
          "type": "asset",
          "name": "my-image.jpg",
          "created": 1352505133598,
          "modified": 1352507245108,
          "checksum": "8e0cd3866ee20746c99e9a9825f38ad8",
          "content-length": 11853,
          "content-type": "image/jpeg",
          "etag": "\"8e0cd3866ee20746c99e9a9825f38ad8\"",
          "metadata": {
            "connecting": {
              "assets": "/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets/7bf47435-2ac8-11e2-b4c6-02e81ac5a17b/connecting/assets"
            },
            "connection": "assets",
            "path": "/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets/7bf47435-2ac8-11e2-b4c6-02e81ac5a17b"
          },
          "owner": "5c0c1789-d503-11e1-b36a-12313b01d5c1",
          "path": "my-image"
        }
      ],
      "timestamp": 1352830363797,
      "duration": 57,
      "organization": "my-org",
      "applicationName": "my-app"
    }

Retrieving an asset
-------------------

You can retrieve an asset by its UUID. The general form of the request
is:

    GET /{org-uuid}/{app-uuid}/assets/{asset-uuid}

For example, to retrieve the asset for the my-image.jpg image, you make
a request like this:

    GET https://api.usergrid.com/my-org/my-app/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b

The response should look something like this:

    {
      "action": "get",
      "application": "3400ba10-cd0c-11e1-bcf7-12313d1c4491",
      "params": {
        "_": [
          "1352833055831"
        ]
      },
      "path": "/assets",
      "uri": "https://api.usergrid.com/my-org/my-app/assets",
      "entities": [
        {
          "uuid": "9501cda1-2d21-11e2-b4c6-02e81ac5a17b",
          "type": "asset",
          "name": "edhead3.jpg",
          "created": 1352763303163,
          "modified": 1352763303163,
          "metadata": {
            "connecting": {
              "assets": "/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b/connecting/assets"
            },
            "path": "/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b"
          },
          "owner": "5c0c1789-d503-11e1-b36a-12313b01d5c1",
          "path": "/myassets/asset1"
        }
      ],
      "timestamp": 1352833055408,
      "duration": 17,
      "organization": "my-org",
      "applicationName": "my-app"
    }

Uploading and retrieving binary data for an asset
-------------------------------------------------

So far the focus has been on assets entities, which App services uses to
manage binary objects such as images. But what about the data for those
objects? You can use Apache Usergrid APIs to upload the data for a binary
object and retrieve the data.

**Note:** The data for an asset is stored separately from the entity
itself.

To upload data for an asset, you issue a POST request to the endpoint
for the asset’s data. The general form of the request is:

    POST /{org-uuid}/{app-uuid}/assets/{asset-uuid}/data {binary-data}

where {binary-data} is the binary data for the asset.

For example, you can upload the binary data for the my-image.jpg image
asset like this:

    POST https://api.usergrid.com/my-org/my-app/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b/data {binary data}

where {binary-data} is the binary data for the image.

In cURL format, the upload request would look something like this:

    POST  --data-binary "@src/resources/my-image.jpg" -H
    "Content-Type: application/octet-stream"
    ‘https://api.usergrid.com/my-org/my-app/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b/data’

In the cURL example, the binary data is in a file whose relative path is
src/resources/my-image.jpg.

**Note:** Currently, a 5 MB limitation exists for the size of the
uploaded binary data. This limitation is temporary and the allowable
size of a data upload should increase soon.

To retrieve the data, issue a GET request to the same endpoint, for
example:

    GET https://api.usergrid.com/my-org/my-app/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b/data

In response, you’ll get the binary data for the asset.

Updating the binary data for an asset
-------------------------------------

Suppose, you want to update the my-image.jpg image. You can do it by
replacing the binary data for the asset through a PUT request. Here’s an
example:

    PUT https://api.usergrid.com/my-org/my-app/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b/data {binary-data}

where {binary-data} is the updated binary data for the image.

In cURL format, the update request would look something like this:

    PUT  --data-binary "@src/resources/my-image2.jpg" -H
    "Content-Type: application/octet-stream"
    ‘https://api.usergrid.com/my-org/my-app/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b/data’

In the cURL example, the updated binary data is in a file whose relative
path is src/resources/my-image2.jpg.
