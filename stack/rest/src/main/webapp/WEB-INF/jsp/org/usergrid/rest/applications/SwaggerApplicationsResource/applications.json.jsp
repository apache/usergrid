<%@ page language="java" contentType="application/json"%>{
  "resourcePath": "/applications",
  "basePath": "",
  "swaggerVersion": "1.1-SHAPSHOT.121026",
  "apiVersion": "0.1",
  "apis": [
    {
      "path": "/applications/{name_or_uuid}",
      "description": "Applications",
      "operations": [
        {
          "parameters": [
            {
              "name": "access_token",
              "description": "The OAuth2 access token",
              "dataType": "string",
              "required": true,
              "allowMultiple": false,
              "paramType": "query"
            },
            {
              "name": "name_or_uuid",
              "description": "application name or uuid",
              "dataType": "string",
              "required": true,
              "allowMultiple": false,
              "paramType": "path"
            }
          ],
          "httpMethod": "GET",
          "notes": "Returns the application details",
          "responseTypeInternal": "",
          "errorResponses": [
            {
              "reason": "Invalid ID supplied",
              "code": 400
            },
            {
              "reason": "Application not found",
              "code": 404
            }
          ],
          "nickname": "get_application",
          "responseClass": "application",
          "summary": "Find application by name or UUID"
        }
      ]
    },
    {
      "path": "/applications/{name_or_uuid}/users",
      "description": "Applications",
      "operations": [
        {
          "parameters": [
            {
              "name": "access_token",
              "description": "The OAuth2 access token",
              "dataType": "string",
              "required": true,
              "allowMultiple": false,
              "paramType": "query"
            },
            {
              "name": "name_or_uuid",
              "description": "application name or uuid",
              "dataType": "string",
              "required": true,
              "allowMultiple": false,
              "paramType": "path"
            }
          ],
          "httpMethod": "GET",
          "notes": "Returns the application details",
          "responseTypeInternal": "",
          "errorResponses": [
            {
              "reason": "Invalid ID supplied",
              "code": 400
            },
            {
              "reason": "User not found",
              "code": 404
            }
          ],
          "nickname": "get_application_users",
          "responseClass": "application",
          "summary": "Find application by name or UUID"
        }
      ]
    }
  ],
  "models": {
    "Application": {
      "properties": {
        "id": {
          "type": "long"
        },
        "name": {
          "type": "string"
        }
      },
      "id": "category"
    }
  }
}