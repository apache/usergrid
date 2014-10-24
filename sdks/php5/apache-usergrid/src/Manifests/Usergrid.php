<?php
/**
 * Copyright 2010-2014 baas-platform.com, Pty Ltd. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */


return [

    'name' => 'Management',
    'apiVersion' => '1.1',
    'baseUrl' => 'http://baas-platform.com',
    'description' => 'Client to Usergrid management service',
    'operations' => [

        'AuthPasswordGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/token',
            'summary' => 'Get management access token',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'grant_type' => [
                    'description' => 'Grant type.',
                    'location' => 'query',
                    'type' => 'string',
                    'defaultValue' => 'password',
                    'required' => true,
                ],
                'username' => [
                    'description' => 'Username (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'password' => [
                    'description' => 'Password (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_secret' => [
                    'description' => 'Client Secret (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'AuthorizeGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/authorize',
            'summary' => 'Authorize the client.  See the OAuth2 specification.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'response_type' => [
                    'description' => 'Response type.',
                    'location' => 'query',
                    'type' => 'string',
                    'defaultValue' => 'token',
                    'required' => true,
                    'allowableValues' => ['code', 'token']
                ],
                'client_id' => [
                    'description' => 'Client ID.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'redirect_uri' => [
                    'description' => 'Redirect URI.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'scope' => [
                    'description' => 'Access Token Scope.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'state' => [
                    'description' => 'Client State.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]

        ],
        'OrgJsonPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs',
            'summary' => 'Create new organization.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'organization' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization Name'
                ],
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'OrgGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}',
            'summary' => 'Find organization by name or UUID',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied or Name",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'OrgActivateGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/activate',
            'summary' => 'Activates the organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied or Name",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'confirm' => [
                    'location' => 'query',
                    'type' => 'boolean',
                    'required' => false,
                    'description' => 'Send confirmation email'
                ]

            ]
        ],
        'OrgReactivateGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/reactivate',
            'summary' => 'Reactivates the organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
            ]
        ],
        'OrgFeedGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/feed',
            'summary' => 'Get organization activity feed',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'OrgCredentialsGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/credentials',
            'summary' => 'Get organization client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'OrgCredentialsPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/credentials',
            'summary' => 'Generate organization client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'OrgUsersGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/users',
            'summary' => 'Get admin users for organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'OrgUsersJsonPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/users',
            'summary' => 'Create new admin user for organization using JSON payload.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'OrgUsersFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/users',
            'summary' => 'Create new admin user for organization using form parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'username' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]
        ],
        'OrgUserPut' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/orgs/{org_name_or_uuid}/users/{user_username_email_or_uuid}',
            'summary' => 'Adds existing admin users for organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin user username, email, or uuid'
                ]
            ]
        ],
        'OrgUserDelete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/management/orgs/{org_name_or_uuid}/users/{user_username_email_or_uuid}',
            'summary' => 'Remove an admin user from organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin user username, email, or uuid'
                ]
            ]
        ],
        'OrgAppsGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps',
            'summary' => 'Get apps for organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'OrgAppsJsonPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps',
            'summary' => 'Create new application for organization using JSON payload.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application Name'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'OrgAppsFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps',
            'summary' => 'Create new application for organization using form parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application Name'
                ]
            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]
        ],
        'OrgAppDelete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}',
            'summary' => 'Delete an application in an organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'app_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application name or uuid'
                ]
            ]
        ],
        'OrgAppCredentialsGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}/credentials',
            'summary' => 'Get application keys.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'app_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application name or uuid'
                ]
            ]
        ],
        'OrgAppCredentialsPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}/credentials',
            'summary' => 'Generate application keys.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'app_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application name or uuid'
                ]
            ]
        ],
        'OrgUserJsonPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users',
            'summary' => 'Create new admin user.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'OrgUserFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users',
            'summary' => 'Create new admin using form post parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'username' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]

        ],
        'OrgUserResetPasswordGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/resetpw',
            'summary' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
        ],
        'OrgUserResetPasswordFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users/resetpw',
            'summary' => 'Complete a user password reset.  Handles form POST response.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'recaptcha_challenge_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Challenge Field'
                ],
                'recaptcha_response_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Response Field'
                ],
            ]
        ],
        'AdminUserGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}',
            'summary' => 'Returns the admin user details',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
            ]

        ],
        'AdminUserJsonPut' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/users/{user_username_email_or_uuid}',
            'summary' => 'Updates the admin user details.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]

        ],
        'AdminUserActivateGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/activate',
            'summary' => 'Activates the admin user from link provided in email notification.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'confirm' => [
                    'location' => 'uri',
                    'type' => 'boolean',
                    'required' => false,
                    'description' => 'Send confirmation email'
                ],
            ]
        ],
        'AdminUserReactivateGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/reactivate',
            'summary' => 'Request admin user reactivation.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ]
            ]
        ],
        'AdminUserFeedGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/feed',
            'summary' => 'Get admin user activity feed.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
            ]
        ],
        'AdminUserPasswordJsonPut' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/users/{user_username_email_or_uuid}/password',
            'summary' => 'Set admin user password.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'old_password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Old and new password'
                ],
                'new_password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Old and new password'
                ],
            ]
        ],
        'AdminUserResetPasswordGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/resetpw',
            'summary' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ]
            ]
        ],
        'AdminUserResetPasswordFormPost' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/resetpw',
            'summary' => 'Complete a user password reset.  Handles form POST response.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'recaptcha_challenge_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Challenge Field'
                ],
                'recaptcha_response_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Response Field'
                ]
            ]
        ],
        'AdminUserOrgsGet' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
            'summary' => 'Get organizations for admin user.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],

            ]
        ],
        'AdminUserOrgsJsonPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
            'summary' => 'Create new organization for admin user using JSON payload.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'organization' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],

            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'AdminUserOrgsFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
            'summary' => 'Create new organization for admin user using form parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'organization' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],

            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]
        ],
        'AdminUserOrgPut' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs/{org_name_or_uuid}',
            'summary' => 'Add admin users to organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],

            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'AdminUserOrgDelete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs/{org_name_or_uuid}',
            'summary' => 'Remove an admin user from organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],

            ]
        ]


    ]

];

return [

    'name' => 'Application',
    'apiVersion' => '1.1',
    'baseUrl' => 'http://baas-platform.com',
    'description' => 'Client to Usergrid application service',
    'operations' => [
        'AuthPasswordGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/token',
            'notes' => 'Get the app access token.  See the OAuth2 specification for details.',
            'summary' => 'Get app access token',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'grant_type' => [
                    'description' => 'Grant type.',
                    'location' => 'query',
                    'type' => 'string',
                    'defaultValue' => 'password',
                    'required' => true,
                ],
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'username' => [
                    'description' => 'Username (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'password' => [
                    'description' => 'Password (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_secret' => [
                    'description' => 'Client Secret (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'AuthPasswordPost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/token',
            'notes' => 'Get the app access token.  See the OAuth2 specification for details.',
            'summary' => 'Get app access token',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'grant_type' => [
                    'description' => 'Grant type.',
                    'location' => 'postField',
                    'type' => 'string',
                    'defaultValue' => 'password',
                    'required' => true,
                ],
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'username' => [
                    'description' => 'Username (for grant_type=password).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ],
                'password' => [
                    'description' => 'Password (for grant_type=password).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID (for grant_type=client_credentials).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_secret' => [
                    'description' => 'Client Secret (for grant_type=client_credentials).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'AuthorizeGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/authorize',
            'notes' => 'Authorize the app client.  See the OAuth2 specification.',
            'summary' => 'Authorize app client',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'response_type' => [
                    'description' => 'Response type',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                    'default' => 'token'
                ],
                'redirect_uri' => [
                    'description' => 'Redirect URI',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'scope' => [
                    'description' => 'Access Token Scope.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'state' => [
                    'description' => 'Client State.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'AuthorizePost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/authorize',
            'notes' => 'Authorize the app client.  See the OAuth2 specification.',
            'summary' => 'Authorize app client',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'response_type' => [
                    'description' => 'Response type',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                    'default' => 'token'
                ],
                'redirect_uri' => [
                    'description' => 'Redirect URI',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'scope' => [
                    'description' => 'Access Token Scope.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'state' => [
                    'description' => 'Client State.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'CredentialsGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/credentials',
            'notes' => 'Get the app client credentials.',
            'summary' => 'Get app client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
            ]
        ],
        'CredentialsPost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/credentials',
            'notes' => 'Generate new app client credentials',
            'summary' => 'Generate app client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
            ]
        ],
        'UserJsonPost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users',
            'notes' => 'Create new app user',
            'summary' => 'Create new app user.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ]
        ],
        'UserFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users',
            'notes' => 'Create new app user',
            'summary' => 'Create new app user using form post parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'username' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ]
        ],
        'UserPasswordRestGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/resetpw',
            'notes' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'summary' => 'Initiate a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'UserPasswordFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users/resetpw',
            'notes' => 'Complete a user password reset.  Handles form POST response.',
            'summary' => 'Complete a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'email' => [
                    'description' => 'User Email',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_challenge_field' => [
                    'description' => 'Recaptcha Challenge Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_response_field' => [
                    'description' => 'Recaptcha Response Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'UserGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}',
            'notes' => 'Returns the app user details.',
            'summary' => 'Returns the app user details',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'UserJsonPut' => [
            'httpMethod' => 'PUT',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}',
            'notes' => 'Updates the app user details.',
            'summary' => 'Updates the app user details',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'UserActivateGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/activate',
            'notes' => 'Activates the app user from link provided in email notification.',
            'summary' => 'Activates the app user',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'token' => [
                    'description' => 'Activation Token (supplied via email)',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'confirm' => [
                    'description' => 'Send confirmation email',
                    'location' => 'query',
                    'type' => 'boolean',
                    'required' => true,
                ],
            ]
        ],
        'UserReactivateGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/reactivate',
            'notes' => 'Request app user reactivation.',
            'summary' => 'Reactivates the app user',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'UserFeedGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/feed',
            'notes' => 'Get app user activity feed.',
            'summary' => 'Get app user activity feed',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'UserPasswordJsonPut' => [
            'httpMethod' => 'PUT',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/password',
            'notes' => 'Set app user password.  See Usergrid documentation for JSON format of body.',
            'summary' => 'Set app user password',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ]
            ],
            'additionalParameters' => [
                "description" => "Old and new password",
                'location' => 'json'
            ]
        ],
        'UserResetPasswordGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/resetpw',
            'notes' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'summary' => 'Initiate a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'UserResetPasswordFormPost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/resetpw',
            'notes' => 'Complete a user password reset.  Handles form POST response.',
            'summary' => 'Complete a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_challenge_field' => [
                    'description' => 'Recaptcha Challenge Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_response_field' => [
                    'description' => 'Recaptcha Response Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'EntityGet' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/{collection}',
            'notes' => 'Query an app collection.',
            'summary' => 'Query an app collection',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'ql' => [
                    'description' => 'a query in the query language',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'reversed' => [
                    'description' => 'return results in reverse order',
                    'location' => 'query',
                    'type' => 'boolean',
                    'required' => false,
                ],
                'start' => [
                    'description' => 'the first entity UUID to return',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'cursor' => [
                    'description' => 'an encoded representation of the query position for paging',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'limit' => [
                    'description' => 'an encoded representation of the query position for paging',
                    'location' => 'query',
                    'type' => 'integer',
                    'required' => false,
                ],
                'filter' => [
                    'description' => 'a condition to filter on',
                    'location' => 'query',
                    'type' => 'integer',
                    'required' => false,
                ]
            ]
        ],
        'EntityJsonPost' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/{collection}',
            'notes' => 'Create new app entity.  See Usergrid documentation for JSON format of body.',
            'summary' => 'Create new app entity',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ],
            'additionalParameters' => [
                "description" => "Entity data",
                'location' => 'json'
            ]
        ],
        'EntityPut' => [
            'httpMethod' => 'PUT',
            'uri' => '/apps/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
            'notes' => 'Update an app entity in a collection.',
            'summary' => 'Update an app entity',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'entity_name_or_uuid' => [
                    'description' => 'entity name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ],
            'additionalParameters' => [
                "description" => "Entity data",
                'location' => 'json'
            ]
        ],
        'EntityDelete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/apps/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
            'notes' => 'Delete an app entity.',
            'summary' => 'Delete an app entity',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'entity_name_or_uuid' => [
                    'description' => 'entity name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],

    ]
];


# Application
return [

    'name' => 'Application',
    'apiVersion' => '1.1',
    'baseUrl' => 'http://baas-platform.com',
    'description' => 'Client to Usergrid application service',
    'operations' => [
        'app_auth_password_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/token',
            'notes' => 'Get the app access token.  See the OAuth2 specification for details.',
            'summary' => 'Get app access token',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'grant_type' => [
                    'description' => 'Grant type.',
                    'location' => 'query',
                    'type' => 'string',
                    'defaultValue' => 'password',
                    'required' => true,
                ],
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'username' => [
                    'description' => 'Username (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'password' => [
                    'description' => 'Password (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_secret' => [
                    'description' => 'Client Secret (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'app_auth_password_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/token',
            'notes' => 'Get the app access token.  See the OAuth2 specification for details.',
            'summary' => 'Get app access token',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'grant_type' => [
                    'description' => 'Grant type.',
                    'location' => 'postField',
                    'type' => 'string',
                    'defaultValue' => 'password',
                    'required' => true,
                ],
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'username' => [
                    'description' => 'Username (for grant_type=password).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ],
                'password' => [
                    'description' => 'Password (for grant_type=password).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID (for grant_type=client_credentials).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_secret' => [
                    'description' => 'Client Secret (for grant_type=client_credentials).',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'app_authorize_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/authorize',
            'notes' => 'Authorize the app client.  See the OAuth2 specification.',
            'summary' => 'Authorize app client',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'response_type' => [
                    'description' => 'Response type',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                    'default' => 'token'
                ],
                'redirect_uri' => [
                    'description' => 'Redirect URI',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'scope' => [
                    'description' => 'Access Token Scope.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'state' => [
                    'description' => 'Client State.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'app_authorize_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/authorize',
            'notes' => 'Authorize the app client.  See the OAuth2 specification.',
            'summary' => 'Authorize app client',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'response_type' => [
                    'description' => 'Response type',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                    'default' => 'token'
                ],
                'redirect_uri' => [
                    'description' => 'Redirect URI',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'scope' => [
                    'description' => 'Access Token Scope.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'state' => [
                    'description' => 'Client State.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'app_credentials_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/credentials',
            'notes' => 'Get the app client credentials.',
            'summary' => 'Get app client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
            ]
        ],
        'app_credentials_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/credentials',
            'notes' => 'Generate new app client credentials',
            'summary' => 'Generate app client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
            ]
        ],
        'app_user_json_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users',
            'notes' => 'Create new app user',
            'summary' => 'Create new app user.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ]
        ],
        'app_user_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users',
            'notes' => 'Create new app user',
            'summary' => 'Create new app user using form post parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => false,
                ],
                'username' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ]
        ],
        'app_users_reset_password_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/resetpw',
            'notes' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'summary' => 'Initiate a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'app_users_reset_password_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users/resetpw',
            'notes' => 'Complete a user password reset.  Handles form POST response.',
            'summary' => 'Complete a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'email' => [
                    'description' => 'User Email',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_challenge_field' => [
                    'description' => 'Recaptcha Challenge Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_response_field' => [
                    'description' => 'Recaptcha Response Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'app_user_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}',
            'notes' => 'Returns the app user details.',
            'summary' => 'Returns the app user details',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'app_user_json_put' => [
            'httpMethod' => 'PUT',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}',
            'notes' => 'Updates the app user details.',
            'summary' => 'Updates the app user details',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'app_user_activate_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/activate',
            'notes' => 'Activates the app user from link provided in email notification.',
            'summary' => 'Activates the app user',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'token' => [
                    'description' => 'Activation Token (supplied via email)',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'confirm' => [
                    'description' => 'Send confirmation email',
                    'location' => 'query',
                    'type' => 'boolean',
                    'required' => true,
                ],
            ]
        ],
        'app_user_reactivate_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/reactivate',
            'notes' => 'Request app user reactivation.',
            'summary' => 'Reactivates the app user',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'app_user_feed_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/feed',
            'notes' => 'Get app user activity feed.',
            'summary' => 'Get app user activity feed',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'app_user_password_json_put' => [
            'httpMethod' => 'PUT',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/password',
            'notes' => 'Set app user password.  See Usergrid documentation for JSON format of body.',
            'summary' => 'Set app user password',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ]
            ],
            'additionalParameters' => [
                "description" => "Old and new password",
                'location' => 'json'
            ]
        ],
        'app_user_reset_password_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/resetpw',
            'notes' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'summary' => 'Initiate a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'app_user_reset_password_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/users/{user_username_email_or_uuid}/resetpw',
            'notes' => 'Complete a user password reset.  Handles form POST response.',
            'summary' => 'Complete a user password reset',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'user_username_email_or_uuid' => [
                    'description' => 'User username, email or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_challenge_field' => [
                    'description' => 'Recaptcha Challenge Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ],
                'recaptcha_response_field' => [
                    'description' => 'Recaptcha Response Field',
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                ]
            ]
        ],
        'app_entity_get' => [
            'httpMethod' => 'GET',
            'uri' => '/apps/{app_name_or_uuid}/{collection}',
            'notes' => 'Query an app collection.',
            'summary' => 'Query an app collection',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'ql' => [
                    'description' => 'a query in the query language',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'reversed' => [
                    'description' => 'return results in reverse order',
                    'location' => 'query',
                    'type' => 'boolean',
                    'required' => false,
                ],
                'start' => [
                    'description' => 'the first entity UUID to return',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'cursor' => [
                    'description' => 'an encoded representation of the query position for paging',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'limit' => [
                    'description' => 'an encoded representation of the query position for paging',
                    'location' => 'query',
                    'type' => 'integer',
                    'required' => false,
                ],
                'filter' => [
                    'description' => 'a condition to filter on',
                    'location' => 'query',
                    'type' => 'integer',
                    'required' => false,
                ]
            ]
        ],
        'app_entity_json_post' => [
            'httpMethod' => 'POST',
            'uri' => '/apps/{app_name_or_uuid}/{collection}',
            'notes' => 'Create new app entity.  See Usergrid documentation for JSON format of body.',
            'summary' => 'Create new app entity',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ],
            'additionalParameters' => [
                "description" => "Entity data",
                'location' => 'json'
            ]
        ],
        'app_entity_put' => [
            'httpMethod' => 'PUT',
            'uri' => '/apps/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
            'notes' => 'Update an app entity in a collection.',
            'summary' => 'Update an app entity',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'entity_name_or_uuid' => [
                    'description' => 'entity name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ],
            'additionalParameters' => [
                "description" => "Entity data",
                'location' => 'json'
            ]
        ],
        'app_entity_delete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/apps/{app_name_or_uuid}/{collection}/{entity_name_or_uuid}',
            'notes' => 'Delete an app entity.',
            'summary' => 'Delete an app entity',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Application not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'app_name_or_uuid' => [
                    'description' => 'app name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'entity_name_or_uuid' => [
                    'description' => 'entity name or uuid',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'collection' => [
                    'description' => 'collection name (entity type)',
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                ],
                'access_token' => [
                    'description' => 'The OAuth2 access token',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ]
    ]
];

# Management

return [

    'name' => 'Management',
    'apiVersion' => '1.1',
    'baseUrl' => 'http://baas-platform.com',
    'description' => 'Client to Usergrid management service',
    'operations' => [

        'mgt_auth_password_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/token',
            'summary' => 'Get management access token',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'grant_type' => [
                    'description' => 'Grant type.',
                    'location' => 'query',
                    'type' => 'string',
                    'defaultValue' => 'password',
                    'required' => true,
                ],
                'username' => [
                    'description' => 'Username (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'password' => [
                    'description' => 'Password (for grant_type=password).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_id' => [
                    'description' => 'Client ID (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'client_secret' => [
                    'description' => 'Client Secret (for grant_type=client_credentials).',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]
        ],
        'mgt_authorize_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/authorize',
            'summary' => 'Authorize the client.  See the OAuth2 specification.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'response_type' => [
                    'description' => 'Response type.',
                    'location' => 'query',
                    'type' => 'string',
                    'defaultValue' => 'token',
                    'required' => true,
                    'allowableValues' => ['code', 'token']
                ],
                'client_id' => [
                    'description' => 'Client ID.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                ],
                'redirect_uri' => [
                    'description' => 'Redirect URI.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'scope' => [
                    'description' => 'Access Token Scope.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ],
                'state' => [
                    'description' => 'Client State.',
                    'location' => 'query',
                    'type' => 'string',
                    'required' => false,
                ]
            ]

        ],
        'mgt_org_json_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs',
            'summary' => 'Create new organization.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid ID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'organization' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization Name'
                ],
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'mgt_org_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}',
            'summary' => 'Find organization by name or UUID',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied or Name",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'mgt_org_activate_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/activate',
            'summary' => 'Activates the organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied or Name",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'confirm' => [
                    'location' => 'query',
                    'type' => 'boolean',
                    'required' => false,
                    'description' => 'Send confirmation email'
                ]

            ]
        ],
        'mgt_org_reactivate_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/reactivate',
            'summary' => 'Reactivates the organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
            ]
        ],
        'mgt_org_feed_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/feed',
            'summary' => 'Get organization activity feed',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'mgt_org_credentials_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/credentials',
            'summary' => 'Get organization client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'mgt_org_credentials_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/credentials',
            'summary' => 'Generate organization client credentials',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'mgt_org_users_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/users',
            'summary' => 'Get admin users for organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'mgt_org_users_json_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/users',
            'summary' => 'Create new admin user for organization using JSON payload.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'mgt_org_users_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/users',
            'summary' => 'Create new admin user for organization using form parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'username' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]
        ],
        'mgt_org_user_put' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/orgs/{org_name_or_uuid}/users/{user_username_email_or_uuid}',
            'summary' => 'Adds existing admin users for organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin user username, email, or uuid'
                ]
            ]
        ],
        'mgt_org_user_delete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/management/orgs/{org_name_or_uuid}/users/{user_username_email_or_uuid}',
            'summary' => 'Remove an admin user from organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin user username, email, or uuid'
                ]
            ]
        ],
        'mgt_org_apps_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps',
            'summary' => 'Get apps for organization',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ]
            ]
        ],
        'mgt_org_apps_json_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps',
            'summary' => 'Create new application for organization using JSON payload.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application Name'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'mgt_org_apps_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps',
            'summary' => 'Create new application for organization using form parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application Name'
                ]
            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]
        ],
        'mgt_org_app_delete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}',
            'summary' => 'Delete an application in an organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'app_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application name or uuid'
                ]
            ]
        ],
        'mgt_org_app_credentials_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}/credentials',
            'summary' => 'Get application keys.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'app_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application name or uuid'
                ]
            ]
        ],
        'mgt_org_app_credentials_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/orgs/{org_name_or_uuid}/apps/{app_name_or_uuid}/credentials',
            'summary' => 'Generate application keys.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],
                'app_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Application name or uuid'
                ]
            ]
        ],
        'mgt_org_user_json_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users',
            'summary' => 'Create new admin user.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'username' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'mgt_org_user_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users',
            'summary' => 'Create new admin using form post parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'username' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Username'
                ],
                'name' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Name'
                ],
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'password' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Password'
                ]
            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]

        ],
        'mgt_org_user_reset_password_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/resetpw',
            'summary' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
        ],
        'mgt_org_user_reset_password_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users/resetpw',
            'summary' => 'Complete a user password reset.  Handles form POST response.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'email' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin Email'
                ],
                'recaptcha_challenge_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Challenge Field'
                ],
                'recaptcha_response_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Response Field'
                ],
            ]
        ],
        'mgt_admin_user_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}',
            'summary' => 'Returns the admin user details',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
            ]

        ],
        'mgt_admin_user_json_put' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/users/{user_username_email_or_uuid}',
            'summary' => 'Updates the admin user details.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
            ],
            'additionalParameters' => [
                'location' => 'json'
            ]

        ],
        'mgt_admin_user_activate_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/activate',
            'summary' => 'Activates the admin user from link provided in email notification.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'confirm' => [
                    'location' => 'uri',
                    'type' => 'boolean',
                    'required' => false,
                    'description' => 'Send confirmation email'
                ],
            ]
        ],
        'mgt_admin_user_reactivate_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/reactivate',
            'summary' => 'Request admin user reactivation.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ]
            ]
        ],
        'mgt_admin_user_feed_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/feed',
            'summary' => 'Get admin user activity feed.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
            ]
        ],
        'mgt_admin_user_password_json_put' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/users/{user_username_email_or_uuid}/password',
            'summary' => 'Set admin user password.  See Usergrid documentation for JSON format of body.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'old_password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Old and new password'
                ],
                'new_password' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Old and new password'
                ],
            ]
        ],
        'mgt_admin_user_reset_password_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/resetpw',
            'summary' => 'Initiate a user password reset.  Returns browser-viewable HTML page.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ]
            ]
        ],
        'mgt_admin_user_reset_password_form_post' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/resetpw',
            'summary' => 'Complete a user password reset.  Handles form POST response.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "Organization not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'recaptcha_challenge_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Challenge Field'
                ],
                'recaptcha_response_field' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Recaptcha Response Field'
                ]
            ]
        ],
        'mgt_admin_user_orgs_get' => [
            'httpMethod' => 'GET',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
            'summary' => 'Get organizations for admin user.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],

            ]
        ],
        'mgt_admin_user_orgs_json_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
            'summary' => 'Create new organization for admin user using JSON payload.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'organization' => [
                    'location' => 'json',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],

            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'mgt_admin_user_orgs_form_post' => [
            'httpMethod' => 'POST',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs',
            'summary' => 'Create new organization for admin user using form parameters.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'organization' => [
                    'location' => 'postField',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],

            ],
            'additionalParameters' => [
                'location' => 'postField'
            ]
        ],
        'mgt_admin_user_org_put' => [
            'httpMethod' => 'PUT',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs/{org_name_or_uuid}',
            'summary' => 'Add admin users to organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],

            ],
            'additionalParameters' => [
                'location' => 'json'
            ]
        ],
        'mgt_admin_user_org_delete' => [
            'httpMethod' => 'DELETE',
            'uri' => '/management/users/{user_username_email_or_uuid}/orgs/{org_name_or_uuid}',
            'summary' => 'Remove an admin user from organization.',
            'responseClass' => 'Response',
            'errorResponses' => [
                [
                    "reason" => "Invalid UUID supplied",
                    "code" => 400
                ],
                [
                    "reason" => "User not found",
                    "code" => 404
                ]
            ],
            'parameters' => [
                'access_token' => [
                    'location' => 'query',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'The OAuth2 access token'
                ],
                'user_username_email_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Admin username, email or uuid'
                ],
                'org_name_or_uuid' => [
                    'location' => 'uri',
                    'type' => 'string',
                    'required' => true,
                    'description' => 'Organization name or uuid'
                ],

            ]
        ]
    ]

];