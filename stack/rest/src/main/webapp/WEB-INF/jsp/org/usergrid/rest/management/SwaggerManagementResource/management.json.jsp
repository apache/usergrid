<%@ page language="java" contentType="application/json"%>{
  "basePath" : "",
  "swaggerVersion" : "1.1-SHAPSHOT.121026",
  "apiVersion" : "0.1",
  "apis" : [
    {
      "path" : "/management/token",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "grant_type",
              "defaultValue" : "password",
              "description" : "Grant type",
              "dataType" : "string",
              "allowableValues" : {
                "values" : [
                  "password",
                  "client_credentials",
                  "refresh_token",
                  "authorization_code"
                ],
                "valueType" : "LIST"
              },
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "username",
              "description" : "Username (for grant_type=password)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "password",
              "description" : "Password (for grant_type=password)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "client_id",
              "description" : "Client ID (for grant_type=client_credentials)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "client_secret",
              "description" : "Client Secret (for grant_type=client_credentials)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get the management access token.  See the OAuth2 specification for details.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_auth_password_get",
          "responseClass" : "response",
          "summary" : "Get management access token"
        },
        {
          "parameters" : [
            {
              "name" : "grant_type",
              "defaultValue" : "password",
              "description" : "Grant type",
              "dataType" : "string",
              "allowableValues" : {
                "values" : [
                  "password",
                  "client_credentials",
                  "refresh_token",
                  "authorization_code"
                ],
                "valueType" : "LIST"
              },
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "username",
              "description" : "Username (for grant_type=password)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "password",
              "description" : "Password (for grant_type=password)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "client_id",
              "description" : "Client ID (for grant_type=client_credentials)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "client_secret",
              "description" : "Client Secret (for grant_type=client_credentials)",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Get the management access token.  See the OAuth2 specification for details.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_auth_password_post",
          "responseClass" : "response",
          "summary" : "Get management access token"
        }
      ]
    },
    {
      "path" : "/management/authorize",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "response_type",
              "defaultValue" : "token",
              "description" : "Response type",
              "dataType" : "string",
              "allowableValues" : {
                "values" : [
                  "token",
                  "code"
                ],
                "valueType" : "LIST"
              },
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "client_id",
              "description" : "Client ID",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "redirect_uri",
              "description" : "Redirect URI",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "scope",
              "description" : "Access Token Scope",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "state",
              "description" : "Client State",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Authorize the client.  See the OAuth2 specification.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_authorize_get",
          "responseClass" : "response",
          "summary" : "Authorize client"
        },
        {
          "parameters" : [
            {
              "name" : "response_type",
              "defaultValue" : "token",
              "description" : "Response type",
              "dataType" : "string",
              "allowableValues" : {
                "values" : [
                  "token",
                  "code"
                ],
                "valueType" : "LIST"
              },
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "client_id",
              "description" : "Client ID",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "redirect_uri",
              "description" : "Redirect URI",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "scope",
              "description" : "Access Token Scope",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "state",
              "description" : "Client State",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "username",
              "description" : "Username",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "password",
              "description" : "Password",
              "dataType" : "string",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "query"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Authorize the client.  See the OAuth2 specification.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_authorize_post",
          "responseClass" : "response",
          "summary" : "Authorize client"
        }
      ]
    },
    {
      "path" : "/management/orgs",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "json",
              "description" : "Organization to post",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "body"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new organization.  See Usergrid documentation for JSON format of body.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_json_post",
          "responseClass" : "response",
          "summary" : "Create new organization"
        },
        {
          "parameters" : [
            {
              "name" : "organization",
              "description" : "Organization",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "username",
              "description" : "Admin Username",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "name",
              "description" : "Admin Name",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "email",
              "description" : "Admin Email",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "password",
              "description" : "Admin Password",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new organization using form post parameters.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_form_post",
          "responseClass" : "response",
          "summary" : "Create new organization"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Returns the organization details",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_get",
          "responseClass" : "response",
          "summary" : "Find organization by name or UUID"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/activate",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "token",
              "description" : "Activation Token (supplied via email)",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "confirm",
              "description" : "Send confirmation email",
              "dataType" : "boolean",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Activates the organization from link provided in email notification.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_activate_get",
          "responseClass" : "response",
          "summary" : "Activates the organization"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/reactivate",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Request organization reactivation.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_reactivate_get",
          "responseClass" : "response",
          "summary" : "Reactivates the organization"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/feed",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get organization activity feed.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_feed_get",
          "responseClass" : "response",
          "summary" : "Get organization activity feed"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/credentials",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get the organization client credentials.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_credentials_get",
          "responseClass" : "response",
          "summary" : "Get organization client credentials"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Generate new organization client credentials.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_credentials_post",
          "responseClass" : "response",
          "summary" : "Generate organization client credentials"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/users",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get admin users for organization.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_users_get",
          "responseClass" : "response",
          "summary" : "Get admin users for organization"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "json",
              "description" : "Admin user to create",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "body"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new admin user for organization using JSON payload.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_users_json_post",
          "responseClass" : "response",
          "summary" : "Create new admin user for organization"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "username",
              "description" : "Admin Username",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "name",
              "description" : "Admin Name",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "email",
              "description" : "Admin Email",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "password",
              "description" : "Admin Password",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new admin user for organization using form parameters.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_users_form_post",
          "responseClass" : "response",
          "summary" : "Create new admin user for organization"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/users/{user_username_email_or_uuid}",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin user username, email, or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "PUT",
          "notes" : "Adds existing admin users for organization.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_user_put",
          "responseClass" : "response",
          "summary" : "Add admin users to organization"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin user username, email, or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "DELETE",
          "notes" : "Remove an admin user from organization.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_user_delete",
          "responseClass" : "response",
          "summary" : "Remove admin user from organization"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/apps",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get apps for organization.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Application not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_apps_get",
          "responseClass" : "response",
          "summary" : "Get apps for organization"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "json",
              "description" : "Application to create",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "body"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new application for organization using JSON payload.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Application not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_apps_json_post",
          "responseClass" : "response",
          "summary" : "Create new applicationfor organization"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "name",
              "description" : "Application Name",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new application for organization using form parameters.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Application not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_apps_form_post",
          "responseClass" : "response",
          "summary" : "Create new application for organization"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "app_name_or_uuid",
              "description" : "Application name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "DELETE",
          "notes" : "Delete an application in an organization.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Application not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_app_delete",
          "responseClass" : "response",
          "summary" : "Delete an application in an organization"
        }
      ]
    },
    {
      "path" : "/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}/credentials",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "app_name_or_uuid",
              "description" : "Application name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get application keys.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Application not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_app_credentials_get",
          "responseClass" : "response",
          "summary" : "Get application keys"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "app_name_or_uuid",
              "description" : "Application name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Generate application keys.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Application not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_app_credentials_post",
          "responseClass" : "response",
          "summary" : "Generate application keys"
        }
      ]
    },
    {
      "path" : "/management/users",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "json",
              "description" : "Admin user to post",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "body"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new admin user.  See Usergrid documentation for JSON format of body.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_user_json_post",
          "responseClass" : "response",
          "summary" : "Create new admin user"
        },
        {
          "parameters" : [
            {
              "name" : "username",
              "description" : "Admin Username",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "name",
              "description" : "Admin Name",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "email",
              "description" : "Admin Email",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "password",
              "description" : "Admin Password",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new admin using form post parameters.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_user_form_post",
          "responseClass" : "response",
          "summary" : "Create new organization"
        }
      ]
    },
    {
      "path" : "/management/users/resetpw",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [],
          "httpMethod" : "GET",
          "notes" : "Initiate a user password reset.  Returns browser-viewable HTML page.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_user_reset_password_get",
          "responseClass" : "response",
          "summary" : "Initiate a user password reset"
        },
        {
          "parameters" : [
            {
              "name" : "email",
              "description" : "Admin Email",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "recaptcha_challenge_field",
              "description" : "Recaptcha Challenge Field",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "recaptcha_response_field",
              "description" : "Recaptcha Response Field",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Complete a user password reset.  Handles form POST response.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_org_user_reset_password_form_post",
          "responseClass" : "response",
          "summary" : "Complete a user password reset"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Returns the admin user details.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_get",
          "responseClass" : "response",
          "summary" : "Returns the admin user details"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "json",
              "description" : "Admin user details",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "body"
            }
          ],
          "httpMethod" : "PUT",
          "notes" : "Updates the admin user details.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_json_put",
          "responseClass" : "response",
          "summary" : "Updates the admin user details"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}/activate",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "token",
              "description" : "Activation Token (supplied via email)",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "confirm",
              "description" : "Send confirmation email",
              "dataType" : "boolean",
              "required" : false,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Activates the admin user from link provided in email notification.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_activate_get",
          "responseClass" : "response",
          "summary" : "Activates the admin user"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}/reactivate",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Request admin user reactivation.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_reactivate_get",
          "responseClass" : "response",
          "summary" : "Reactivates the admin user"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}/feed",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get admin user activity feed.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_feed_get",
          "responseClass" : "response",
          "summary" : "Get admin user activity feed"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}/password",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "json",
              "description" : "Old and new password",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "body"
            }
          ],
          "httpMethod" : "PUT",
          "notes" : "Set admin user password.  See Usergrid documentation for JSON format of body.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_password_json_put",
          "responseClass" : "response",
          "summary" : "Set admin user password"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}/resetpw",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Initiate a user password reset.  Returns browser-viewable HTML page.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_reset_password_get",
          "responseClass" : "response",
          "summary" : "Initiate a user password reset"
        },
        {
          "parameters" : [
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "recaptcha_challenge_field",
              "description" : "Recaptcha Challenge Field",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "recaptcha_response_field",
              "description" : "Recaptcha Response Field",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Complete a user password reset.  Handles form POST response.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "Organization not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_reset_password_form_post",
          "responseClass" : "response",
          "summary" : "Complete a user password reset"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}/orgs",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "GET",
          "notes" : "Get organizations for admin user.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_orgs_get",
          "responseClass" : "response",
          "summary" : "Get organizations for admin user"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "json",
              "description" : "Organization to create",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "body"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new organization for admin user using JSON payload.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_orgs_json_post",
          "responseClass" : "response",
          "summary" : "Create new organization for admn user"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin username, email or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "organization",
              "description" : "Organization name",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "post"
            }
          ],
          "httpMethod" : "POST",
          "notes" : "Create new organization for admin user using form parameters.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_orgs_form_post",
          "responseClass" : "response",
          "summary" : "Create new organization for admin user"
        }
      ]
    },
    {
      "path" : "/management/users/{user_username_email_or_uuid}/orgs/{org_name_or_uuid}",
      "description" : "Management",
      "operations" : [
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin user username, email, or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "PUT",
          "notes" : "Add admin users to organization.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_org_put",
          "responseClass" : "response",
          "summary" : "Add admin user to organization"
        },
        {
          "parameters" : [
            {
              "name" : "access_token",
              "description" : "The OAuth2 access token",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "query"
            },
            {
              "name" : "user_username_email_or_uuid",
              "description" : "Admin user username, email, or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            },
            {
              "name" : "org_name_or_uuid",
              "description" : "Organization name or uuid",
              "dataType" : "string",
              "required" : true,
              "allowMultiple" : false,
              "paramType" : "path"
            }
          ],
          "httpMethod" : "DELETE",
          "notes" : "Remove an admin user from organization.",
          "responseTypeInternal" : "",
          "errorResponses" : [
            {
              "reason" : "Invalid ID supplied",
              "code" : 400
            },
            {
              "reason" : "User not found",
              "code" : 404
            }
          ],
          "nickname" : "mgt_admin_user_org_delete",
          "responseClass" : "response",
          "summary" : "Remove admin user from organization"
        }
      ]
    }
  ],
  "models" : {
    "response" : {
      "properties" : {
        "id" : {
          "type" : "long"
        },
        "name" : {
          "type" : "string"
        }
      },
      "id" : "response"
    }
  }
}