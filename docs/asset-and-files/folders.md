# Folders

## Linking a folder to an asset
In order to access the asset for the image in the folder, you need to link the folder to the asset. You can do that by issuing a POST request in the following format:

    POST /{org-uuid}/{app-uuid}/folders/{folder-uuid}/assets/{assets_id}
    
where ``{folder-uuid}`` is the UUID of the folder, and ``{assets-uuid}`` is the UUID of the assets entity.

Here’s how you could link the folder and the asset you created for the my-image.jpg image:

    POST https://api.usergrid.com/my-org/my-app/folders/6640a601-2ac6-11e2-92c3-02e81ae640dc/assets/9501cda1-2d21-11e2-b4c6-02e81ac5a17b 
    
In the response, you should see the assets entity for the image added to the folder:

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
    
## Retrieve list of folder assets
You can also request a list of the linked contents of the folder like this:

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
